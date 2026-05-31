package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherCacheDao {
    @Query("SELECT * FROM weather_cache WHERE locationName = :locationName")
    suspend fun getCache(locationName: String): WeatherCache?

    @Query("SELECT * FROM weather_cache")
    fun getAllCacheFlow(): Flow<List<WeatherCache>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCache(cache: WeatherCache)

    @Query("DELETE FROM weather_cache WHERE locationName = :locationName")
    suspend fun deleteCache(locationName: String)

    @Query("DELETE FROM weather_cache")
    suspend fun clearCache()
}

@Dao
interface TrackedLocationDao {
    @Query("SELECT * FROM tracked_locations ORDER BY locationName ASC")
    fun getAllTrackedLocationsFlow(): Flow<List<TrackedLocation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: TrackedLocation)

    @Delete
    suspend fun deleteLocation(location: TrackedLocation)

    @Query("DELETE FROM tracked_locations WHERE locationName = :name")
    suspend fun deleteLocationByName(name: String)
}

@Dao
interface WeatherAlertDao {
    @Query("SELECT * FROM weather_alerts ORDER BY timestamp DESC")
    fun getAllAlertsFlow(): Flow<List<WeatherAlert>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: WeatherAlert)

    @Query("UPDATE weather_alerts SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Int)

    @Query("DELETE FROM weather_alerts")
    suspend fun clearAllAlerts()
}

@Dao
interface AlertPreferenceDao {
    @Query("SELECT * FROM alert_preferences")
    fun getAllPreferencesFlow(): Flow<List<AlertPreference>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreference(preference: AlertPreference)

    @Query("SELECT * FROM alert_preferences WHERE alertType = :alertType")
    suspend fun getPreference(alertType: String): AlertPreference?
}
