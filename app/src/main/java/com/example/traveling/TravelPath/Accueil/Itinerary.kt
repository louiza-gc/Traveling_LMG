package com.example.traveling.TravelPath.Accueil

import java.io.Serializable

data class Itinerary(
    val name: String,
    val description: String,
    val totalCost: Double,
    val totalDurationMinutes: Int,
    val averageEffort: Double,
    val steps: List<Step>,
    val weatherAlerts: List<String>,
    val openingHoursConflicts: List<String>
) : Serializable

data class Step(
    val order: Int,
    val placeId: String,
    val arrivalTime: String,
    val departureTime: String,
    val durationMinutes: Int,
    val cost: Double,
    val effort: Int,
    val distanceFromPreviousKm: Double
) : Serializable