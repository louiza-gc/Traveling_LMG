package com.example.traveling.TravelPath.Parcours

import com.example.traveling.TravelPath.Accueil.Place
import kotlin.math.*

object ItineraryPlanner {

    fun generateItineraries(places: List<Place>, prefs: UserPreferences): List<Itinerary> {
        val candidates = places.filter { place ->
            prefs.activities.any { it in place.activities } &&
                    place.details.costEstimate.adult <= prefs.budgetMax &&
                    place.details.effortLevel <= prefs.effortMax
        }
        if (candidates.isEmpty()) return emptyList()
        val economic = candidates.sortedBy { it.details.costEstimate.adult }
        val balanced = candidates.sortedBy { it.details.effortLevel }
        val comfort = candidates.shuffled()
        return listOf(
            buildItinerary("Économique", "Parcours low-cost", economic.take(4)),
            buildItinerary("Équilibré", "Compromis coût/effort", balanced.take(4)),
            buildItinerary("Confort", "Parcours premium", comfort.take(4))
        )
    }

    private fun buildItinerary(name: String, desc: String, selected: List<Place>): Itinerary {
        var totalCost = 0.0
        var totalDuration = 0
        var totalEffort = 0
        val steps = mutableListOf<Step>()
        var currentTime = 9
        var previousLatLon: Pair<Double, Double>? = null

        selected.forEachIndexed { index, place ->
            val duration = place.details.typicalDurationMinutes
            val cost = place.details.costEstimate.adult
            totalCost += cost
            totalDuration += duration
            totalEffort += place.details.effortLevel

            val distance = if (previousLatLon == null) 0.0
            else distanceInKm(previousLatLon.first, previousLatLon.second, place.location.lat, place.location.lon)

            steps.add(
                Step(
                    order = index + 1,
                    placeId = place.id,
                    arrivalTime = String.format("%02d:%02d", currentTime, 0),
                    departureTime = String.format("%02d:%02d", currentTime + duration / 60, duration % 60),
                    durationMinutes = duration,
                    cost = cost,
                    effort = place.details.effortLevel,
                    distanceFromPreviousKm = distance
                )
            )
            currentTime += duration / 60
            previousLatLon = Pair(place.location.lat, place.location.lon)
        }

        val avgEffort = if (selected.isNotEmpty()) totalEffort.toDouble() / selected.size else 0.0
        return Itinerary(
            name = name,
            description = desc,
            totalCost = totalCost,
            totalDurationMinutes = totalDuration,
            averageEffort = avgEffort,
            steps = steps,
            placeIds = steps.map { it.placeId }

        )

    }

    private fun distanceInKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }
}