package com.example.traveling.TravelShare.Profil

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.example.traveling.R
import com.example.traveling.TravelPath.Accueil.AccueilPath
import com.example.traveling.TravelShare.Acceuil.photo_post
import com.example.traveling.TravelShare.Connection.login
import com.example.traveling.TravelShare.feed.PublicationItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView
import java.io.File

class account_profile : Fragment(R.layout.fragment_account_profile) {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var ivAvatar: CircleImageView
    private lateinit var tvPseudo: TextView
    private lateinit var tvFullName: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvPostCount: TextView
    private lateinit var tvGroupCount: TextView
    private lateinit var tvLikesCount: TextView
    private lateinit var rvPhotos: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var llEmptyState: View
    private lateinit var tvPublierLink: TextView

    private lateinit var photoAdapter: ProfilePhotoAdapter
    private val userPhotos = mutableListOf<PublicationItem>()
    private var currentUserId: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        ivAvatar = view.findViewById(R.id.ivAvatar)
        tvPseudo = view.findViewById(R.id.tvPseudo)
        tvFullName = view.findViewById(R.id.tvFullName)
        progressBar = view.findViewById(R.id.progressBar)
        tvPostCount = view.findViewById(R.id.tvPostCount)
        tvGroupCount = view.findViewById(R.id.tvGroupCount)
        tvLikesCount = view.findViewById(R.id.tvLikesCount)
        rvPhotos = view.findViewById(R.id.rvPhotos)
        swipeRefresh = view.findViewById(R.id.swipeRefreshProfile)
        llEmptyState = view.findViewById(R.id.llEmptyState)
        tvPublierLink = view.findViewById(R.id.tvPublierLink)
        val btnMenu = view.findViewById<ImageButton>(R.id.btnMenu)

        // Setup RecyclerView
        rvPhotos.layoutManager = GridLayoutManager(requireContext(), 3)
        photoAdapter = ProfilePhotoAdapter(userPhotos,
            onPhotoClick = { publication ->
                val intent = Intent(requireContext(), photo_post::class.java)
                intent.putExtra("post_id", publication.id)
                startActivity(intent)
            },
            onPhotoDelete = { publication, position ->
                deletePhoto(publication, position)
            }
        )
        rvPhotos.adapter = photoAdapter

        // Swipe to refresh
        swipeRefresh.setOnRefreshListener {
            loadUserData()
            loadUserPhotos()
        }

        // Lien pour publier
        tvPublierLink.setOnClickListener {
            // Naviguer vers l'écran de publication
            val intent = Intent(requireContext(), com.example.traveling.TravelShare.Publication.publication_add::class.java)
            startActivity(intent)
        }

