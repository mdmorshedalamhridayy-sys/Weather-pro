package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        WeatherCache::class,
        TrackedLocation::class,
        WeatherAlert::class,
        AlertPreference::class
    ],
    version = 1,
    exportSchema = false
)
abstract class WeatherDatabase : RoomDatabase() {

    abstract fun weatherCacheDao(): WeatherCacheDao
    abstract fun trackedLocationDao(): TrackedLocationDao
    abstract fun weatherAlertDao(): WeatherAlertDao
    abstract fun alertPreferenceDao(): AlertPreferenceDao

    companion object {
        @Volatile
        private var INSTANCE: WeatherDatabase? = null

        fun getDatabase(context: Context): WeatherDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WeatherDatabase::class.java,
                    "weather_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
