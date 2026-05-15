package com.example.traveling.TravelPath.Accueil

data class Item(
    val name: String,
    val assetPath: String = "",
    val imageUrl: String = "",
    val city: String = "",
    val country: String = "",
    var isFavorite: Boolean = false
)