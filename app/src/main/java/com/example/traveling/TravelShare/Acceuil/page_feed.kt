package com.example.traveling.TravelShare.Acceuil

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
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
                val intent = Intent(requireContext(), photo_post::class.java).apply {
                    putExtra("post_id", item.id)
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
                        tags = (data["tags"] as? List<*>)?.map { it.toString() } ?: emptyList()
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
            // Vérifier la permission microphone
            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(),
                    arrayOf(android.Manifest.permission.RECORD_AUDIO), 200)
                return@setOnClickListener
            }

            // Lancer la reconnaissance vocale
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "🔎 Parlez votre recherche...")
            startActivityForResult(intent, REQUEST_VOICE_SEARCH)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 200 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission accordée, relancer la recherche vocale
            btnMic.performClick()
        } else if (requestCode == 200) {
            Toast.makeText(requireContext(), "Permission micro requise pour la recherche vocale", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Résultat de la recherche vocale
        if (requestCode == REQUEST_VOICE_SEARCH && resultCode == Activity.RESULT_OK) {
            val result = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = result?.getOrNull(0) ?: ""
            if (spokenText.isNotEmpty()) {
                etSearch.setText(spokenText)
                searchPublications(spokenText)
                Toast.makeText(requireContext(), "🔍 Recherche : $spokenText", Toast.LENGTH_SHORT).show()
            }
        }

        // Résultat des filtres
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

        var filtered = allPublications

        if (filters.author.isNotEmpty()) {
            filtered = filtered.filter {
                it.authorName.equals(filters.author, ignoreCase = true)
            }
        }

        if (filters.location.isNotEmpty()) {
            filtered = filtered.filter {
                it.location.lowercase().contains(filters.location.lowercase())
            }
        }

        if (filters.placeTypes.isNotEmpty()) {
            filtered = filtered.filter { publication ->
                filters.placeTypes.any { filterTag ->
                    publication.tags.any { tag ->
                        tag.equals(filterTag, ignoreCase = true)
                    }
                }
            }
        }

        if (filters.startDate > 0) {
            filtered = filtered.filter { it.timestamp >= filters.startDate }
        }
        if (filters.endDate > 0) {
            filtered = filtered.filter { it.timestamp <= filters.endDate }
        }

        if (filters.radius.isNotEmpty()) {
            Toast.makeText(requireContext(), "Filtre rayon à venir", Toast.LENGTH_SHORT).show()
        }

        feedAdapter.updateData(filtered)

        if (filtered.isEmpty()) {
            Toast.makeText(requireContext(), "Aucun résultat avec ces filtres", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "${filtered.size} résultat(s) trouvé(s)", Toast.LENGTH_SHORT).show()
        }
    }
}