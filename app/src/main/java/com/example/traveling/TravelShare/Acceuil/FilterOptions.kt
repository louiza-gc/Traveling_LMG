package com.example.traveling.TravelShare.Acceuil

import java.io.Serializable

data class FilterOptions(
    val placeTypes: List<String> = emptyList(),  // ← Changé en List pour plusieurs
    val author: String = "",
    val location: String = "",
    val radius: String = "",
    val startDate: Long = 0,
    val endDate: Long = 0
) : Serializable