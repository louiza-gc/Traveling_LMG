package com.example.traveling.TravelPath.Accueil

import android.annotation.SuppressLint
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
import com.google.gson.Gson
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
            val intent = Intent(this, PreferencesActivity::class.java).apply {
                putExtra("CATEGORY_NAME", currentCategoryName)
                putExtra("CITY", currentCity)
            }
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
        val items = places.map { place ->
            Item(
                name = place.name,
                assetPath = "",
                imageUrl = place.media.thumbnail,
                city = place.location.city,
                country = place.location.country,
                isFromApi = false
            )
        }
        adapter = ItemAdapter(items) { clickedItem ->
            val place = places.find { it.name == clickedItem.name }
            place?.let { showPlaceDialog(it) }
        }
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

    // Popup affichant les détails du lieu
    @SuppressLint("MissingInflatedId")
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