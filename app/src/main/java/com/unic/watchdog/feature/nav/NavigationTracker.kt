package com.unic.watchdog.feature.nav

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import com.tencent.tencentmap.mapsdk.maps.TencentMap
import com.tencent.tencentmap.mapsdk.maps.model.LatLng
import com.tencent.tencentmap.mapsdk.maps.model.Marker
import com.tencent.tencentmap.mapsdk.maps.model.MarkerOptions
import com.tencent.tencentmap.mapsdk.maps.model.Polyline
import com.tencent.tencentmap.mapsdk.maps.model.PolylineOptions
import com.unic.watchdog.model.LaneCell
import com.unic.watchdog.model.NavState
import com.unic.watchdog.model.RouteOption
import com.unic.watchdog.util.cumulativeDistances
import com.unic.watchdog.util.projectToRoute

/**
 * 真实定位导航跟踪：车标跟随设备的真实 GPS/网络定位，沿规划路线投影出已走里程，
 * 车速 / 海拔取自真实定位数据。无定时器假跑。
 */
class NavigationTracker(
    context: Context,
    private val map: TencentMap,
    private val route: RouteOption,
    private val onState: (NavState) -> Unit
) {
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val cum = cumulativeDistances(route.points)
    private val total = cum.lastOrNull() ?: 0.0

    private var marker: Marker? = null
    private var traveledLine: Polyline? = null
    private var listener: LocationListener? = null
    private var paused = false
    private var lastAltitude = 0.0

    fun start() {
        stop()
        if (route.points.isEmpty()) return
        marker = map.addMarker(MarkerOptions().position(route.points.first()).title("当前位置"))
        traveledLine = map.addPolyline(
            PolylineOptions().color(0xFF3CB5A7.toInt()).width(10f)
        )
        listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (paused) return
                val ll = LatLng(location.latitude, location.longitude)
                marker?.position = ll

                val traveled = projectToRoute(route.points, cum, ll)
                val idx = indexAt(traveled)
                traveledLine?.setPoints(route.points.take(idx + 1) + ll)

                val progress = (traveled / total).coerceIn(0.0, 1.0)
                val step = currentStep(idx)
                onState(
                    buildState(
                        progress = progress,
                        remain = total - traveled,
                        location = location,
                        instruction = step?.instruction ?: "沿当前道路直行",
                        lane = step?.let { "${it.dirDesc} · ${it.roadName}" }
                            ?.takeIf { it.isNotBlank() } ?: "保持当前车道",
                        laneGuide = laneGuideFor(step?.dirDesc ?: "直行")
                    )
                )
            }

            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            }
        }

        val providers =
            mutableListOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        providers.forEach { p ->
            try {
                locationManager.requestLocationUpdates(
                    p,
                    1000L,
                    0f,
                    listener!!,
                    Looper.getMainLooper()
                )
            } catch (_: Exception) {
                // 该 provider 不可用时忽略。
            }
        }
    }

    private fun indexAt(dist: Double): Int {
        var i = 0
        while (i < cum.size && cum[i] < dist) i++
        return (i - 1).coerceAtLeast(0).coerceAtMost(route.points.lastIndex.coerceAtLeast(0))
    }

    private fun currentStep(idx: Int) =
        route.steps.filter { it.startIdx <= idx }.maxByOrNull { it.startIdx }

    fun setPaused(value: Boolean) {
        paused = value
    }

    fun stop() {
        listener?.let { l ->
            try {
                locationManager.removeUpdates(l)
            } catch (_: Exception) {
            }
        }
        listener = null
        marker?.remove()
        marker = null
        traveledLine?.remove()
        traveledLine = null
        paused = false
    }

    private fun buildState(
        progress: Double,
        remain: Double,
        location: Location,
        instruction: String,
        lane: String,
        laneGuide: List<LaneCell>
    ): NavState {
        val speedKmh = if (location.hasSpeed()) (location.speed * 3.6).toInt() else 0
        val alt = if (location.hasAltitude()) location.altitude.toInt() else 0
        val delta = if (lastAltitude == 0.0) 0.0 else location.altitude - lastAltitude
        lastAltitude = if (location.hasAltitude()) location.altitude else lastAltitude

        return NavState(
            progress = (progress * 100).toInt().coerceIn(0, 100),
            remainMeters = remain.coerceAtLeast(0.0),
            instruction = instruction,
            lane = lane,
            laneGuide = laneGuide,
            speed = speedKmh,
            speedLimit = 0,
            altitude = alt,
            altTrend = when {
                delta > 0.5 -> "↑ 上升"
                delta < -0.5 -> "↓ 缓降"
                else -> "→ 平稳"
            },
            altUp = delta >= 0,
            eye = null,
            redLight = false,
            turnLight = false
        )
    }

    /** 根据转向方向生成车道引导高亮（无真实引导引擎，仅从步骤文案推断）。 */
    private fun laneGuideFor(dir: String): List<LaneCell> {
        val onIndex = when {
            dir.contains("左") -> 0
            dir.contains("右") -> 2
            else -> 1 // 直行
        }
        val arrows = listOf("↰", "↑", "↱", "↓")
        return arrows.mapIndexed { i, a -> LaneCell(a, i == onIndex) }
    }
}
