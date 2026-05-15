package com.example.traveling.TravelPath.Parcours

import java.io.Serializable

data class Itinerary(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val placeIds: List<String> = emptyList(),
    val createdBy: String = "",
    val createdAt: Long = 0L,
    val totalCost: Double = 0.0,
    val totalDurationMinutes: Int = 0,
    val averageEffort: Double = 0.0,
    val steps: List<Step> = emptyList()
) : Serializable

data class Step(
    val order: Int = 0,
    val placeId: String = "",
    val arrivalTime: String = "",
    val departureTime: String = "",
    val durationMinutes: Int = 0,
    val cost: Double = 0.0,
    val effort: Int = 0,
    val distanceFromPreviousKm: Double = 0.0
) : Serializable