package com.example.traveling.TravelShare.Publication

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PublicationViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // On retourne maintenant le postId au lieu d'un simple Boolean
    private val _publishSuccess = MutableLiveData<String?>()
    val publishSuccess: LiveData<String?> = _publishSuccess

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _aiTags = MutableLiveData<List<String>>()
    val aiTags: LiveData<List<String>> = _aiTags

    fun publishPost(
        imagePath: String,
        title: String,
        description: String,
        locationName: String,
        isPublic: Boolean,
        tags: List<String>,
        groupId: String = ""
    ) {
        val user = auth.currentUser ?: run {
            _error.value = "Utilisateur non connecté"
            return
        }

        _isLoading.value = true
        _error.value     = null

        db.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { userDoc ->
                val pseudo     = userDoc.getString("pseudo") ?: user.email ?: "Anonyme"
                val avatarPath = userDoc.getString("avatarPath") ?: ""

                val postData = hashMapOf(
                    "authorId"       to user.uid,
                    "authorName"     to pseudo,
                    "authorPhotoUrl" to avatarPath,
                    "photoPath"      to imagePath,
                    "title"          to title,
                    "caption"        to description,
                    "locationName"   to locationName,
                    "locationLat"    to 0.0,
                    "locationLng"    to 0.0,
                    "isPublic"       to isPublic,
                    "groupId"        to groupId,
                    "tags"           to tags.map { it.lowercase() },
                    "likesCount"     to 0,
                    "commentsCount"  to 0,
                    "sharesCount"    to 0,
                    "reportsCount"   to 0,
                    "isBlocked"      to false,
                    "timestamp"      to System.currentTimeMillis()
                )

                db.collection("photos")
                    .add(postData)
                    .addOnSuccessListener { docRef ->
                        // On envoie le vrai postId
                        _publishSuccess.value = docRef.id
                        _isLoading.value      = false
                    }
                    .addOnFailureListener { e ->
                        _error.value     = "Erreur publication : ${e.message}"
                        _isLoading.value = false
                    }
            }
            .addOnFailureListener { e ->
                _error.value     = "Erreur récupération utilisateur : ${e.message}"
                _isLoading.value = false
            }
    }

    fun generateAITags(imagePath: String) {
        _isLoading.value = true
        try {
            _aiTags.value = listOf("voyage", "nature", "aventure", "photo", "découverte")
        } catch (e: Exception) {
            _error.value = "Erreur IA : ${e.message}"
        }
        _isLoading.value = false
    }

    // Reset après navigation pour éviter re-trigger
    fun resetPublishSuccess() {
        _publishSuccess.value = null
    }
}