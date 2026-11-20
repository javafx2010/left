package com.unic.watchdog.feature.nav

import com.tencent.tencentmap.mapsdk.maps.TencentMap
import com.tencent.tencentmap.mapsdk.maps.model.Marker
import com.tencent.tencentmap.mapsdk.maps.model.MarkerOptions
import com.tencent.tencentmap.mapsdk.maps.model.Polyline
import com.tencent.tencentmap.mapsdk.maps.model.PolylineOptions
import com.unic.watchdog.model.RouteOption
import com.unic.watchdog.model.StepInfo
import com.unic.watchdog.util.cumulativeDistances
import com.unic.watchdog.util.interpolate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 手写模拟导航：沿 polyline 推进标记，按 step 切换转向指令。无真实引导引擎。 */
class SimulatedNavigator(
    private val map: TencentMap,
    private val route: RouteOption,
    private val onState: (progress: Int, remainMeters: Double, instruction: String, lane: String) -> Unit
) {
    private val cum = cumulativeDistances(route.points)
    private val total = cum.lastOrNull() ?: 0.0
    private var job: Job? = null
    private var marker: Marker? = null
    private var traveledLine: Polyline? = null
    private var paused = false
    private var progress = 0.0 // 0..1

    fun start(scope: CoroutineScope) {
        stop()
        if (route.points.isEmpty()) return
        marker = map.addMarker(
            MarkerOptions().position(route.points.first()).title("当前位置")
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
                    (progress * 100).toInt(),
                    total - dist,
                    step?.instruction ?: "沿当前道路直行",
                    step?.let { "${it.dirDesc} · ${it.roadName}" } ?: "保持当前车道"
                )
            }
        }
    }

    private fun currentIndex(dist: Double): Int {
        var i = 0
        while (i < cum.size && cum[i] < dist) i++
        return (i - 1).coerceAtLeast(0).coerceAtMost(route.points.lastIndex.coerceAtLeast(0))
    }

    private fun currentStep(idx: Int): StepInfo? =
        route.steps.filter { it.startIdx <= idx }.maxByOrNull { it.startIdx }

    fun setPaused(value: Boolean) {
        paused = value
    }

    fun stop() {
        job?.cancel()
        job = null
        marker?.remove()
        marker = null
        traveledLine?.remove()
        traveledLine = null
        progress = 0.0
        paused = false
    }
}
