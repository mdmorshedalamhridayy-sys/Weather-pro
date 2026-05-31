package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather_cache")
data class WeatherCache(
    @PrimaryKey val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val temperature: Double,
    val humidity: Double,
    val apparentTemperature: Double,
    val windSpeed: Double,
    val windDirection: Double,
    val precipitation: Double,
    val weatherCode: Int,
    val weatherCondition: String,
    val hourlyForecastJson: String, // Moshi serialized JSON for simple offline loading
    val dailyForecastJson: String,  // Moshi serialized JSON for simple offline loading
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "tracked_locations")
data class TrackedLocation(
    @PrimaryKey val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val isUserCurrentLocation: Boolean = false
)

@Entity(tableName = "weather_alerts")
data class WeatherAlert(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val intensity: String, // Info, Moderate, Severe, Extreme
    val locationName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

@Entity(tableName = "alert_preferences")
data class AlertPreference(
    @PrimaryKey val alertType: String, // e.g. "HEAVY_RAIN", "STRONG_WIND", "HEAT_WARNING", "STORM_SURGE"
    val isEnabled: Boolean = true,
    val thresholdValue: Double // Customize trigger point, e.g. 50 km/h wind, 10 mm/hr rain
)
