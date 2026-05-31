package com.example.data.repository

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.data.api.WeatherApi
import com.example.data.database.*
import com.example.data.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class WeatherRepository(
    private val weatherApi: WeatherApi,
    private val weatherCacheDao: WeatherCacheDao,
    private val trackedLocationDao: TrackedLocationDao,
    private val weatherAlertDao: WeatherAlertDao,
    private val alertPreferenceDao: AlertPreferenceDao,
    private val context: Context
) {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val hourlyListType = Types.newParameterizedType(List::class.java, HourlyForecastItem::class.java)
    private val dailyListType = Types.newParameterizedType(List::class.java, DailyForecastItem::class.java)

    private val hourlyAdapter = moshi.adapter<List<HourlyForecastItem>>(hourlyListType)
    private val dailyAdapter = moshi.adapter<List<DailyForecastItem>>(dailyListType)

    // Flow lists
    val trackedLocations: Flow<List<TrackedLocation>> = trackedLocationDao.getAllTrackedLocationsFlow()
    val allAlerts: Flow<List<WeatherAlert>> = weatherAlertDao.getAllAlertsFlow()
    val alertPreferences: Flow<List<AlertPreference>> = alertPreferenceDao.getAllPreferencesFlow()

    init {
        createNotificationChannel()
        initializeDefaultPreferencesIfNeeded()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Weather Alerts"
            val descriptionText = "Push notifications for extreme weather changes and storm hazards"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("WEATHER_ALERTS_CHANNEL", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun initializeDefaultPreferencesIfNeeded() {
        // Run in background / repository coroutine
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val defaultPrefs = listOf(
                AlertPreference("HEAVY_RAIN", true, 10.0),      // rain > 10 mm/h
                AlertPreference("STRONG_WIND", true, 35.0),     // wind > 35 km/h
                AlertPreference("STORM_SURGE", true, 95.0),     // weather code >= 95 (Thunderstorms)
                AlertPreference("EXTREME_TEMP", false, 40.0)    // temp > 40C or < 10C
            )
            for (pref in defaultPrefs) {
                if (alertPreferenceDao.getPreference(pref.alertType) == null) {
                    alertPreferenceDao.insertPreference(pref)
                }
            }
            // Add Dhaka and Cox's Bazar by default
            trackedLocationDao.insertLocation(TrackedLocation("Dhaka", 23.8103, 90.4125, false))
            trackedLocationDao.insertLocation(TrackedLocation("Cox's Bazar", 21.4272, 92.0058, false))
        }
    }

    /**
     * Fetches current weather for custom latitude & longitude.
     * Integrates transparent local caching for seamless offline operations.
     */
    suspend fun getWeather(locationName: String, lat: Double, lon: Double): Result<WeatherState> {
        return try {
            val response = weatherApi.getForecast(latitude = lat, longitude = lon)
            val state = mapToWeatherState(locationName, response)

            // Cache in Database
            cacheWeatherState(state)

            // Analyze state and fire alerts if thresholds crossed
            checkForAlerts(state)

            Result.success(state)
        } catch (e: Exception) {
            // Retrieve Cache on Error (Offline Support)
            val cached = weatherCacheDao.getCache(locationName)
            if (cached != null) {
                Result.success(mapCachedToWeatherState(cached))
            } else {
                Result.failure(Exception("Offline mode: No cached weather data available for $locationName.", e))
            }
        }
    }

    private suspend fun cacheWeatherState(state: WeatherState) {
        val cache = WeatherCache(
            locationName = state.locationName,
            latitude = state.latitude,
            longitude = state.longitude,
            temperature = state.temperature,
            humidity = state.humidity,
            apparentTemperature = state.apparentTemperature,
            windSpeed = state.windSpeed,
            windDirection = state.windDirection,
            precipitation = state.precipitation,
            weatherCode = state.weatherCode,
            weatherCondition = state.weatherCondition,
            hourlyForecastJson = hourlyAdapter.toJson(state.hourlyForecast) ?: "[]",
            dailyForecastJson = dailyAdapter.toJson(state.dailyForecast) ?: "[]",
            lastUpdated = System.currentTimeMillis()
        )
        weatherCacheDao.insertCache(cache)
    }

    private fun mapCachedToWeatherState(cache: WeatherCache): WeatherState {
        val hourly = try {
            hourlyAdapter.fromJson(cache.hourlyForecastJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        val daily = try {
            dailyAdapter.fromJson(cache.dailyForecastJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        return WeatherState(
            locationName = cache.locationName,
            latitude = cache.latitude,
            longitude = cache.longitude,
            temperature = cache.temperature,
            humidity = cache.humidity,
            apparentTemperature = cache.apparentTemperature,
            windSpeed = cache.windSpeed,
            windDirection = cache.windDirection,
            precipitation = cache.precipitation,
            weatherCode = cache.weatherCode,
            weatherCondition = cache.weatherCondition,
            hourlyForecast = hourly,
            dailyForecast = daily,
            lastUpdated = cache.lastUpdated
        )
    }

    private fun mapToWeatherState(locationName: String, response: WeatherResponse): WeatherState {
        val current = response.current ?: throw Exception("Invalid API response: 'current' field is empty")
        
        // Parse hourly values
        val hourlyList = mutableListOf<HourlyForecastItem>()
        val hourlySource = response.hourly
        if (hourlySource != null) {
            val limit = minOf(24, hourlySource.time.size) // Next 24 hours
            for (i in 0 until limit) {
                val rawTime = hourlySource.time[i]
                val formattedTime = try {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("h a", Locale.getDefault())
                    val date = inputFormat.parse(rawTime)
                    if (date != null) outputFormat.format(date) else rawTime
                } catch (e: Exception) {
                    rawTime.substringAfter("T")
                }

                hourlyList.add(
                    HourlyForecastItem(
                        time = formattedTime,
                        temperature = hourlySource.temperatures[i],
                        weatherCode = hourlySource.weatherCodes?.getOrNull(i) ?: 0,
                        windSpeed = hourlySource.windSpeeds?.getOrNull(i) ?: 0.0
                    )
                )
            }
        }

        // Parse daily values
        val dailyList = mutableListOf<DailyForecastItem>()
        val dailySource = response.daily
        if (dailySource != null) {
            val limit = minOf(7, dailySource.time.size) // Next 7 days
            for (i in 0 until limit) {
                val rawDate = dailySource.time[i]
                val formattedDate = try {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
                    val date = inputFormat.parse(rawDate)
                    if (date != null) outputFormat.format(date) else rawDate
                } catch (e: Exception) {
                    rawDate
                }

                dailyList.add(
                    DailyForecastItem(
                        date = formattedDate,
                        weatherCode = dailySource.weatherCodes?.getOrNull(i) ?: 0,
                        maxTemp = dailySource.temperaturesMax?.getOrNull(i) ?: 0.0,
                        minTemp = dailySource.temperaturesMin?.getOrNull(i) ?: 0.0,
                        precipitation = dailySource.precipitationSum?.getOrNull(i) ?: 0.0
                    )
                )
            }
        }

        val code = current.weatherCode
        val condition = translateWeatherCode(code)

        return WeatherState(
            locationName = locationName,
            latitude = response.latitude,
            longitude = response.longitude,
            temperature = current.temperature,
            humidity = current.humidity ?: 0.0,
            apparentTemperature = current.apparentTemperature ?: current.temperature,
            windSpeed = current.windSpeed,
            windDirection = current.windDirection ?: 0.0,
            precipitation = current.precipitation ?: 0.0,
            weatherCode = code,
            weatherCondition = condition,
            hourlyForecast = hourlyList,
            dailyForecast = dailyList
        )
    }

    private suspend fun checkForAlerts(state: WeatherState) {
        val windPref = alertPreferenceDao.getPreference("STRONG_WIND") ?: AlertPreference("STRONG_WIND", true, 35.0)
        val rainPref = alertPreferenceDao.getPreference("HEAVY_RAIN") ?: AlertPreference("HEAVY_RAIN", true, 10.0)
        val stormPref = alertPreferenceDao.getPreference("STORM_SURGE") ?: AlertPreference("STORM_SURGE", true, 95.0)
        val tempPref = alertPreferenceDao.getPreference("EXTREME_TEMP") ?: AlertPreference("EXTREME_TEMP", false, 40.0)

        // Rule 1: High Wind Speed
        if (windPref.isEnabled && state.windSpeed >= windPref.thresholdValue) {
            triggerAlert(
                title = "Cyclone / Gale Risk Alert",
                description = "High wind speed of ${state.windSpeed} km/h detected in ${state.locationName}! Secure property; extreme hazards expected in 15-30 minutes.",
                intensity = "Extreme",
                locationName = state.locationName
            )
        }

        // Rule 2: Heavy Torrential Rain
        if (rainPref.isEnabled && state.precipitation >= rainPref.thresholdValue) {
            triggerAlert(
                title = "Severe Flash Flood Alert",
                description = "Heavy rainfall of ${state.precipitation} mm detected in ${state.locationName}. High risk of local flash floods and waterlogging starting in 15-30 mins.",
                intensity = "Severe",
                locationName = state.locationName
            )
        }

        // Rule 3: Extreme Weather Codes (Thunderstorms, hail, storms)
        if (stormPref.isEnabled && state.weatherCode >= 95) {
            triggerAlert(
                title = "Severe Thunderstorm Warning",
                description = "Severe thunderstorm and lightning activity moving over ${state.locationName}. High risk of lightning strikes and hail in 15-30 minutes. Stay indoors!",
                intensity = "Severe",
                locationName = state.locationName
            )
        }

        // Rule 4: Extreme Temp Heat/Cold
        if (tempPref.isEnabled && (state.temperature >= tempPref.thresholdValue || state.temperature <= 10.0)) {
            val desc = if (state.temperature >= tempPref.thresholdValue) {
                "Extreme heat waves of ${state.temperature}°C in ${state.locationName}. Limit outdoor activities."
            } else {
                "Cold wave warning of ${state.temperature}°C in ${state.locationName}. Stay warm!"
            }
            triggerAlert(
                title = "Temperature Advisory",
                description = desc,
                intensity = "Moderate",
                locationName = state.locationName
            )
        }
    }

    private suspend fun triggerAlert(
        title: String,
        description: String,
        intensity: String,
        locationName: String
    ) {
        // Avoid duplicate alerts in same area within recent timeframe
        // Insert into Room
        val alert = WeatherAlert(
            title = title,
            description = description,
            intensity = intensity,
            locationName = locationName
        )
        weatherAlertDao.insertAlert(alert)

        // Trigger Local Push Notification
        showNotification(title, description)
    }

    private fun showNotification(title: String, message: String) {
        val builder = NotificationCompat.Builder(context, "WEATHER_ALERTS_CHANNEL")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    // Custom Alert triggering for Manual simulation / instant test
    suspend fun simulateManualAlert(locationName: String, title: String, description: String, intensity: String) {
        triggerAlert(title, description, intensity, locationName)
    }

    suspend fun addTrackedLocation(location: PresetLocation) {
        trackedLocationDao.insertLocation(
            TrackedLocation(
                locationName = location.name,
                latitude = location.latitude,
                longitude = location.longitude
            )
        )
    }

    suspend fun addTrackedLocationByCustom(name: String, lat: Double, lon: Double) {
        trackedLocationDao.insertLocation(
            TrackedLocation(
                locationName = name,
                latitude = lat,
                longitude = lon
            )
        )
    }

    suspend fun removeTrackedLocationByName(name: String) {
        trackedLocationDao.deleteLocationByName(name)
    }

    suspend fun savePreference(preference: AlertPreference) {
        alertPreferenceDao.insertPreference(preference)
    }

    fun translateWeatherCode(code: Int): String {
        return when (code) {
            0 -> "Clear Sky"
            1, 2, 3 -> "Partly Cloudy"
            45, 48 -> "Foggy conditions"
            51, 53, 55 -> "Drizzling Mist"
            56, 57 -> "Freezing Drizzle"
            61, 63 -> "Showers of Rain"
            65 -> "Heavy Downpour"
            66, 67 -> "Freezing Rain"
            71, 73 -> "Slight Snowfall"
            75, 77 -> "Blizzard / Snowfall"
            80, 81, 82 -> "Rain Showers"
            85, 86 -> "Snow Showers"
            95 -> "Thunderstorm Alert"
            96, 99 -> "Severe Hail Thunderstorm"
            else -> "Unknown Weather"
        }
    }
}
