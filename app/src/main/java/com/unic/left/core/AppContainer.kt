package com.unic.left.core

import android.content.Context
import androidx.room.Room
import com.unic.left.data.db.AppDatabase
import com.unic.left.data.repository.PlaceRepository
import com.unic.left.data.repository.RecordRepository
import com.unic.left.data.repository.TripRepository

/** 手动服务定位（工程未引入 Hilt）。在 Application.onCreate 中 init 一次。 */
object AppContainer {
    private var db: AppDatabase? = null

    fun init(context: Context) {
        if (db == null) {
            db = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "left.db"
            ).build()
        }
    }

    private fun requireDb(): AppDatabase =
        db ?: throw IllegalStateException("AppContainer 未初始化")

    val placeRepo: PlaceRepository by lazy { PlaceRepository(requireDb().favoritePlaceDao()) }
    val tripRepo: TripRepository by lazy { TripRepository(requireDb().tripDao()) }
    val recordRepo: RecordRepository by lazy { RecordRepository(requireDb().travelRecordDao()) }
}
