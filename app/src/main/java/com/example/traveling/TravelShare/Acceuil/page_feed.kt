package com.example.traveling.TravelShare.Acceuil

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.traveling.R
import com.example.traveling.TravelShare.feed.FeedAdapter
import com.example.traveling.TravelShare.feed.PublicationItem
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale
import android.util.Log

class page_feed : Fragment(R.layout.fragment_page_feed) {

    private lateinit var rvFeed: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var feedAdapter: FeedAdapter
    private lateinit var etSearch: EditText
    private lateinit var btnMic: ImageButton
    private lateinit var btnMenu: ImageButton

    private val firestore = FirebaseFirestore.getInstance()
    private var allPublications = listOf<PublicationItem>()

    companion object {
        private const val REQUEST_FILTERS = 1001
        private const val REQUEST_VOICE_SEARCH = 1002
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvFeed = view.findViewById(R.id.rvFeed)
        progressBar = view.findViewById(R.id.progressBar)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        etSearch = view.findViewById(R.id.etSearch)
        btnMic = view.findViewById(R.id.btnMic)
        btnMenu = view.findViewById(R.id.btnMenu)

        setupRecyclerView()
        setupSearch()
        setupVoiceSearch()
        setupMenu()
        loadPublications()

        swipeRefresh.setOnRefreshListener {
            loadPublications()
        }
    }

    private fun setupRecyclerView() {
        feedAdapter = FeedAdapter(
            publications = emptyList(),
            onLikeClick = { item, position ->
                Toast.makeText(requireContext(), "Like: ${item.authorName}", Toast.LENGTH_SHORT).show()
            },
            onItemClick = { item ->
                //  Envoie l'ID ET les tags de la publication
                val intent = Intent(requireContext(), photo_post::class.java).apply {
                    putExtra("post_id", item.id)
                    putExtra("tags", item.tags.joinToString(","))  // ← AJOUTE CETTE LIGNE
                }
                startActivity(intent)
            }
        )

        rvFeed.layoutManager = LinearLayoutManager(requireContext())
        rvFeed.adapter = feedAdapter
    }

