package com.example.traveling.TravelShare.Acceuil

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.traveling.R
import com.example.traveling.TravelShare.feed.CommentAdapter
import com.example.traveling.TravelShare.feed.CommentItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class photo_post : AppCompatActivity() {

    private lateinit var ivAvatar: CircleImageView
    private lateinit var tvAuthorName: TextView
    private lateinit var tvLocationDate: TextView
    private lateinit var ivPhoto: ImageView
    private lateinit var btnLike: ImageButton
    private lateinit var tvLikeCount: TextView
    private lateinit var tvCommentCount: TextView
    private lateinit var tvShareCount: TextView
    private lateinit var tvPostAuthor: TextView
    private lateinit var tvPostDescription: TextView
    private lateinit var tvLikedBy: TextView
    private lateinit var btnComment: ImageButton
    private lateinit var btnShare: ImageButton
    private lateinit var btnMore: ImageButton
    private lateinit var btnReport: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var btnGenerateRoute: com.google.android.material.button.MaterialButton
    private lateinit var btnOpenRoute: com.google.android.material.button.MaterialButton
    private lateinit var btnSimilarPhotos: com.google.android.material.button.MaterialButton

    // Commentaires
    private lateinit var rvComments: RecyclerView
    private lateinit var etNewComment: EditText
    private lateinit var btnSendComment: ImageButton
    private lateinit var commentAdapter: CommentAdapter
    private val commentsList = mutableListOf<CommentItem>()

    private var postId: String = ""
    private var authorName: String = ""
    private var authorAvatar: String = ""
    private var imageUrl: String = ""
    private var location: String = ""
    private var description: String = ""
    private var likesCount: Int = 0
    private var commentsCount: Int = 0
    private var sharesCount: Int = 0
    private var timestamp: Long = 0
    private lateinit var tvReportCount: TextView
    private var reportsCount: Int = 0

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var isLiked = false
    private var isDataLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_post)

        initViews()

        // Récupérer juste l'ID
        postId = intent.getStringExtra("post_id") ?: ""

        if (postId.isNotEmpty()) {
            setupCommentsRecyclerView()
            loadPostFromFirestore()
        } else {
            Toast.makeText(this, "Erreur: publication introuvable", Toast.LENGTH_SHORT).show()
            finish()
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setupKeyboardListener()
        setupListeners()
    }

    private fun setupCommentsRecyclerView() {
        commentAdapter = CommentAdapter(
            comments = emptyList(),
            postId = postId,
            onCommentDeleted = {
                loadComments()
            }
        )
        rvComments.layoutManager = LinearLayoutManager(this)
        rvComments.adapter = commentAdapter
        rvComments.isNestedScrollingEnabled = false
    }

    private fun loadComments() {
        firestore.collection("photos")
            .document(postId)
            .collection("comments")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                commentsList.clear()
                snapshot?.documents?.forEach { doc ->
                    val data = doc.data
                    if (data != null) {
                        val comment = CommentItem(
                            id = doc.id,
                            userId = data["userId"] as? String ?: "",
                            authorName = data["authorName"] as? String ?: "Anonyme",
                            authorAvatar = data["authorAvatar"] as? String ?: "",
                            text = data["text"] as? String ?: "",
                            timestamp = data["timestamp"] as? Long ?: System.currentTimeMillis()
                        )
                        commentsList.add(comment)
                    }
                }
                commentAdapter.updateComments(commentsList)

                if (commentsList.size != commentsCount) {
                    commentsCount = commentsList.size
                    tvCommentCount.text = formatCount(commentsCount)
                }
            }
    }

    private fun sendComment() {
        val commentText = etNewComment.text.toString().trim()
        if (commentText.isEmpty()) {
            Toast.makeText(this, "Veuillez écrire un commentaire", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "Vous devez être connecté", Toast.LENGTH_SHORT).show()
            return
        }

        firestore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { userDoc ->
                val authorName = userDoc.getString("pseudo") ?: userDoc.getString("fullName") ?: "Anonyme"
                val authorAvatar = userDoc.getString("avatarPath") ?: ""

                val commentData = hashMapOf(
                    "userId" to userId,
                    "authorName" to authorName,
                    "authorAvatar" to authorAvatar,
                    "text" to commentText,
                    "timestamp" to System.currentTimeMillis()
                )

                firestore.collection("photos")
                    .document(postId)
                    .collection("comments")
                    .add(commentData)
                    .addOnSuccessListener {
                        firestore.collection("photos")
                            .document(postId)
                            .update("commentsCount", FieldValue.increment(1))

                        etNewComment.text.clear()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erreur récupération utilisateur: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadPostFromFirestore() {
        progressBar.visibility = android.view.View.VISIBLE

        firestore.collection("photos")
            .document(postId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val data = document.data

                    authorName = data?.get("authorName") as? String ?: "Anonyme"
                    authorAvatar = data?.get("authorPhotoUrl") as? String ?: ""
                    imageUrl = data?.get("photoPath") as? String ?: ""
                    location = data?.get("locationName") as? String ?: ""
                    description = data?.get("caption") as? String ?: ""
                    likesCount = (data?.get("likesCount") as? Long)?.toInt() ?: 0
                    commentsCount = (data?.get("commentsCount") as? Long)?.toInt() ?: 0
                    sharesCount = (data?.get("sharesCount") as? Long)?.toInt() ?: 0
                    reportsCount = (data?.get("reportsCount") as? Long)?.toInt() ?: 0
                    timestamp = (data?.get("timestamp") as? Long) ?: System.currentTimeMillis()

                    isDataLoaded = true
                    displayData()
                    checkIfLiked()
                    loadComments()

                    progressBar.visibility = android.view.View.GONE
                } else {
                    Toast.makeText(this, "Publication non trouvée", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = android.view.View.GONE
                Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun initViews() {
        ivAvatar = findViewById(R.id.ivAvatar)
        tvAuthorName = findViewById(R.id.tvAuthorName)
        tvLocationDate = findViewById(R.id.tvLocationDate)
        ivPhoto = findViewById(R.id.ivPhoto)
        btnLike = findViewById(R.id.btnLike)
        tvLikeCount = findViewById(R.id.tvLikeCount)
        tvCommentCount = findViewById(R.id.tvCommentCount)
        tvShareCount = findViewById(R.id.tvShareCount)
        tvPostAuthor = findViewById(R.id.tvPostAuthor)
        tvPostDescription = findViewById(R.id.tvPostDescription)
        tvLikedBy = findViewById(R.id.tvLikedBy)
        btnComment = findViewById(R.id.btnComment)
        btnShare = findViewById(R.id.btnShare)
        btnMore = findViewById(R.id.btnMore)
        tvReportCount = findViewById(R.id.tvReportCount)
        btnReport = findViewById(R.id.btnReport)
        btnGenerateRoute = findViewById(R.id.btnGenerateRoute)
        btnOpenRoute = findViewById(R.id.btnOpenRoute)
        btnSimilarPhotos = findViewById(R.id.btnSimilarPhotos)
        progressBar = findViewById(R.id.progressBar)

        // Commentaires
        rvComments = findViewById(R.id.rvComments)
        etNewComment = findViewById(R.id.etNewComment)
        btnSendComment = findViewById(R.id.btnSendComment)

        // Désactiver les boutons d'itinéraire par défaut
        btnGenerateRoute.isEnabled = false
        btnOpenRoute.isEnabled = false
        btnGenerateRoute.alpha = 0.5f
        btnOpenRoute.alpha = 0.5f
    }

    private fun displayData() {
        if (!isDataLoaded) return

        // Avatar
        if (authorAvatar.isNotEmpty()) {
            Glide.with(this)
                .load(authorAvatar)
                .placeholder(R.drawable.ic_default_avatar)
                .into(ivAvatar)
        }

        tvAuthorName.text = authorName
        tvPostAuthor.text = authorName

        // Localisation + Date
        val locationText = if (location.isNotEmpty() && location != "Ajouter un lieu") {
            "📍 $location"
        } else {
            ""
        }
        val dateText = getTimeAgo(timestamp)

        tvLocationDate.text = when {
            locationText.isNotEmpty() && dateText.isNotEmpty() -> "$locationText • $dateText"
            locationText.isNotEmpty() -> locationText
            else -> dateText
        }

        // Activer les boutons d'itinéraire si lieu présent
        val hasValidLocation = location.isNotEmpty() && location != "Ajouter un lieu"
        btnGenerateRoute.isEnabled = hasValidLocation
        btnOpenRoute.isEnabled = hasValidLocation
        btnGenerateRoute.alpha = if (hasValidLocation) 1f else 0.5f
        btnOpenRoute.alpha = if (hasValidLocation) 1f else 0.5f

        // Description
        if (description.isNotEmpty()) {
            tvPostDescription.text = description
        } else {
            tvPostDescription.visibility = android.view.View.GONE
        }

        // Image
        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.drawable.placeholder_photo)
            .into(ivPhoto)

        // Compteurs
        tvLikeCount.text = formatCount(likesCount)
        tvCommentCount.text = formatCount(commentsCount)
        tvShareCount.text = formatCount(sharesCount)
        tvReportCount.text = formatCount(reportsCount)

        updateLikedByText(likesCount)
    }

    private fun setupListeners() {
        btnLike.setOnClickListener {
            toggleLike()
        }

        btnComment.setOnClickListener {
            rvComments.smoothScrollToPosition(commentsList.size)
        }

        btnShare.setOnClickListener {
            sharePost()
        }

        btnMore.setOnClickListener {
            Toast.makeText(this, "Options", Toast.LENGTH_SHORT).show()
        }

        btnReport.setOnClickListener {
            showReportDialog()
        }

        btnGenerateRoute.setOnClickListener {
            openMapsForLocation()
        }

        btnOpenRoute.setOnClickListener {
            openMapsForLocation()
        }

        btnSimilarPhotos.setOnClickListener {
            Toast.makeText(this, "Photos similaires", Toast.LENGTH_SHORT).show()
        }

        btnSendComment.setOnClickListener {
            sendComment()
        }
    }

    private fun openMapsForLocation() {
        if (location.isEmpty() || location == "Ajouter un lieu") {
            Toast.makeText(this, "📍 Aucun lieu associé à cette publication", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val encodedLocation = Uri.encode(location)
            val uri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$encodedLocation")

            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.google.android.apps.maps")
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Impossible d'ouvrir Maps", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkIfLiked() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("photos")
            .document(postId)
            .collection("likes")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                isLiked = document.exists()
                updateLikeIcon()
            }
    }

    private fun toggleLike() {
        val userId = auth.currentUser?.uid ?: return

        if (isLiked) {
            firestore.collection("photos")
                .document(postId)
                .collection("likes")
                .document(userId)
                .delete()
                .addOnSuccessListener {
                    firestore.collection("photos")
                        .document(postId)
                        .update("likesCount", FieldValue.increment(-1))
                    isLiked = false
                    likesCount--
                    tvLikeCount.text = formatCount(likesCount)
                    updateLikeIcon()
                    updateLikedByText(likesCount)
                }
        } else {
            val likeData = hashMapOf(
                "userId" to userId,
                "timestamp" to System.currentTimeMillis()
            )

            firestore.collection("photos")
                .document(postId)
                .collection("likes")
                .document(userId)
                .set(likeData)
                .addOnSuccessListener {
                    firestore.collection("photos")
                        .document(postId)
                        .update("likesCount", FieldValue.increment(1))
                    isLiked = true
                    likesCount++
                    tvLikeCount.text = formatCount(likesCount)
                    updateLikeIcon()
                    updateLikedByText(likesCount)
                }
        }
    }

    private fun updateLikeIcon() {
        if (isLiked) {
            btnLike.setImageResource(R.drawable.ic_like)
        } else {
            btnLike.setImageResource(R.drawable.ic_dislike)
        }
    }

    private fun updateLikedByText(count: Int) {
        if (count > 0) {
            tvLikedBy.text = "Aimé par $count personne${if (count > 1) "s" else ""}"
        } else {
            tvLikedBy.text = "Soyez le premier à aimer"
        }
    }

    private fun sharePost() {
        val shareIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            putExtra(android.content.Intent.EXTRA_TEXT, "Regarde cette publication sur TravelShare !\n\n$imageUrl")
            type = "text/plain"
        }
        startActivity(android.content.Intent.createChooser(shareIntent, "Partager via"))
    }

    private fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60000 -> "À l'instant"
            diff < 3600000 -> "Il y a ${diff / 60000} min"
            diff < 86400000 -> "Il y a ${diff / 3600000} h"
            diff < 604800000 -> "Il y a ${diff / 86400000} j"
            else -> {
                val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                date.format(Date(timestamp))
            }
        }
    }

    private fun formatCount(count: Int): String {
        return when {
            count >= 1000 -> "${count / 1000}k"
            else -> count.toString()
        }
    }

    private fun setupKeyboardListener() {
        val rootView = window.decorView.findViewById<View>(android.R.id.content)

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val cardHeight = findViewById<androidx.cardview.widget.CardView>(R.id.cardAddComment).height

            val cardComment = findViewById<androidx.cardview.widget.CardView>(R.id.cardAddComment)
            val params = cardComment.layoutParams as ConstraintLayout.LayoutParams
            params.bottomMargin = if (imeHeight > 0) imeHeight - navBarHeight + 16 else 16
            cardComment.layoutParams = params

            val scrollView = findViewById<ScrollView>(R.id.scrollView)
            val extraPad = if (imeHeight > 0) imeHeight - navBarHeight + cardHeight + 32 else cardHeight + 32
            scrollView.setPadding(0, 0, 0, extraPad)

            if (imeHeight > 0) {
                scrollView.postDelayed({
                    val scrollY = scrollView.getChildAt(0).height - scrollView.height
                    scrollView.scrollTo(0, scrollY)
                }, 150)
            }

            insets
        }
    }

    private fun showReportDialog() {
        val dialog = android.app.AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.report_dialog, null)
        dialog.setView(view)

        val radioGroup = view.findViewById<RadioGroup>(R.id.radioGroupReport)
        val btnCancel = view.findViewById<Button>(R.id.btnCancelReport)
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmitReport)

        val alertDialog = dialog.create()

        btnCancel.setOnClickListener {
            alertDialog.dismiss()
        }

        btnSubmit.setOnClickListener {
            val selectedId = radioGroup.checkedRadioButtonId
            val reason = when (selectedId) {
                R.id.radioSpam -> "Spam / contenu trompeur"
                R.id.radioInappropriate -> "Contenu inapproprié"
                R.id.radioHarassment -> "Harcèlement / discours haineux"
                else -> {
                    Toast.makeText(this, "Veuillez sélectionner une raison", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            submitReport(reason)
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    private fun submitReport(reason: String) {
        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "Vous devez être connecté", Toast.LENGTH_SHORT).show()
            return
        }

        firestore.collection("photos")
            .document(postId)
            .collection("reports")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    Toast.makeText(this, "Vous avez déjà signalé cette publication", Toast.LENGTH_SHORT).show()
                } else {
                    val reportData = hashMapOf(
                        "userId" to userId,
                        "reason" to reason,
                        "timestamp" to System.currentTimeMillis()
                    )

                    firestore.collection("photos")
                        .document(postId)
                        .collection("reports")
                        .document(userId)
                        .set(reportData)
                        .addOnSuccessListener {
                            firestore.collection("photos")
                                .document(postId)
                                .update("reportsCount", FieldValue.increment(1))

                            Toast.makeText(this, "Merci, signalement envoyé", Toast.LENGTH_SHORT).show()
                        }
                }
            }
    }
}