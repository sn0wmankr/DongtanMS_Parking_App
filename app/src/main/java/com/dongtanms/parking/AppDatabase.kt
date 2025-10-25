package com.dongtanms.parking

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ParkingEntry::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun parkingDao(): ParkingDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "parking_db"
                )
                    // ⚙️ 아래 한 줄 추가 (DB 스키마 변경 시 앱이 튕기지 않게)
                    .fallbackToDestructiveMigration()
                    // ⚙️ DB를 안전하게 유지하기 위해 write-ahead logging 활성화
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                    .build()
                INSTANCE = instance
                instance
            }
        }

    }
}
