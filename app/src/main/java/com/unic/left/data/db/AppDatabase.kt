package com.unic.left.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        FavoritePlaceEntity::class,
        TripEntity::class,
        TripPointEntity::class,
        TravelRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoritePlaceDao(): FavoritePlaceDao
    abstract fun tripDao(): TripDao
    abstract fun travelRecordDao(): TravelRecordDao
}
