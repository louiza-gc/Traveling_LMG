package com.example.traveling.TravelShare.Acceuil

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.traveling.R
import com.example.traveling.TravelShare.Acceuil.photo_post
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
                // Envoie juste l'ID de la publication
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

                    // Ignorer les publications non publiques
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

        // Désactiver ou cacher le bouton micro si pas utilisé
        btnMic.visibility = View.GONE
    }


    private fun searchPublications(query: String) {
        if (query.isEmpty()) {
            feedAdapter.updateData(allPublications)
            return
        }

        val searchTerm = query.lowercase().trim()
        val filtered = allPublications.filter { publication ->
            publication.title.lowercase().contains(searchTerm) ||           // Titre
                    publication.authorName.lowercase().contains(searchTerm) ||     // Auteur
                    publication.location.lowercase().contains(searchTerm) ||       // Lieu
                    publication.description.lowercase().contains(searchTerm) ||    // Description
                    publication.tags.any { tag ->                                  // Tags (Types)
                        tag.lowercase().contains(searchTerm)
                    }
        }

        feedAdapter.updateData(filtered)

        if (filtered.isEmpty()) {
            Toast.makeText(requireContext(), "Aucun résultat pour \"$query\"", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupMenu() {
        btnMenu.setOnClickListener {
            val intent = Intent(requireContext(), page_filters::class.java)
            // Passer les filtres actuels si besoin
            startActivityForResult(intent, REQUEST_FILTERS)
        }
    }

    // Pour recevoir les résultats des filtres
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_FILTERS && resultCode == Activity.RESULT_OK) {
            val filters = data?.getSerializableExtra("filters") as? FilterOptions
            applyFilters(filters)
        }
    }


    private fun applyFilters(filters: FilterOptions?) {
        if (filters == null) {
            feedAdapter.updateData(allPublications)
            return
        }

        var filtered = allPublications

        // Filtre par auteur (insensible à la casse)
        if (filters.author.isNotEmpty()) {
            filtered = filtered.filter {
                it.authorName.equals(filters.author, ignoreCase = true)
            }
        }

        // Filtre par lieu (insensible à la casse)
        if (filters.location.isNotEmpty()) {
            filtered = filtered.filter {
                it.location.lowercase().contains(filters.location.lowercase())
            }
        }

        // Filtre par types de lieu (tags - insensible à la casse)
        if (filters.placeTypes.isNotEmpty()) {
            filtered = filtered.filter { publication ->
                filters.placeTypes.any { filterTag ->
                    publication.tags.any { tag ->
                        tag.equals(filterTag, ignoreCase = true)  // ✅ Ignore maj/min
                    }
                }
            }
        }

        // Filtre par plage de dates
        if (filters.startDate > 0) {
            filtered = filtered.filter { it.timestamp >= filters.startDate }
        }
        if (filters.endDate > 0) {
            filtered = filtered.filter { it.timestamp <= filters.endDate }
        }

        // Filtre par rayon (à implémenter avec géolocalisation)
        if (filters.radius.isNotEmpty()) {
            // TODO: Implémenter le filtre par rayon avec géolocalisation
            Toast.makeText(requireContext(), "Filtre rayon à venir", Toast.LENGTH_SHORT).show()
        }

        feedAdapter.updateData(filtered)

        if (filtered.isEmpty()) {
            Toast.makeText(requireContext(), "Aucun résultat avec ces filtres", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "${filtered.size} résultat(s) trouvé(s)", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val REQUEST_FILTERS = 1001
    }
}