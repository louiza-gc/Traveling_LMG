package com.example.traveling.TravelPath.Accueil

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
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
import com.example.traveling.TravelPath.Parcours.AutoItineraryActivity
import com.example.traveling.TravelPath.Parcours.CreateItineraryActivity
import com.example.traveling.TravelPath.Parcours.MyItinerariesActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class AccueilPath : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var searchBar: EditText
    private lateinit var fabGenerate: FloatingActionButton
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var sharedPref: SharedPreferences
    private var userId: String = ""

    private val gridLayoutManager = GridLayoutManager(this, 2)
    private val listLayoutManager = LinearLayoutManager(this)

    private var allPlaces: List<Place> = emptyList()
    private var isSearchMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accueil_path)

        userId = intent.getStringExtra("userId") ?: ""
        val prefsName = if (userId.isNotEmpty()) "travelpath_${userId}" else "travelpath_guest"
        sharedPref = getSharedPreferences(prefsName, MODE_PRIVATE)

        recycler = findViewById(R.id.recyclerCategories)
        searchBar = findViewById(R.id.search)
        fabGenerate = findViewById(R.id.fab_generate_home)
        bottomNav = findViewById(R.id.bottomNav)

        showCategories()

        fabGenerate.setOnClickListener {
            val options = arrayOf("Créer un parcours personnalisé", "Générer un parcours automatique")
            AlertDialog.Builder(this)
                .setTitle("Nouveau parcours")
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
                    val intent = Intent(this, MyItinerariesActivity::class.java)
                    intent.putExtra("userId", userId)
                    startActivity(intent)
                    true
                }
                R.id.menu_path_favorites -> {
                    val intent = Intent(this, FavoritesActivity::class.java)
                    intent.putExtra("userId", userId)
                    startActivity(intent)
                    true
                }
                R.id.menu_path_profile -> {
                    val intent = Intent(this, TravelPathProfileActivity::class.java)
                    intent.putExtra("userId", userId) // si vous voulez passer l'UID
                    startActivity(intent)
                    true
                }
                else -> false
            }
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

        val favoritesSet = sharedPref.getStringSet("favorites", emptySet()) ?: emptySet()
        val items = results.map { place ->
            Item(
                name = place.name,
                assetPath = "",
                imageUrl = place.media.thumbnail,
                city = place.location.city,
                country = place.location.country,
                isFavorite = favoritesSet.contains(place.name)
            )
        }

        recycler.adapter = ItemAdapter(
            items = items,
            onItemClick = { clickedItem ->
                val place = results.find { it.name == clickedItem.name }
                place?.let { showPlaceDialog(it) }
            },
            onFavoriteClick = { item -> toggleFavorite(item.name) },
            onAddClick = { item -> addToSelection(item.name) }
        )
        if (items.isEmpty()) Toast.makeText(this, "Aucun résultat pour \"$query\"", Toast.LENGTH_SHORT).show()
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
        if (place.media.thumbnail.isNotBlank()) Glide.with(this).load(place.media.thumbnail).into(imageView)
        else imageView.setImageResource(android.R.drawable.ic_menu_gallery)
        AlertDialog.Builder(this).setView(dialogView).setPositiveButton("Fermer", null).show()
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