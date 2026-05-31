package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WeatherResponse(
    val latitude: Double,
    val longitude: Double,
    val current: CurrentWeather?,
    val hourly: HourlyWeather?,
    val daily: DailyWeather?
)

@JsonClass(generateAdapter = true)
data class CurrentWeather(
    val time: String,
    @Json(name = "temperature_2m") val temperature: Double,
    @Json(name = "relative_humidity_2m") val humidity: Double?,
    @Json(name = "apparent_temperature") val apparentTemperature: Double?,
    val precipitation: Double?,
    val rain: Double?,
    val showers: Double?,
    @Json(name = "weather_code") val weatherCode: Int,
    @Json(name = "wind_speed_10m") val windSpeed: Double,
    @Json(name = "wind_direction_10m") val windDirection: Double?
)

@JsonClass(generateAdapter = true)
data class HourlyWeather(
    val time: List<String>,
    @Json(name = "temperature_2m") val temperatures: List<Double>,
    @Json(name = "relative_humidity_2m") val humidities: List<Double>?,
    @Json(name = "weather_code") val weatherCodes: List<Int>?,
    @Json(name = "wind_speed_10m") val windSpeeds: List<Double>?
)

@JsonClass(generateAdapter = true)
data class DailyWeather(
    val time: List<String>,
    @Json(name = "weather_code") val weatherCodes: List<Int>?,
    @Json(name = "temperature_2m_max") val temperaturesMax: List<Double>?,
    @Json(name = "temperature_2m_min") val temperaturesMin: List<Double>?,
    @Json(name = "precipitation_sum") val precipitationSum: List<Double>?
)

// UI representation of weather state
data class WeatherState(
    val locationName: String,
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
    val hourlyForecast: List<HourlyForecastItem>,
    val dailyForecast: List<DailyForecastItem>,
    val lastUpdated: Long = System.currentTimeMillis()
)

data class HourlyForecastItem(
    val time: String,
    val temperature: Double,
    val weatherCode: Int,
    val windSpeed: Double
)

data class DailyForecastItem(
    val date: String,
    val weatherCode: Int,
    val maxTemp: Double,
    val minTemp: Double,
    val precipitation: Double
)
