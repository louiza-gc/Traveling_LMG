package com.example.traveling.TravelPath.Accueil

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.traveling.R
import kotlinx.coroutines.launch

class FavoritesActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var sharedPref: SharedPreferences
    private var userId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        userId = intent.getStringExtra("userId") ?: ""
        val prefsName = if (userId.isNotEmpty()) "travelpath_${userId}" else "travelpath_guest"
        sharedPref = getSharedPreferences(prefsName, MODE_PRIVATE)

        recycler = findViewById(R.id.recyclerFavorites)
        recycler.layoutManager = LinearLayoutManager(this)

        loadFavorites()
    }

    private fun loadFavorites() {
        val favoriteNames = sharedPref.getStringSet("favorites", emptySet()) ?: emptySet()
        if (favoriteNames.isEmpty()) {
            Toast.makeText(this, "Aucun favori", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch {
            try {
                PlaceRepository.init(applicationContext)
                val allPlaces = PlaceRepository.loadAllPlaces()
                val favoritePlaces = allPlaces.filter { it.name in favoriteNames }
                val items = favoritePlaces.map { place ->
                    Item(
                        name = place.name,
                        assetPath = "",
                        imageUrl = place.media.thumbnail,
                        city = place.location.city,
                        country = place.location.country,
                        isFavorite = true
                    )
                }
                val adapter = ItemAdapter(
                    items = items,
                    onItemClick = { clickedItem ->
                        val place = favoritePlaces.find { it.name == clickedItem.name }
                        place?.let { showPlaceDialog(it) }
                    },
                    onFavoriteClick = { item -> toggleFavorite(item.name) },
                    onAddClick = { item -> addToSelection(item.name) }
                )
                recycler.adapter = adapter
            } catch (e: Exception) {
                Toast.makeText(this@FavoritesActivity, "Erreur de chargement", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showPlaceDialog(place: Place) {
        val dialogView = layoutInflater.inflate(R.layout.activity_place_detail, null)
        dialogView.findViewById<TextView>(R.id.place_name).text = place.name
        dialogView.findViewById<TextView>(R.id.place_description).text = place.details.description
        dialogView.findViewById<TextView>(R.id.place_cost).text = "Coût : %.2f €".format(place.details.costEstimate.adult)
        dialogView.findViewById<TextView>(R.id.place_effort).text = "Effort : ${place.details.effortLevel}/5"
        dialogView.findViewById<TextView>(R.id.place_duration).text = "Durée : ${place.details.typicalDurationMinutes} min"
        dialogView.findViewById<TextView>(R.id.place_address).text = "Adresse : ${place.location.address}"
        val tagsText = if (place.details.tags.isNotEmpty()) "Tags : ${place.details.tags.joinToString(", ")}" else ""
        dialogView.findViewById<TextView>(R.id.place_tags).text = tagsText
        val imageView = dialogView.findViewById<ImageView>(R.id.place_image)
        if (place.media.thumbnail.isNotBlank()) {
            Glide.with(this).load(place.media.thumbnail).into(imageView)
        } else {
            imageView.setImageResource(android.R.drawable.ic_menu_gallery)
        }
        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Fermer", null)
            .show()
    }

    private fun toggleFavorite(placeName: String) {
        val favorites = sharedPref.getStringSet("favorites", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        if (favorites.contains(placeName)) {
            favorites.remove(placeName)
            Toast.makeText(this, "$placeName retiré des favoris", Toast.LENGTH_SHORT).show()
        } else {
            favorites.add(placeName)
            Toast.makeText(this, "$placeName ajouté aux favoris", Toast.LENGTH_SHORT).show()
        }
        sharedPref.edit().putStringSet("favorites", favorites).apply()
        // Recharger la liste des favoris
        loadFavorites()
    }

    private fun addToSelection(placeName: String) {
        val selection = sharedPref.getStringSet("to_see", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        if (selection.contains(placeName)) {
            Toast.makeText(this, "$placeName déjà dans votre sélection", Toast.LENGTH_SHORT).show()
        } else {
            selection.add(placeName)
            sharedPref.edit().putStringSet("to_see", selection).apply()
            Toast.makeText(this, "$placeName ajouté à votre sélection", Toast.LENGTH_SHORT).show()
        }
    }
}