    private fun loadPublications() {
        progressBar.visibility = View.VISIBLE

        firestore.collection("photos")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val publications = mutableListOf<PublicationItem>()

                for (doc in result) {
                    val data = doc.data
                    val isPublic = data["isPublic"] as? Boolean ?: true

                    if (!isPublic) continue

                    val publication = PublicationItem(
                        id = doc.id,
                        authorName = data["authorName"] as? String ?: "Anonyme",
                        authorAvatar = data["authorPhotoUrl"] as? String ?: "",
                        location = data["locationName"] as? String ?: "",
                        imageUrl = data["photoPath"] as? String ?: "",
                        likesCount = (data["likesCount"] as? Long)?.toInt() ?: 0,
                        commentsCount = (data["commentsCount"] as? Long)?.toInt() ?: 0,
                        sharesCount = (data["sharesCount"] as? Long)?.toInt() ?: 0,
                        isLiked = false,
                        title = data["title"] as? String ?: "",
                        description = data["caption"] as? String ?: "",
                        timestamp = (data["timestamp"] as? Long) ?: System.currentTimeMillis(),
                        tags = (data["tags"] as? List<*>)?.map { it.toString() } ?: emptyList(),
                        latitude = (data["locationLat"] as? Double) ?: 0.0,
                        longitude = (data["locationLng"] as? Double) ?: 0.0
                    )
                    publications.add(publication)
                }

                allPublications = publications
                feedAdapter.updateData(publications)
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false

                if (publications.isEmpty()) {
                    Toast.makeText(requireContext(), "Aucune publication publique", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
                Toast.makeText(requireContext(), "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchPublications(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun setupVoiceSearch() {
        btnMic.visibility = View.VISIBLE
        btnMic.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(),
                    arrayOf(android.Manifest.permission.RECORD_AUDIO), 200)
                return@setOnClickListener
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "🔎 Parlez votre recherche...")
            startActivityForResult(intent, REQUEST_VOICE_SEARCH)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 200 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            btnMic.performClick()
        } else if (requestCode == 200) {
            Toast.makeText(requireContext(), "Permission micro requise pour la recherche vocale", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_VOICE_SEARCH && resultCode == Activity.RESULT_OK) {
            val result = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = result?.getOrNull(0) ?: ""
            if (spokenText.isNotEmpty()) {
                etSearch.setText(spokenText)
                searchPublications(spokenText)
                Toast.makeText(requireContext(), "🔍 Recherche : $spokenText", Toast.LENGTH_SHORT).show()
            }
        }

        if (requestCode == REQUEST_FILTERS && resultCode == Activity.RESULT_OK) {
            val filters = data?.getSerializableExtra("filters") as? FilterOptions
            applyFilters(filters)
        }
    }

    private fun searchPublications(query: String) {
        if (query.isEmpty()) {
            feedAdapter.updateData(allPublications)
            return
        }

        val searchTerm = query.lowercase().trim()
        val filtered = allPublications.filter { publication ->
            publication.title.lowercase().contains(searchTerm) ||
                    publication.authorName.lowercase().contains(searchTerm) ||
                    publication.location.lowercase().contains(searchTerm) ||
                    publication.description.lowercase().contains(searchTerm) ||
                    publication.tags.any { tag -> tag.lowercase().contains(searchTerm) }
        }

        feedAdapter.updateData(filtered)

        if (filtered.isEmpty()) {
            Toast.makeText(requireContext(), "Aucun résultat pour \"$query\"", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupMenu() {
        btnMenu.setOnClickListener {
            val intent = Intent(requireContext(), page_filters::class.java)
            startActivityForResult(intent, REQUEST_FILTERS)
        }
    }

    private fun applyFilters(filters: FilterOptions?) {
        if (filters == null) {
            feedAdapter.updateData(allPublications)
            return
        }

        Log.d("Filters", "=== APPLY FILTERS ===")
        Log.d("Filters", "Lieu: '${filters.location}'")
        Log.d("Filters", "Rayon: '${filters.radius}'")
        Log.d("Filters", "Auteur: '${filters.author}'")
        Log.d("Filters", "Types: ${filters.placeTypes}")

        var filtered = allPublications

        // Filtre par auteur
        if (filters.author.isNotEmpty()) {
            filtered = filtered.filter {
                it.authorName.equals(filters.author, ignoreCase = true)
            }
        }

        // Filtre par lieu (texte normal, PAS rayon)
        if (filters.location.isNotEmpty() && filters.radius.isEmpty()) {
            filtered = filtered.filter {
                it.location.lowercase().contains(filters.location.lowercase())
            }
        }

        // Filtre par tags
        if (filters.placeTypes.isNotEmpty()) {
            filtered = filtered.filter { publication ->
                filters.placeTypes.any { filterTag ->
                    publication.tags.any { tag ->
                        tag.equals(filterTag, ignoreCase = true)
                    }
                }
            }
        }

        // Filtre par dates
        if (filters.startDate > 0) {
            filtered = filtered.filter { it.timestamp >= filters.startDate }
        }
        if (filters.endDate > 0) {
            filtered = filtered.filter { it.timestamp <= filters.endDate }
        }

        //  FILTRE PAR RAYON - SEULEMENT si LIEU est REMPLI
        if (filters.radius.isNotEmpty() && filters.location.isNotEmpty()) {
            val radiusValue = filters.radius.replace(" km", "").toIntOrNull()
            if (radiusValue != null && radiusValue > 0) {
                filterByRadius(filters.location, radiusValue) { radiusFiltered ->
                    val finalFiltered = filtered.filter { pub ->
                        radiusFiltered.any { it.id == pub.id }
                    }
                    feedAdapter.updateData(finalFiltered)
                    showFilterResultMessage(finalFiltered)
                }
                return
            }
        }

        // Si pas de filtre rayon, mettre à jour directement
        feedAdapter.updateData(filtered)
        showFilterResultMessage(filtered)
    }

    // FONCTION FILTRE PAR RAYON
    private fun filterByRadius(locationName: String, radiusKm: Int, callback: (List<PublicationItem>) -> Unit) {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())

        try {
            val addresses = geocoder.getFromLocationName(locationName, 1)

            if (addresses.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "📍 Lieu '$locationName' non trouvé", Toast.LENGTH_SHORT).show()
                callback(emptyList())
                return
            }

            val centerLat = addresses[0].latitude
            val centerLng = addresses[0].longitude

            // Filtrer les publications qui ont des coordonnées valides
            val filtered = allPublications.filter { publication ->
                // Vérifier que les coordonnées existent et sont valides
                if (publication.latitude == 0.0 && publication.longitude == 0.0) {
                    return@filter false
                }
                val distance = calculateDistance(centerLat, centerLng, publication.latitude, publication.longitude)
                distance <= radiusKm
            }

            if (filtered.isEmpty()) {
                Toast.makeText(requireContext(), "📍 Aucune publication dans un rayon de $radiusKm km autour de $locationName", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(requireContext(), "📍 ${filtered.size} publication(s) dans un rayon de $radiusKm km", Toast.LENGTH_LONG).show()
            }

            callback(filtered)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Erreur de géolocalisation", Toast.LENGTH_SHORT).show()
            callback(emptyList())
        }
    }

    // CALCUL DE DISTANCE (formule de Haversine)
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371 // Rayon de la Terre en km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
    }

    private fun showFilterResultMessage(filtered: List<PublicationItem>) {
        if (filtered.isEmpty()) {
            Toast.makeText(requireContext(), "Aucun résultat avec ces filtres", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "${filtered.size} résultat(s) trouvé(s)", Toast.LENGTH_SHORT).show()
        }
    }
}