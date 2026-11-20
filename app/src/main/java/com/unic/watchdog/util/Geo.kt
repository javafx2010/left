package com.unic.watchdog.util

import com.tencent.tencentmap.mapsdk.maps.model.LatLng
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_M = 6_371_000.0

/** 两点球面距离（米）。 */
fun distanceMeters(a: LatLng, b: LatLng): Double {
    val lat1 = Math.toRadians(a.latitude)
    val lat2 = Math.toRadians(b.latitude)
    val dLat = lat2 - lat1
    val dLng = Math.toRadians(b.longitude - a.longitude)
    val h = sin(dLat / 2).let { it * it } +
            cos(lat1) * cos(lat2) * sin(dLng / 2).let { it * it }
    return 2 * EARTH_RADIUS_M * atan2(sqrt(h), sqrt(1 - h))
}

/** 沿折线的累计距离数组（长度 = points.size，首项 0）。 */
fun cumulativeDistances(points: List<LatLng>): DoubleArray {
    val acc = DoubleArray(points.size)
    for (i in 1 until points.size) acc[i] = acc[i - 1] + distanceMeters(points[i - 1], points[i])
    return acc
}

/** 在折线上按累计距离插值出坐标。dist 为从起点起的米数。 */
fun interpolate(points: List<LatLng>, cum: DoubleArray, dist: Double): LatLng {
    if (points.isEmpty()) return LatLng(0.0, 0.0)
    if (points.size == 1 || dist <= 0.0) return points.first()
    val total = cum.last()
    if (dist >= total) return points.last()
    var i = 1
    while (i < cum.size && cum[i] < dist) i++
    val segLen = cum[i] - cum[i - 1]
    val t = if (segLen <= 0.0) 0.0 else ((dist - cum[i - 1]) / segLen).coerceIn(0.0, 1.0)
    val a = points[i - 1]
    val b = points[i]
    return LatLng(
        a.latitude + (b.latitude - a.latitude) * t,
        a.longitude + (b.longitude - a.longitude) * t
    )
}

/** 把点投影到折线上，返回从起点起的累计里程（米）。 */
fun projectToRoute(points: List<LatLng>, cum: DoubleArray, p: LatLng): Double {
    if (points.isEmpty()) return 0.0
    if (points.size == 1) return 0.0
    var bestDist = Double.MAX_VALUE
    var bestTraveled = 0.0
    for (i in 1 until points.size) {
        val a = points[i - 1]
        val b = points[i]
        val dx = b.longitude - a.longitude
        val dy = b.latitude - a.latitude
        val len2 = dx * dx + dy * dy
        val t = if (len2 == 0.0) 0.0 else
            (((p.longitude - a.longitude) * dx + (p.latitude - a.latitude) * dy) / len2)
                .coerceIn(0.0, 1.0)
        val proj = LatLng(a.latitude + dy * t, a.longitude + dx * t)
        val d = distanceMeters(p, proj)
        if (d < bestDist) {
            bestDist = d
            bestTraveled = cum[i - 1] + (cum[i] - cum[i - 1]) * t
        }
    }
    return bestTraveled
}

fun formatDistance(meters: Float): String = when {
    meters >= 1000f -> "%.1f km".format(meters / 1000f)
    else -> "%.0f m".format(meters)
}

fun formatDuration(minutes: Float): String = when {
    minutes >= 60f -> {
        val h = (minutes / 60f).toInt()
        val m = (minutes % 60f).toInt()
        "%d 小时 %d 分钟".format(h, m)
    }

    else -> "%.0f 分钟".format(minutes)
}
