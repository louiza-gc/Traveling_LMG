package com.example.traveling.TravelPath.Accueil

data class Item(
    val id: String,          // id du lieu (ex: "eiffel_tower")
    val name: String,
    val imageUrl: String,
    val city: String,
    val country: String,
    var isFavorite: Boolean = false
)