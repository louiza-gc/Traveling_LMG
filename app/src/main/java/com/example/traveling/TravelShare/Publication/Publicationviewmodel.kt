package com.example.traveling.TravelShare.Publication

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PublicationViewModel : ViewModel() {

    // Firebase
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Loading
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Success publication
    private val _publishSuccess = MutableLiveData<Boolean>()
    val publishSuccess: LiveData<Boolean> = _publishSuccess

    // Error
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // AI Tags
    private val _aiTags = MutableLiveData<List<String>>()
    val aiTags: LiveData<List<String>> = _aiTags

    /**
     * Publication d’un post
     * L’image est déjà stockée localement dans le téléphone
     * On enregistre seulement son chemin dans Firestore
     */
    fun publishPost(
        imagePath: String,
        title: String,
        description: String,
        locationName: String,
        isPublic: Boolean,
        tags: List<String>,
        groupId: String = ""
    ) {
        val user = auth.currentUser

        if (user == null) {
            _error.value = "Utilisateur non connecté"
            return
        }

        _isLoading.value = true
        _error.value = null

        // Récupération infos utilisateur
        db.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { userDoc ->

                val pseudo =
                    userDoc.getString("pseudo")
                        ?: user.email
                        ?: "Anonyme"

                val avatarPath =
                    userDoc.getString("avatarPath")
                        ?: ""

                // Données publication
                val postData = hashMapOf(
                    "authorId" to user.uid,
                    "authorName" to pseudo,
                    "authorPhotoUrl" to avatarPath,

                    // IMPORTANT :
                    // chemin local de l’image
                    "photoPath" to imagePath,

                    "title" to title,
                    "caption" to description,
                    "locationName" to locationName,

                    // si pas encore de géolocalisation réelle
                    "locationLat" to 0.0,
                    "locationLng" to 0.0,

                    "isPublic" to isPublic,
                    "groupId" to groupId,

                    "tags" to tags.map { it.lowercase() },

                    "likesCount" to 0,
                    "commentsCount" to 0,
                    "sharesCount" to 0,

                    "timestamp" to System.currentTimeMillis()
                )

                // Sauvegarde Firestore
                db.collection("photos")
                    .add(postData)
                    .addOnSuccessListener {
                        _publishSuccess.value = true
                        _isLoading.value = false
                    }
                    .addOnFailureListener { e ->
                        _error.value =
                            "Erreur publication : ${e.message}"
                        _isLoading.value = false
                    }
            }
            .addOnFailureListener { e ->
                _error.value =
                    "Erreur récupération utilisateur : ${e.message}"
                _isLoading.value = false
            }
    }

    /**
     * Génération automatique de tags IA
     * pas encore finalisée
     */
    fun generateAITags(imagePath: String) {

        _isLoading.value = true

        try {
            // Simulation simple
            val fakeTags = listOf(
                "voyage",
                "nature",
                "aventure",
                "photo",
                "découverte"
            )

            _aiTags.value = fakeTags

        } catch (e: Exception) {
            _error.value = "Erreur IA : ${e.message}"
        }

        _isLoading.value = false
    }
}