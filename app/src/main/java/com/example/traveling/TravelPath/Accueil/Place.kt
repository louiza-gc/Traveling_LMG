package com.example.traveling.TravelPath.Accueil

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Place(
    val id: String,
    val name: String,
    val category: String,
    val subcategory: String,
    val location: Location,
    val media: Media,
    val details: Details,
    @SerializedName("indoor_outdoor") val indoorOutdoor: String,
    val activities: List<String>,
    val sensitivity: Sensitivity
) : Serializable

data class Location(
    val lat: Double,
    val lon: Double,
    val city: String,
    val country: String,
    val address: String
) : Serializable

data class Media(
    val thumbnail: String,
    val images: List<String>,
    val videos: List<String>
) : Serializable

data class Details(
    val description: String,
    @SerializedName("opening_hours") val openingHours: Map<String, String>,
    @SerializedName("typical_duration_minutes") val typicalDurationMinutes: Int,
    @SerializedName("cost_estimate") val costEstimate: CostEstimate,
    @SerializedName("effort_level") val effortLevel: Int,
    val tags: List<String>
) : Serializable

data class CostEstimate(
    val adult: Double,
    val child: Double,
    val currency: String,
    @SerializedName("price_category") val priceCategory: String
) : Serializable

data class Sensitivity(
    val cold: Boolean = false,
    val heat: Boolean = false,
    val humidity: Boolean = false
) : Serializable