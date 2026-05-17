package com.example.traveling.TravelPath.Accueil

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class CategoryDetailActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var searchBar: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var fabGenerate: FloatingActionButton
    private lateinit var adapter: ItemAdapter
    private var currentCategoryName: String = ""
    private var currentCity: String = "Paris"
    private var allPlaces: List<Place> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_detail)

        currentCategoryName = intent.getStringExtra("CATEGORY_NAME") ?: ""
        currentCity = intent.getStringExtra("CITY") ?: "Paris"

        supportActionBar?.title = currentCategoryName

        recycler = findViewById(R.id.recyclerCategoryDetails)
        searchBar = findViewById(R.id.search)
        progressBar = findViewById(R.id.progressBar)
        fabGenerate = findViewById(R.id.fab_generate)

        recycler.layoutManager = LinearLayoutManager(this)

        loadPlacesByCategory()

        fabGenerate.setOnClickListener {
            val intent = Intent(this, com.example.traveling.TravelPath.Parcours.PreferencesActivity::class.java)
            intent.putExtra("CATEGORY_NAME", currentCategoryName)
            intent.putExtra("CITY", currentCity)
            startActivity(intent)
        }

        searchBar.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                performLocalSearch(s.toString())
            }
        })
    }

    private fun loadPlacesByCategory() {
        progressBar.visibility = ProgressBar.VISIBLE
        lifecycleScope.launch {
            try {
                PlaceRepository.init(applicationContext)
                val all = PlaceRepository.loadAllPlaces()
                allPlaces = all.filter { it.category == currentCategoryName }
                updateAdapter(allPlaces)
                if (allPlaces.isEmpty()) {
                    Toast.makeText(this@CategoryDetailActivity, "Aucun lieu trouvé pour cette catégorie", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@CategoryDetailActivity, "Erreur de chargement : ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                progressBar.visibility = ProgressBar.GONE
            }
        }
    }

    private fun updateAdapter(places: List<Place>) {
        // Récupérer l'état des favoris pour cet utilisateur (SharedPreferences avec userId)
        val userId = intent.getStringExtra("userId") ?: ""
        val prefsName = if (userId.isNotEmpty()) "travelpath_${userId}" else "travelpath_guest"
        val sharedPref = getSharedPreferences(prefsName, MODE_PRIVATE)
        val favoritesSet = sharedPref.getStringSet("favorites", emptySet()) ?: emptySet()

        val items = places.map { place ->
            Item(
                id = place.id,   // important
                name = place.name,
                imageUrl = place.media.thumbnail,
                city = place.location.city,
                country = place.location.country,
                isFavorite = favoritesSet.contains(place.id)
            )
        }

        adapter = ItemAdapter(
            items = items,
            onItemClick = { clickedItem ->
                val place = places.find { it.id == clickedItem.id }
                place?.let { showPlaceDialog(it) }
            },
            onFavoriteClick = { item -> toggleFavorite(item.id, sharedPref) },
            onAddClick = { item -> showAddToItineraryDialog(item.id) }
        )
        recycler.adapter = adapter
    }

    private fun performLocalSearch(query: String) {
        val lowerQuery = query.lowercase().trim()
        val filtered = if (lowerQuery.isEmpty()) {
            allPlaces
        } else {
            allPlaces.filter { place ->
                place.name.lowercase().contains(lowerQuery) ||
                        place.location.city.lowercase().contains(lowerQuery) ||
                        place.location.country.lowercase().contains(lowerQuery)
            }
        }
        updateAdapter(filtered)
        supportActionBar?.title = if (lowerQuery.isNotEmpty()) "Résultats : $query" else currentCategoryName
    }

    private fun toggleFavorite(placeId: String, sharedPref: android.content.SharedPreferences) {
        val favorites = sharedPref.getStringSet("favorites", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        if (favorites.contains(placeId)) {
            favorites.remove(placeId)
            Toast.makeText(this, "Retiré des favoris", Toast.LENGTH_SHORT).show()
        } else {
            favorites.add(placeId)
            Toast.makeText(this, "Ajouté aux favoris", Toast.LENGTH_SHORT).show()
        }
        sharedPref.edit().putStringSet("favorites", favorites).apply()
        // Recharger l'affichage pour mettre à jour l'icône
        updateAdapter(allPlaces.filter { it.category == currentCategoryName })
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

    // ==================== AJOUT À UN PARCOURS EXISTANT ====================

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