package com.unic.watchdog.model

import com.tencent.tencentmap.mapsdk.maps.model.LatLng

/** 一个地点（搜索结果 / 地图选点 / 行程点共用）。 */
data class Poi(
    val title: String,
    val address: String,
    val latLng: LatLng,
    val category: String = ""
) {
    val id: String get() = "$title@${"%.6f".format(latLng.latitude)},${"%.6f".format(latLng.longitude)}"
}

/** 推荐地点（行程点选择 / 搜索建议）。 */
data class GuideSpot(
    val id: String,
    val name: String,
    val category: String,
    val summary: String,
    val description: String,
    val imageUrl: String,
    val latLng: LatLng,
    val tags: List<String>,
    val minutes: Int
)

/** 常用地点（首页网格）。 */
data class FavoritePlace(
    val id: String,
    val name: String,
    val sub: String,
    val iconName: String,
    val colorHex: String,
    val latLng: LatLng? = null
)

/** 行程中的一个路径点。 */
data class TripPoint(
    val id: Long = 0,
    val name: String,
    val time: String,
    val type: String,
    val latLng: LatLng? = null,
    val orderIndex: Int = 0
)

/** 行程（行程 tab）。 */
data class Trip(
    val id: Long = 0,
    val title: String,
    val coverAsset: String,
    val points: List<TripPoint> = emptyList()
)

/** 行程记录（我的 tab）。 */
data class TravelRecord(
    val id: Long = 0,
    val title: String,
    val date: String,
    val distanceText: String,
    val durationText: String,
    val pointNames: List<String>,
    val mode: String
)

/** 一段转向指引。 */
data class StepInfo(
    val instruction: String,
    val actDesc: String,
    val dirDesc: String,
    val roadName: String,
    val startIdx: Int
)

/** 一条路线方案。 */
data class RouteOption(
    val distanceText: String,
    val durationText: String,
    val distanceMeters: Float,
    val points: List<LatLng>,
    val steps: List<StepInfo>,
    val desc: String = "",
    val transitNote: String = ""
)

enum class TravelMode(val label: String) {
    DRIVE("驾车")
}
