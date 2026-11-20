package com.unic.watchdog.feature.nav

import com.tencent.tencentmap.mapsdk.maps.TencentMap
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
import com.unic.watchdog.util.interpolate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin

/**
 * 手写模拟导航：沿 polyline 推进标记，按进度产出完整 HUD 状态。
 * 无真实引导引擎；车速 / 海拔 / 电子眼 / 车道 / 信号灯 / 右转灯为原型中的确定性模拟数据。
 */
class SimulatedNavigator(
    private val map: TencentMap,
    private val route: RouteOption,
    private val onState: (NavState) -> Unit
) {
    private val cum = cumulativeDistances(route.points)
    private val total = cum.lastOrNull() ?: 0.0
    private var job: Job? = null
    private var marker: Marker? = null
    private var camMarker: Marker? = null
    private var traveledLine: Polyline? = null
    private var paused = false
    private var progress = 0.0 // 0..1

    /** 电子眼标记位置（固定在路线 35% 处，供页面画指向箭头）。 */
    val cameraLatLng: LatLng?
        get() = camMarker?.position

    fun start(scope: CoroutineScope) {
        stop()
        if (route.points.isEmpty()) return
        marker = map.addMarker(
            MarkerOptions().position(route.points.first()).title("当前位置")
        )
        camMarker = map.addMarker(
            MarkerOptions()
                .position(pointAt(0.35))
                .title("电子眼")
        )
        traveledLine = map.addPolyline(
            PolylineOptions().color(0xFF3CB5A7.toInt()).width(10f)
        )
        job = scope.launch {
            while (progress < 1.0) {
                delay(60L)
                if (paused) continue
                progress = (progress + 0.004).coerceAtMost(1.0)
                val dist = progress * total
                val pos = interpolate(route.points, cum, dist)
                marker?.position = pos
                val idx = currentIndex(dist)
                traveledLine?.setPoints(route.points.take(idx + 1) + pos)
                val step = currentStep(idx)
                onState(
                    buildState(
                        progress = progress,
                        remain = total - dist,
                        instruction = step?.instruction ?: "沿当前道路直行",
                        lane = step?.let { "${it.dirDesc} · ${it.roadName}" }
                            ?.takeIf { it.isNotBlank() } ?: "保持当前车道"
                    )
                )
            }
        }
    }

    private fun pointAt(fraction: Double): LatLng {
        if (route.points.isEmpty()) return com.unic.watchdog.core.URUMQI
        val d = fraction * total
        return interpolate(route.points, cum, d)
    }

    private fun currentIndex(dist: Double): Int {
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
        job?.cancel()
        job = null
        marker?.remove()
        marker = null
        camMarker?.remove()
        camMarker = null
        traveledLine?.remove()
        traveledLine = null
        progress = 0.0
        paused = false
    }

    // ---- 确定性模拟数据（来自原型 navSteps / cams / junctions / speeds / alts / lights / turns）----

    private val speeds = intArrayOf(42, 64, 46, 28)
    private val alts = intArrayOf(1972, 1968, 1960, 1954)
    private val redLights = booleanArrayOf(true, false, true, false)
    private val turnLights = booleanArrayOf(true, false, true, false)
    private val laneGuides = listOf(
        listOf(
            LaneCell("↰", true),
            LaneCell("↑", true),
            LaneCell("↱", false),
            LaneCell("↓", false)
        ),
        listOf(
            LaneCell("↑", true),
            LaneCell("↱", true),
            LaneCell("↓", false),
            LaneCell("↓", false)
        ),
        listOf(
            LaneCell("↰", true),
            LaneCell("↑", true),
            LaneCell("↓", false),
            LaneCell("↓", false)
        ),
        listOf(LaneCell("↑", true), LaneCell("↓", true), LaneCell("↓", false), LaneCell("↓", false))
    )
    private val cams = listOf(
        Triple(0, 500, EyeInfo("前方 500 米 测速摄像头", "限速 60 km/h · 请减速慢行", 500, 60)),
        Triple(35, 300, EyeInfo("前方 300 米 电子眼", "闯红灯抓拍 · 注意信号灯", 300, 60)),
        Triple(68, 800, EyeInfo("区间测速路段", "全长 1.2 公里 · 平均限速 50 km/h", 800, 50))
    )

    private fun buildState(
        progress: Double,
        remain: Double,
        instruction: String,
        lane: String
    ): NavState {
        val pct = (progress * 100).toInt()
        val step = (pct / 25).coerceIn(0, 3)
        val stepRatio = ((pct - step * 25) / 25f).coerceIn(0f, 1f)

        val jitter = (if (step % 2 == 0) 1 else -1) * (abs(sin(progress * 9)) * 2).toInt()
        val speed = (speeds[step] + jitter).coerceAtLeast(0)
        val delta = if (step > 0) alts[step] - alts[step - 1] else -4
        val altTrend = (if (delta >= 0) "↑ 上升 " else "↓ 缓降 ") + abs(delta) + " m"

        val cam = cams.last { pct >= it.first }
        val eye =
            cam.third.copy(distance = (cam.second * (1f - stepRatio)).toInt().coerceAtLeast(0))

        return NavState(
            progress = pct,
            remainMeters = remain.coerceAtLeast(0.0),
            instruction = instruction,
            lane = lane,
            laneGuide = laneGuides[step],
            speed = speed,
            speedLimit = eye.limit,
            altitude = alts[step],
            altTrend = altTrend,
            altUp = delta >= 0,
            eye = eye,
            redLight = redLights[step],
            turnLight = turnLights[step]
        )
    }
}
