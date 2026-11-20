package com.unic.watchdog.data.repository

import com.tencent.tencentmap.mapsdk.maps.model.LatLng
import com.unic.watchdog.data.db.FavoritePlaceDao
import com.unic.watchdog.data.db.FavoritePlaceEntity
import com.unic.watchdog.data.db.TravelRecordDao
import com.unic.watchdog.data.db.TravelRecordEntity
import com.unic.watchdog.data.db.TripDao
import com.unic.watchdog.data.db.TripEntity
import com.unic.watchdog.data.db.TripPointEntity
import com.unic.watchdog.model.FavoritePlace
import com.unic.watchdog.model.TravelRecord
import com.unic.watchdog.model.Trip
import com.unic.watchdog.model.TripPoint

class PlaceRepository(private val dao: FavoritePlaceDao) {
    fun observeAll() = dao.observeAll()
    suspend fun add(place: FavoritePlace) = dao.upsert(
        FavoritePlaceEntity(
            id = place.id, name = place.name, sub = place.sub,
            iconName = place.iconName, colorHex = place.colorHex,
            lat = place.latLng?.latitude, lng = place.latLng?.longitude
        )
    )

    suspend fun remove(id: String) = dao.delete(id)
}

class TripRepository(private val dao: TripDao) {
    fun observeAll() = dao.observeAll()
    fun observePoints(tripId: Long) = dao.observePoints(tripId)
    suspend fun create(title: String, coverAsset: String): Long =
        dao.insert(TripEntity(title = title, coverAsset = coverAsset))

    suspend fun rename(id: Long, title: String) = dao.rename(id, title)
    suspend fun delete(id: Long) = dao.delete(id)
    suspend fun addPoint(tripId: Long, point: TripPoint) = dao.insertPoint(
        TripPointEntity(
            tripId = tripId, name = point.name, time = point.time, type = point.type,
            lat = point.latLng?.latitude, lng = point.latLng?.longitude,
            orderIndex = point.orderIndex
        )
    )

    suspend fun deletePoint(id: Long) = dao.deletePoint(id)
    suspend fun clearPoints(tripId: Long) = dao.clearPoints(tripId)
}

class RecordRepository(private val dao: TravelRecordDao) {
    fun observeAll() = dao.observeAll()
    suspend fun add(record: TravelRecord) = dao.insert(
        TravelRecordEntity(
            title = record.title, date = record.date,
            distanceText = record.distanceText, durationText = record.durationText,
            pointNames = record.pointNames.joinToString(","), mode = record.mode
        )
    )

    suspend fun delete(id: Long) = dao.delete(id)
}

/** 把实体转换为展示模型（集中在此，便于复用）。 */
object EntityMappers {
    fun place(e: FavoritePlaceEntity) = FavoritePlace(
        id = e.id, name = e.name, sub = e.sub, iconName = e.iconName, colorHex = e.colorHex,
        latLng = if (e.lat != null && e.lng != null) LatLng(e.lat, e.lng) else null
    )

    fun trip(e: TripEntity, points: List<TripPointEntity>) = Trip(
        id = e.id, title = e.title, coverAsset = e.coverAsset,
        points = points.map {
            TripPoint(
                it.id, it.name, it.time, it.type,
                if (it.lat != null && it.lng != null) LatLng(it.lat, it.lng) else null,
                it.orderIndex
            )
        }
    )

    fun record(e: TravelRecordEntity) = TravelRecord(
        id = e.id, title = e.title, date = e.date,
        distanceText = e.distanceText, durationText = e.durationText,
        pointNames = e.pointNames.split(",").filter { it.isNotBlank() }, mode = e.mode
    )
}
