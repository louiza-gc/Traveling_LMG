package com.example.traveling.TravelPath.Parcours

import java.io.Serializable

data class UserPreferences(
    val activities: List<String>,
    val favoritePlaces: List<String>,
    val mandatoryPlaces: List<String>,
    val budgetMax: Double,
    val durationHours: Int,
    val effortMax: Int,
    val sensitivity: Sensitivity,
    val weatherForecast: WeatherForecast
) : Serializable

data class WeatherForecast(
    val condition: String,
    val temperatureCelsius: Int,
    val humidityPercent: Int,
    val windSpeedKmh: Int
) : Serializable

data class Sensitivity(
    val cold: Boolean = false,
    val heat: Boolean = false,
    val humidity: Boolean = false
) : Serializable