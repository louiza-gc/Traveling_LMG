package com.example.traveling.TravelPath.Accueil

data class UserPreferences(
    val activities: List<String>,
    val favoritePlaces: List<String>,
    val mandatoryPlaces: List<String>,
    val budgetMax: Double,
    val durationHours: Int,
    val effortMax: Int,
    val sensitivity: Sensitivity,
    val weatherForecast: WeatherForecast
)

data class WeatherForecast(
    val condition: String,
    val temperatureCelsius: Int,
    val humidityPercent: Int,
    val windSpeedKmh: Int
)