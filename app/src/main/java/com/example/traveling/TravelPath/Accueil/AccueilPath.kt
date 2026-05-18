package com.example.traveling.TravelPath.Accueil

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.traveling.R
import com.example.traveling.TravelPath.Parcours.CreateItineraryActivity
import com.example.traveling.TravelPath.Parcours.Itinerary
import com.example.traveling.TravelPath.Parcours.MyItinerariesActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class AccueilPath : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var searchBar: EditText
    private lateinit var fabGenerate: FloatingActionButton
    private lateinit var bottomNav: BottomNavigationView

    private val gridLayoutManager = GridLayoutManager(this, 2)
    private val listLayoutManager = LinearLayoutManager(this)

    private var allPlaces: List<Place> = emptyList()
    private var isSearchMode = false
    private var userId: String = ""
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accueil_path)

        userId = intent.getStringExtra("userId") ?: FirebaseAuth.getInstance().currentUser?.uid ?: ""

        recycler = findViewById(R.id.recyclerCategories)
        searchBar = findViewById(R.id.search)
        fabGenerate = findViewById(R.id.fab_generate_home)
        bottomNav = findViewById(R.id.bottomNav)

        showCategories()

        fabGenerate.setOnClickListener {
            val options = arrayOf("Parcours personnalisé", "Parcours automatique")
            AlertDialog.Builder(this)
                .setTitle("Créer un parcours")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> startActivity(Intent(this, CreateItineraryActivity::class.java))
                        1 -> startActivity(Intent(this, AutoItineraryActivity::class.java))
                    }
                }
                .show()
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_path_home -> true
                R.id.menu_path_itineraries -> {
                    startActivity(Intent(this, MyItinerariesActivity::class.java))
                    true
                }
                R.id.menu_path_favorites -> {
                    val intent = Intent(this, FavoritesActivity::class.java)
                    intent.putExtra("userId", userId)
                    startActivity(intent)
                    true
                }
                R.id.menu_path_profile -> {
                    startActivity(Intent(this, TravelPathProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
        bottomNav.selectedItemId = when (this) {
            is AccueilPath -> R.id.menu_path_home
            is MyItinerariesActivity -> R.id.menu_path_itineraries
            is FavoritesActivity -> R.id.menu_path_favorites
            is TravelPathProfileActivity -> R.id.menu_path_profile
            else -> R.id.menu_path_home
        }

        searchBar.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                performSearch(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        lifecycleScope.launch {
            PlaceRepository.init(applicationContext)
            allPlaces = PlaceRepository.loadAllPlaces()
        }
    }

    private fun showCategories() {
        isSearchMode = false
        recycler.layoutManager = gridLayoutManager
        val categories = listOf(
            Category("Nature", R.drawable.nature),
            Category("Monuments", R.drawable.monuments),
            Category("Musées", R.drawable.musee),
            Category("Restaurants", R.drawable.food),
            Category("Vie nocturne", R.drawable.nightlife)
        )
        recycler.adapter = CategoryAdapter(categories)
    }

    private fun performSearch(query: String) {
        if (query.isBlank()) {
            if (isSearchMode) showCategories()
            return
        }
        if (allPlaces.isEmpty()) return

        val keywords = query.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val results = allPlaces.filter { place ->
            val searchText = "${place.name} ${place.location.city} ${place.location.country}".lowercase()
            keywords.all { searchText.contains(it) }
        }

        if (!isSearchMode) {
            isSearchMode = true
            recycler.layoutManager = listLayoutManager
        }

        if (userId.isEmpty()) {
            updateResults(results, emptySet())
        } else {
            db.collection("favorites").document(userId).get()
                .addOnSuccessListener { doc ->
                    val favorites = (doc.get("placeIds") as? List<String>) ?: emptyList()
                    updateResults(results, favorites.toSet())
                }
                .addOnFailureListener {
                    updateResults(results, emptySet())
                }
        }
    }

    private fun updateResults(results: List<Place>, favoritesSet: Set<String>) {
        val items = results.map { place ->
            Item(
                id = place.id,
                name = place.name,
                imageUrl = place.media.thumbnail,
                city = place.location.city,
                country = place.location.country,
                isFavorite = favoritesSet.contains(place.id)
            )
        }

        recycler.adapter = ItemAdapter(
            items = items,
            onItemClick = { clickedItem ->
                val place = results.find { it.id == clickedItem.id }
                place?.let { showPlaceDialog(it) }
            },
            onFavoriteClick = { item -> toggleFavorite(item.id) },
            onAddClick = { item -> showAddToItineraryDialog(item.id) }
        )
        if (items.isEmpty()) {
            Toast.makeText(this, "Aucun résultat pour \"${searchBar.text}\"", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleFavorite(placeId: String) {
        if (userId.isEmpty()) {
            Toast.makeText(this, "Connectez-vous pour gérer les favoris", Toast.LENGTH_SHORT).show()
            return
        }
        val docRef = db.collection("favorites").document(userId)
        docRef.get().addOnSuccessListener { doc ->
            val currentList = (doc.get("placeIds") as? List<String>) ?: emptyList()
            val newList = if (currentList.contains(placeId)) {
                currentList - placeId
            } else {
                currentList + placeId
            }
            docRef.set(mapOf("placeIds" to newList))
                .addOnSuccessListener {
                    Toast.makeText(this, if (newList.contains(placeId)) "Ajouté aux favoris" else "Retiré des favoris", Toast.LENGTH_SHORT).show()
                    performSearch(searchBar.text.toString()) // Rafraîchir l'affichage
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erreur de mise à jour", Toast.LENGTH_SHORT).show()
                }
        }
    }

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
}