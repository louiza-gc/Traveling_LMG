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
import com.google.android.material.floatingactionbutton.FloatingActionButton
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
            override fun afterTextChanged(s: android.text.Editable?) { performLocalSearch(s.toString()) }
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
                if (allPlaces.isEmpty()) Toast.makeText(this@CategoryDetailActivity, "Aucun lieu", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) { Toast.makeText(this@CategoryDetailActivity, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally { progressBar.visibility = ProgressBar.GONE }
        }
    }

    private fun updateAdapter(places: List<Place>) {
        val items = places.map { Item(it.name, "", it.media.thumbnail, it.location.city, it.location.country) }
        adapter = ItemAdapter(items,
            onItemClick = { clicked -> places.find { it.name == clicked.name }?.let { showPlaceDialog(it) } },
            onFavoriteClick = { item -> toggleFavorite(item.name) },
            onAddClick = { item -> addToSelection(item.name) }
        )
        recycler.adapter = adapter
    }

    private fun performLocalSearch(query: String) {
        val lower = query.lowercase().trim()
        val filtered = if (lower.isEmpty()) allPlaces else allPlaces.filter {
            it.name.lowercase().contains(lower) ||
                    it.location.city.lowercase().contains(lower) ||
                    it.location.country.lowercase().contains(lower)
        }
        updateAdapter(filtered)
        supportActionBar?.title = if (lower.isNotEmpty()) "Résultats : $query" else currentCategoryName
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
        val prefs = getSharedPreferences("travelpath_prefs", MODE_PRIVATE)
        val fav = prefs.getStringSet("favorites", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        if (fav.contains(placeName)) fav.remove(placeName) else fav.add(placeName)
        prefs.edit().putStringSet("favorites", fav).apply()
        Toast.makeText(this, if (fav.contains(placeName)) "Ajouté aux favoris" else "Retiré des favoris", Toast.LENGTH_SHORT).show()
    }

    private fun addToSelection(placeName: String) {
        val prefs = getSharedPreferences("travelpath_prefs", MODE_PRIVATE)
        val sel = prefs.getStringSet("to_see", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        if (sel.contains(placeName)) Toast.makeText(this, "Déjà sélectionné", Toast.LENGTH_SHORT).show()
        else {
            sel.add(placeName)
            prefs.edit().putStringSet("to_see", sel).apply()
            Toast.makeText(this, "Ajouté à votre sélection", Toast.LENGTH_SHORT).show()
        }
    }
}