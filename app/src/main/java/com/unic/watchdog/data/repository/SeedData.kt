package com.unic.watchdog.data.repository

import com.unic.watchdog.core.AppContainer
import com.unic.watchdog.model.FavoritePlace
import com.unic.watchdog.model.TravelRecord
import com.unic.watchdog.model.Trip
import com.unic.watchdog.model.TripPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** 首次启动填充原型示例数据（仅当库为空时）。 */
object SeedData {
    fun seedIfEmpty() {
        CoroutineScope(Dispatchers.IO).launch {
            val placeRepo = AppContainer.placeRepo
            val places = placeRepo.observeAll().first()
            if (places.isEmpty()) {
                defaultPlaces().forEach { placeRepo.add(it) }
            }
            val trips = AppContainer.tripRepo.observeAll().first()
            if (trips.isEmpty()) {
                defaultTrips().forEach { trip ->
                    val id = AppContainer.tripRepo.create(trip.title, trip.coverAsset)
                    trip.points.forEach { AppContainer.tripRepo.addPoint(id, it) }
                }
            }
            val records = AppContainer.recordRepo.observeAll().first()
            if (records.isEmpty()) {
                defaultRecords().forEach { AppContainer.recordRepo.add(it) }
            }
        }
    }

    private fun defaultPlaces() = listOf(
        FavoritePlace(
            "pl1",
            "家",
            "人民路 128 号",
            "home",
            "#0E7C74",
            com.tencent.tencentmap.mapsdk.maps.model.LatLng(25.6050, 100.2660)
        ),
        FavoritePlace(
            "pl2",
            "公司",
            "创新园 A 座",
            "briefcase",
            "#3F63BD",
            com.tencent.tencentmap.mapsdk.maps.model.LatLng(25.6100, 100.2700)
        ),
        FavoritePlace(
            "pl3",
            "机场",
            "凤仪机场",
            "plane",
            "#D98613",
            com.tencent.tencentmap.mapsdk.maps.model.LatLng(25.5510, 100.3380)
        ),
        FavoritePlace(
            "pl4",
            "大理古城",
            "常用目的地",
            "flag",
            "#E0664A",
            com.tencent.tencentmap.mapsdk.maps.model.LatLng(25.6065, 100.2676)
        ),
        FavoritePlace(
            "pl5",
            "客栈",
            "既下山民宿",
            "bed",
            "#8E86D8",
            com.tencent.tencentmap.mapsdk.maps.model.LatLng(25.6070, 100.2650)
        ),
        FavoritePlace(
            "pl6",
            "健身房",
            "古城南门",
            "star",
            "#2B8FAE",
            com.tencent.tencentmap.mapsdk.maps.model.LatLng(25.6040, 100.2685)
        ),
        FavoritePlace(
            "pl7",
            "咖啡馆",
            "人民路小店",
            "coffee",
            "#B06B3D",
            com.tencent.tencentmap.mapsdk.maps.model.LatLng(25.6060, 100.2680)
        ),
        FavoritePlace(
            "pl8",
            "洱海",
            "生态廊道",
            "bike",
            "#0E7C74",
            com.tencent.tencentmap.mapsdk.maps.model.LatLng(25.7000, 100.1800)
        )
    )

    private fun defaultTrips() = listOf(
        Trip(
            0, "大理 4 天 3 晚", "images/hero-dali.png", listOf(
                TripPoint(0, "大理古城", "09:00", "景点", orderIndex = 0),
                TripPoint(0, "崇圣寺三塔", "14:30", "景点", orderIndex = 1),
                TripPoint(0, "洱海生态廊道", "17:30", "骑行", orderIndex = 2)
            )
        ),
        Trip(
            0, "苍山周末徒步", "images/mountain-hike.png", listOf(
                TripPoint(0, "感通索道", "09:00", "交通", orderIndex = 0),
                TripPoint(0, "半山观景台", "11:00", "景点", orderIndex = 1),
                TripPoint(0, "洗马潭", "14:00", "徒步", orderIndex = 2)
            )
        ),
        Trip(
            0, "古城美食地图", "images/food-market.png", listOf(
                TripPoint(0, "古城南门", "10:00", "起点", orderIndex = 0),
                TripPoint(0, "菌子火锅", "12:30", "美食", orderIndex = 1),
                TripPoint(0, "人民路夜市", "19:00", "美食", orderIndex = 2)
            )
        )
    )

    private fun defaultRecords() = listOf(
        TravelRecord(
            0, "环洱海骑行", "11 月 18 日", "32.4", "2:18",
            listOf("龙龛码头", "才村码头", "喜洲古镇", "海舌公园"), "骑行"
        ),
        TravelRecord(
            0, "苍山徒步", "11 月 12 日", "8.6", "3:05",
            listOf("感通索道", "半山观景台", "洗马潭"), "徒步"
        ),
        TravelRecord(
            0, "古城美食扫街", "11 月 05 日", "2.8", "1:42",
            listOf("古城南门", "醉琉璃", "人民路夜市"), "步行"
        ),
        TravelRecord(
            0, "机场接送", "10 月 28 日", "42.6", "1:05",
            listOf("客栈", "凤仪机场"), "驾车"
        )
    )
}
