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
import com.example.traveling.TravelPath.Parcours.MyItinerariesActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
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
    private var userId: String = ""
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_detail)

        currentCategoryName = intent.getStringExtra("CATEGORY_NAME") ?: ""
        currentCity = intent.getStringExtra("CITY") ?: "Paris"
        userId = intent.getStringExtra("userId") ?: FirebaseAuth.getInstance().currentUser?.uid ?: ""

        supportActionBar?.title = currentCategoryName

        recycler = findViewById(R.id.recyclerCategoryDetails)
        searchBar = findViewById(R.id.search)
        progressBar = findViewById(R.id.progressBar)
        fabGenerate = findViewById(R.id.fab_generate)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_path_favorites -> {
                    if (this !is FavoritesActivity) {
                        Intent(this, FavoritesActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        }.also { startActivity(it) }
                    }
                    true
                }
                R.id.menu_path_itineraries -> {
                    if (this !is MyItinerariesActivity) {
                        Intent(this, MyItinerariesActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        }.also { startActivity(it) }
                    }
                    true
                }
                R.id.menu_path_home -> true
                R.id.menu_path_profile -> {
                    if (this !is TravelPathProfileActivity) {
                        Intent(this, TravelPathProfileActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        }.also { startActivity(it) }
                    }
                    true
                }
                else -> false
            }
        }

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
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                performLocalSearch(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
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
        if (userId.isEmpty()) {
            buildAdapter(places, emptySet())
        } else {
            db.collection("favorites").document(userId).get()
                .addOnSuccessListener { doc ->
                    val favorites = (doc.get("placeIds") as? List<String>) ?: emptyList()
                    buildAdapter(places, favorites.toSet())
                }
                .addOnFailureListener {
                    buildAdapter(places, emptySet())
                }
        }
    }

    private fun buildAdapter(places: List<Place>, favoritesSet: Set<String>) {
        val items = places.map { place ->
            Item(
                id = place.id,
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
            onFavoriteClick = { item -> toggleFavorite(item.id) },
            onAddClick = { item -> showAddToItineraryDialog(item.id) }
        )
        recycler.adapter = adapter
    }

    private fun performLocalSearch(query: String) {
        val lowerQuery = query.lowercase().trim()
        val filtered = if (lowerQuery.isEmpty()) allPlaces else allPlaces.filter { place ->
            place.name.lowercase().contains(lowerQuery) ||
                    place.location.city.lowercase().contains(lowerQuery) ||
                    place.location.country.lowercase().contains(lowerQuery)
        }
        updateAdapter(filtered)
        supportActionBar?.title = if (lowerQuery.isNotEmpty()) "Résultats : $query" else currentCategoryName
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
                    Toast.makeText(this, if (newList.contains(placeId)) "Ajouté" else "Retiré", Toast.LENGTH_SHORT).show()
                    updateAdapter(allPlaces) // Rafraîchir
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erreur", Toast.LENGTH_SHORT).show()
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