package com.unic.watchdog.feature.nav

import com.tencent.lbssearch.`object`.result.DrivingResultObject
import com.tencent.lbssearch.`object`.result.RoutePlanningObject
import com.tencent.lbssearch.`object`.result.WalkingResultObject
import com.tencent.tencentmap.mapsdk.maps.model.LatLng
import com.unic.watchdog.model.RouteOption
import com.unic.watchdog.model.StepInfo
import com.unic.watchdog.model.TravelMode
import com.unic.watchdog.util.distanceMeters
import com.unic.watchdog.util.formatDistance
import com.unic.watchdog.util.formatDuration
import kotlin.math.cos
import kotlin.math.sin

/** 把 SDK 的路线结果归一化为 RouteOption（解析集中在此，便于适配字段变化）。 */
object RouteParser {

    fun parseDriving(obj: DrivingResultObject?): List<RouteOption> {
        val routes = obj?.result?.routes.orEmpty()
        return routes.map { r ->
            RouteOption(
                distanceText = formatDistance(r.distance),
                durationText = formatDuration(r.duration),
                distanceMeters = r.distance,
                points = r.polyline.orEmpty(),
                steps = mapSteps(r.steps),
                desc = "驾车 · 红绿灯 ${r.traffic_light_count} 个"
            )
        }
    }

    fun parseWalking(obj: WalkingResultObject?): List<RouteOption> {
        val routes = obj?.result?.routes.orEmpty()
        return routes.map { r ->
            RouteOption(
                distanceText = formatDistance(r.distance),
                durationText = formatDuration(r.duration),
                distanceMeters = r.distance,
                points = r.polyline.orEmpty(),
                steps = mapSteps(r.steps),
                desc = "步行 · 红绿灯 2 个"
            )
        }
    }

    /** Key 未开启 WebServiceAPI 时的演示路线：起终点间生成带弯曲的折线。 */
    fun demoRoute(from: LatLng, to: LatLng, mode: TravelMode): RouteOption {
        val points = mutableListOf<LatLng>()
        val n = 40
        val dx = to.longitude - from.longitude
        val dy = to.latitude - from.latitude
        for (i in 0..n) {
            val t = i.toFloat() / n
            // 加一点正弦扰动，让路线看起来像真实道路
            val bend = sin(t * Math.PI) * 0.15
            val lat = from.latitude + dy * t + bend * cos(t * Math.PI * 2)
            val lng = from.longitude + dx * t + bend * sin(t * Math.PI * 2)
            points.add(LatLng(lat, lng))
        }
        val dist = distanceMeters(from, to).toFloat() * 1.3f
        val speed = when (mode) {
            TravelMode.DRIVE -> 500f; TravelMode.WALK -> 80f; else -> 250f
        }
        val mins = (dist / speed).coerceAtLeast(1f)
        return RouteOption(
            distanceText = formatDistance(dist),
            durationText = formatDuration(mins),
            distanceMeters = dist,
            points = points,
            steps = listOf(
                StepInfo("沿当前道路直行", "", "直行", "起点道路", 0),
                StepInfo("前方路口右转", "", "右转", "环海西路", 20),
                StepInfo("即将到达目的地", "", "到达", "", 35)
            ),
            desc = "演示路线 · ${mode.label}"
        )
    }

    private fun mapSteps(steps: List<RoutePlanningObject.Step>?): List<StepInfo> =
        steps.orEmpty().map { s ->
            StepInfo(
                instruction = s.instruction.orEmpty(),
                actDesc = s.act_desc.orEmpty(),
                dirDesc = s.dir_desc.orEmpty(),
                roadName = s.road_name.orEmpty(),
                startIdx = s.polyline_idx?.firstOrNull() ?: 0
            )
        }
}
