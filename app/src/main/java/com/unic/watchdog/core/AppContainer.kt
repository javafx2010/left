package com.unic.watchdog.core

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.unic.watchdog.data.db.AppDatabase
import com.unic.watchdog.data.repository.PlaceRepository
import com.unic.watchdog.data.repository.RecordRepository
import com.unic.watchdog.data.repository.TripRepository
import com.unic.watchdog.tts.TtsSpeaker

/** 手动服务定位（工程未引入 Hilt）。在 Application.onCreate 中 init 一次。 */
object AppContainer {
    private var db: AppDatabase? = null
    private var _ttsSpeaker: TtsSpeaker? = null

    // v2：清除旧版原型示例数据，由 SeedData 重新写入默认常用地点。
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DELETE FROM favorite_places")
            db.execSQL("DELETE FROM trips")
            db.execSQL("DELETE FROM trip_points")
            db.execSQL("DELETE FROM travel_records")
        }
    }

    fun init(context: Context) {
        if (db == null) {
            db = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "watchdog.db"
            )
                .addMigrations(MIGRATION_1_2)
                .build()
        }
        if (_ttsSpeaker == null) {
            _ttsSpeaker = TtsSpeaker(context)
        }
    }

    private fun requireDb(): AppDatabase =
        db ?: throw IllegalStateException("AppContainer 未初始化")

    val placeRepo: PlaceRepository by lazy { PlaceRepository(requireDb().favoritePlaceDao()) }
    val tripRepo: TripRepository by lazy { TripRepository(requireDb().tripDao()) }
    val recordRepo: RecordRepository by lazy { RecordRepository(requireDb().travelRecordDao()) }
    val ttsSpeaker: TtsSpeaker
        get() = _ttsSpeaker ?: throw IllegalStateException("AppContainer 未初始化")
}
