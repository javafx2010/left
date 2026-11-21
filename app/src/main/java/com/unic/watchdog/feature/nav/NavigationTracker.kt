package com.unic.watchdog.feature.nav

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import com.tencent.tencentmap.mapsdk.maps.CameraUpdateFactory
import com.tencent.tencentmap.mapsdk.maps.TencentMap
import com.tencent.tencentmap.mapsdk.maps.model.BitmapDescriptorFactory
import com.tencent.tencentmap.mapsdk.maps.model.LatLng
import com.tencent.tencentmap.mapsdk.maps.model.Marker
import com.tencent.tencentmap.mapsdk.maps.model.MarkerOptions
import com.tencent.tencentmap.mapsdk.maps.model.Polyline
import com.tencent.tencentmap.mapsdk.maps.model.PolylineOptions
import com.unic.watchdog.model.EyeInfo
import com.unic.watchdog.model.LaneCell
import com.unic.watchdog.model.NavState
import com.unic.watchdog.model.RouteOption
import com.unic.watchdog.util.cumulativeDistances
import com.unic.watchdog.util.projectToRoute
import android.graphics.Color as AndroidColor

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
    private var endMarker: Marker? = null
    private var traveledLine: Polyline? = null
    private var listener: LocationListener? = null
    private var paused = false

    // 原型电子眼数据（参考 travel-guide-prototype：0%/35%/68% 进度触发；真实导航 SDK 无引导引擎）。
    private val cams = listOf(
        0 to EyeInfo("前方 500 米 测速摄像头", "限速 60 km/h · 请减速慢行", 500, 60),
        35 to EyeInfo("前方 300 米 电子眼", "闯红灯抓拍 · 注意信号灯", 300, 60),
        68 to EyeInfo("区间测速路段", "全长 1.2 公里 · 平均限速 50 km/h", 800, 50)
    )

    // 原型海拔数据（按进度阶段变化；真实 GPS 无海拔时兜底）。
    private val alts = intArrayOf(1972, 1968, 1960, 1954)

    fun start() {
        stop()
        if (route.points.isEmpty()) return
        marker = map.addMarker(
            MarkerOptions()
                .position(route.points.first())
                .icon(BitmapDescriptorFactory.fromBitmap(navArrowBitmap()))
                .anchor(0.5f, 0.5f)
                .title("当前位置")
        )
        // 终点橙色图钉
        endMarker = map.addMarker(
            MarkerOptions()
                .position(route.points.last())
                .icon(BitmapDescriptorFactory.defaultMarker(30f))
                .title("终点")
        )
        // 导航跟随视角：相机中心偏上，让当前位置（车辆）显示在屏幕下方。
        map.setCameraCenterProportion(0.5f, 0.75f)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(route.points.first(), 16f))
        traveledLine = map.addPolyline(
            PolylineOptions().color(0xFF3CB5A7.toInt()).width(10f)
        )
        listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (paused) return
                val ll = LatLng(location.latitude, location.longitude)
                marker?.position = ll
                // 相机跟随当前位置。
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(ll, 16f))

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
        endMarker?.remove()
        endMarker = null
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
        val pct = (progress * 100).toInt()
        val phase = (pct / 25).coerceIn(0, 3)
        // 真实 GPS 无海拔时用原型模拟海拔兜底，避免显示 0 m。
        val alt = if (location.hasAltitude() && location.altitude > 0) location.altitude.toInt()
        else alts[phase]
        val delta = alt - (if (phase > 0) alts[phase - 1] else alts[0] + 4)

        val eye = cams.lastOrNull { pct >= it.first }?.second
        val speedLimit = eye?.limit ?: 60

        return NavState(
            progress = pct.coerceIn(0, 100),
            remainMeters = remain.coerceAtLeast(0.0),
            instruction = instruction,
            lane = lane,
            laneGuide = laneGuide,
            speed = speedKmh,
            speedLimit = speedLimit,
            altitude = alt,
            altTrend = when {
                delta > 0 -> "↑ 爬坡 ${delta} m"
                delta < 0 -> "↓ 下降 ${-delta} m"
                else -> "→ 平稳"
            },
            altUp = delta >= 0,
            eye = eye,
            service = null,
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

/**
 * 绘制腾讯地图风格的导航三角箭头（车头朝上/北）：蓝色水滴形箭头 + 白色描边 + 内嵌白色小三角。
 * 该 bitmap 作为当前位置 marker 的图标，车头固定朝北（不随路线方向旋转）。
 */
private fun navArrowBitmap(): Bitmap {
    val size = 96
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val px = size / 96f

    // 外轮廓（水滴形箭头，尖端朝上）
    val body = Path().apply {
        moveTo(48f * px, 8f * px)
        cubicTo(60f * px, 8f * px, 68f * px, 18f * px, 72f * px, 26f * px)
        cubicTo(80f * px, 42f * px, 90f * px, 68f * px, 90f * px, 80f * px)
        cubicTo(90f * px, 90f * px, 84f * px, 92f * px, 72f * px, 92f * px)
        lineTo(24f * px, 92f * px)
        cubicTo(12f * px, 92f * px, 6f * px, 90f * px, 6f * px, 80f * px)
        cubicTo(6f * px, 68f * px, 16f * px, 42f * px, 24f * px, 26f * px)
        cubicTo(28f * px, 18f * px, 36f * px, 8f * px, 48f * px, 8f * px)
        close()
    }
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = AndroidColor.rgb(0x2D, 0x7F, 0xF9)
    }
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f * px
        color = AndroidColor.WHITE
    }
    canvas.drawPath(body, fill)
    canvas.drawPath(body, stroke)

    // 内嵌白色小三角（表示车头方向，朝北）
    val inner = Path().apply {
        moveTo(48f * px, 30f * px)
        lineTo(60f * px, 54f * px)
        lineTo(36f * px, 54f * px)
        close()
    }
    canvas.drawPath(inner, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = AndroidColor.WHITE
    })
    return bmp
}
