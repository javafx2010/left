package com.unic.watchdog.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_places")
data class FavoritePlaceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sub: String,
    val iconName: String,
    val colorHex: String,
    val lat: Double?,
    val lng: Double?
)

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val coverAsset: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "trip_points")
data class TripPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val name: String,
    val time: String,
    val type: String,
    val lat: Double?,
    val lng: Double?,
    val orderIndex: Int
)

@Entity(tableName = "travel_records")
data class TravelRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val date: String,
    val distanceText: String,
    val durationText: String,
    val pointNames: String, // 逗号分隔
    val mode: String,
    val createdAt: Long = System.currentTimeMillis()
)
