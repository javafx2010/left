package com.unic.left.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritePlaceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: FavoritePlaceEntity)

    @Query("DELETE FROM favorite_places WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM favorite_places ORDER BY rowid ASC")
    fun observeAll(): Flow<List<FavoritePlaceEntity>>
}

@Dao
interface TripDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trip: TripEntity): Long

    @Query("SELECT * FROM trips ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TripEntity>>

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE trips SET title = :title WHERE id = :id")
    suspend fun rename(id: Long, title: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoint(point: TripPointEntity): Long

    @Query("DELETE FROM trip_points WHERE id = :id")
    suspend fun deletePoint(id: Long)

    @Query("DELETE FROM trip_points WHERE tripId = :tripId")
    suspend fun clearPoints(tripId: Long)

    @Query("SELECT * FROM trip_points WHERE tripId = :tripId ORDER BY orderIndex ASC")
    fun observePoints(tripId: Long): Flow<List<TripPointEntity>>
}

@Dao
interface TravelRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: TravelRecordEntity): Long

    @Query("SELECT * FROM travel_records ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TravelRecordEntity>>

    @Query("DELETE FROM travel_records WHERE id = :id")
    suspend fun delete(id: Long)
}