        btnMenu.setOnClickListener { v ->
            val popup = PopupMenu(requireContext(), v)
            popup.menuInflater.inflate(R.menu.menu_profil, popup.menu)

            try {
                val field = popup.javaClass.getDeclaredField("mPopup")
                field.isAccessible = true
                val menuPopupHelper = field.get(popup)
                val classPopupHelper = Class.forName(menuPopupHelper.javaClass.name)
                val setForceIcons = classPopupHelper.getMethod("setForceShowIcon", Boolean::class.java)
                setForceIcons.invoke(menuPopupHelper, true)
                val setBackground = classPopupHelper.getMethod(
                    "setBackgroundDrawable",
                    android.graphics.drawable.Drawable::class.java
                )
                setBackground.invoke(
                    menuPopupHelper,
                    android.graphics.drawable.ColorDrawable(android.graphics.Color.WHITE)
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_switch_travelpath -> {
                        val intent = Intent(requireContext(), AccueilPath::class.java)
                        startActivity(intent)
                        true
                    }
                    R.id.menu_logout -> {
                        FirebaseAuth.getInstance().signOut()
                        val intent = Intent(requireContext(), login::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        val user = auth.currentUser
        if (user == null) {
            progressBar.visibility = View.GONE
            return
        }

        currentUserId = user.uid
        loadUserData()
        loadUserPhotos()
    }

    private fun loadUserData() {
        progressBar.visibility = View.VISIBLE

        db.collection("users").document(currentUserId)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    progressBar.visibility = View.GONE
                    return@addOnSuccessListener
                }

                val pseudo = doc.getString("pseudo") ?: ""
                val nom = doc.getString("nom") ?: ""
                val prenom = doc.getString("prenom") ?: ""

                tvPseudo.text = pseudo
                tvFullName.text = "$prenom $nom"

                val avatarPath = doc.getString("avatarPath")
                if (!avatarPath.isNullOrEmpty()) {
                    val file = File(avatarPath)
                    if (file.exists()) {
                        ivAvatar.setImageURI(Uri.fromFile(file))
                    } else {
                        ivAvatar.setImageResource(R.drawable.ic_default_avatar)
                    }
                } else {
                    ivAvatar.setImageResource(R.drawable.ic_default_avatar)
                }

                // Nombre de publications
                db.collection("photos")
                    .whereEqualTo("authorId", currentUserId)
                    .get()
                    .addOnSuccessListener { result ->
                        tvPostCount.text = result.size().toString()
                    }

                db.collection("groups")
                    .get()
                    .addOnSuccessListener { groupsResult ->
                        var groupCount = 0
                        val pendingChecks = groupsResult.documents.map { doc ->
                            db.collection("groups")
                                .document(doc.id)
                                .collection("members")
                                .document(currentUserId)
                                .get()
                                .continueWith { task ->
                                    if (task.isSuccessful && task.result.exists()) {
                                        groupCount++
                                    }
                                }
                        }

                        com.google.android.gms.tasks.Tasks.whenAllComplete(pendingChecks)
                            .addOnSuccessListener {
                                tvGroupCount.text = groupCount.toString()
                            }
                    }

                db.collection("photos")
                    .whereEqualTo("authorId", currentUserId)
                    .get()
                    .addOnSuccessListener { photos ->
                        var totalLikes = 0
                        for (photo in photos) {
                            val likesCount = photo.getLong("likesCount") ?: 0
                            totalLikes += likesCount.toInt()
                        }
                        tvLikesCount.text = totalLikes.toString()
                    }

                progressBar.visibility = View.GONE
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Erreur chargement profil", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadUserPhotos() {
        db.collection("photos")
            .whereEqualTo("authorId", currentUserId)
            .get()
            .addOnSuccessListener { result ->
                userPhotos.clear()
                for (doc in result) {
                    val data = doc.data
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
                    userPhotos.add(publication)
                }

                userPhotos.sortByDescending { it.timestamp }

                photoAdapter.updateData(userPhotos)
                swipeRefresh.isRefreshing = false

                if (userPhotos.isEmpty()) {
                    llEmptyState.visibility = View.VISIBLE
                    rvPhotos.visibility = View.GONE
                } else {
                    llEmptyState.visibility = View.GONE
                    rvPhotos.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { e ->
                swipeRefresh.isRefreshing = false
                Toast.makeText(requireContext(), "Erreur chargement photos: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deletePhoto(publication: PublicationItem, position: Int) {
        // Dialog de confirmation
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Supprimer la publication")
            .setMessage("Voulez-vous vraiment supprimer cette publication ?")
            .setPositiveButton("Supprimer") { _, _ ->
                // Supprimer de Firestore
                db.collection("photos")
                    .document(publication.id)
                    .delete()
                    .addOnSuccessListener {
                        // Supprimer aussi l'image locale (optionnel)
                        val imageFile = File(publication.imageUrl)
                        if (imageFile.exists()) {
                            imageFile.delete()
                        }

                        // Mettre à jour la liste
                        userPhotos.removeAt(position)
                        photoAdapter.updateData(userPhotos)

                        // Mettre à jour le compteur
                        tvPostCount.text = userPhotos.size.toString()

                        Toast.makeText(requireContext(), "Publication supprimée", Toast.LENGTH_SHORT).show()

                        // Afficher l'état vide si plus de photos
                        if (userPhotos.isEmpty()) {
                            llEmptyState.visibility = View.VISIBLE
                            rvPhotos.visibility = View.GONE
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }
}