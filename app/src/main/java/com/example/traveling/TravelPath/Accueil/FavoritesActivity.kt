package com.example.traveling.TravelPath.Accueil

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
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
import com.example.traveling.TravelPath.Parcours.Itinerary
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class FavoritesActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var sharedPref: SharedPreferences
    private var userId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        // Récupérer l'utilisateur (soit depuis l'intent, soit via FirebaseAuth)
        userId = intent.getStringExtra("userId") ?: FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val prefsName = if (userId.isNotEmpty()) "travelpath_${userId}" else "travelpath_guest"
        sharedPref = getSharedPreferences(prefsName, MODE_PRIVATE)

        recycler = findViewById(R.id.recyclerFavorites)
        recycler.layoutManager = LinearLayoutManager(this)

        loadFavorites()
    }

    private fun loadFavorites() {
        val favoriteIds = sharedPref.getStringSet("favorites", emptySet()) ?: emptySet()
        if (favoriteIds.isEmpty()) {
            Toast.makeText(this, "Aucun favori", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch {
            try {
                PlaceRepository.init(applicationContext)
                val allPlaces = PlaceRepository.loadAllPlaces()
                val favoritePlaces = allPlaces.filter { it.id in favoriteIds }

                val items = favoritePlaces.map { place ->
                    Item(
                        id = place.id,
                        name = place.name,
                        imageUrl = place.media.thumbnail,
                        city = place.location.city,
                        country = place.location.country,
                        isFavorite = true   // cœur rempli (rouge)
                    )
                }

                val adapter = ItemAdapter(
                    items = items,
                    onItemClick = { clickedItem ->
                        val place = favoritePlaces.find { it.id == clickedItem.id }
                        place?.let { showPlaceDialog(it) }
                    },
                    onFavoriteClick = { item -> removeFavorite(item.id) },  // retirer des favoris
                    onAddClick = { item -> showAddToItineraryDialog(item.id) }
                )
                recycler.adapter = adapter
            } catch (e: Exception) {
                Toast.makeText(this@FavoritesActivity, "Erreur de chargement", Toast.LENGTH_SHORT).show()
            }
        }
        Log.d("FAVORI", "userId récupéré = $userId, prefsName = travelpath_${userId}")
        Log.d("FAVORI", "Contenu favorites : ${sharedPref.getStringSet("favorites", emptySet())}")
    }

    // Retirer un lieu des favoris
    private fun removeFavorite(placeId: String) {
        val favorites = sharedPref.getStringSet("favorites", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        if (favorites.remove(placeId)) {
            sharedPref.edit().putStringSet("favorites", favorites).apply()
            Toast.makeText(this, "Retiré des favoris", Toast.LENGTH_SHORT).show()
            loadFavorites()  // rafraîchir la liste
        }
    }

    // Afficher les détails d'un lieu (popup)
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

    // Ajouter un lieu à un parcours existant (comme dans AccueilPath)
    private fun showAddToItineraryDialog(placeId: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Connectez-vous pour ajouter à un parcours", Toast.LENGTH_SHORT).show()
            return
        }
        val db = FirebaseFirestore.getInstance()
        db.collection("itineraries")
            .whereEqualTo("createdBy", uid)
            .get()
            .addOnSuccessListener { result ->
                val itineraries = result.documents.mapNotNull { doc ->
                    Itinerary(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        description = doc.getString("description") ?: "",
                        placeIds = (doc.get("placeIds") as? List<String>) ?: emptyList()
                    )
                }
                if (itineraries.isEmpty()) {
                    Toast.makeText(this, "Aucun parcours. Créez-en un d'abord.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                val names = itineraries.map { it.name }.toTypedArray()
                AlertDialog.Builder(this)
                    .setTitle("Ajouter à un parcours")
                    .setItems(names) { _, which ->
                        val selected = itineraries[which]
                        addPlaceToItinerary(selected.id, placeId, selected.placeIds)
                    }
                    .show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erreur de chargement des parcours", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addPlaceToItinerary(itineraryId: String, placeId: String, currentPlaceIds: List<String>) {
        if (currentPlaceIds.contains(placeId)) {
            Toast.makeText(this, "Ce lieu est déjà dans le parcours", Toast.LENGTH_SHORT).show()
            return
        }
        val newPlaceIds = currentPlaceIds.toMutableList().apply { add(placeId) }
        FirebaseFirestore.getInstance().collection("itineraries").document(itineraryId)
            .update("placeIds", newPlaceIds)
            .addOnSuccessListener {
                Toast.makeText(this, "Lieu ajouté au parcours", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}