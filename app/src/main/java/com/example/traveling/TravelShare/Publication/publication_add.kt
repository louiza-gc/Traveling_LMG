package com.example.traveling.TravelShare.Publication

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.traveling.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream

class publication_add : Fragment(R.layout.fragment_publication_add) {

    private lateinit var viewModel: PublicationViewModel

    private lateinit var ivPreview: ImageView
    private lateinit var btnSelectImage: MaterialButton
    private lateinit var flMediaPreview: FrameLayout
    private lateinit var etTitle: EditText
    private lateinit var etDescription: EditText
    private lateinit var chipGroupTags: ChipGroup
    private lateinit var tvLocationLabel: TextView
    private lateinit var tvVisibilityLabel: TextView
    private lateinit var btnPublish: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutAddLocation: LinearLayout

    private var selectedLocation: String? = null
    private var selectedMediaUri: Uri? = null
    private var selectedVisibility = "public"
    private var selectedGroupId: String? = null
    private var selectedGroupName: String? = null

    private val firestore = FirebaseFirestore.getInstance()
    private val auth      = FirebaseAuth.getInstance()

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedMediaUri = it
            Glide.with(this).load(it).into(ivPreview)
            flMediaPreview.visibility = View.VISIBLE
            Toast.makeText(requireContext(), "Image sélectionnée", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[PublicationViewModel::class.java]

        ivPreview         = view.findViewById(R.id.ivPreview)
        btnSelectImage    = view.findViewById(R.id.btnSelectMultiple)
        flMediaPreview    = view.findViewById(R.id.flMediaPreview)
        etTitle           = view.findViewById(R.id.etTitle)
        etDescription     = view.findViewById(R.id.etDescription)
        chipGroupTags     = view.findViewById(R.id.chipGroupTags)
        tvLocationLabel   = view.findViewById(R.id.tvLocationLabel)
        tvVisibilityLabel = view.findViewById(R.id.tvVisibilityLabel)
        btnPublish        = view.findViewById(R.id.btnPublish)
        progressBar       = view.findViewById(R.id.progressBar)
        layoutAddLocation = view.findViewById(R.id.layoutAddLocation)

        val preSelectedGroupId   = arguments?.getString("groupId")   ?: activity?.intent?.getStringExtra("groupId")
        val preSelectedGroupName = arguments?.getString("groupName") ?: activity?.intent?.getStringExtra("groupName")

        if (!preSelectedGroupId.isNullOrEmpty()) {
            selectedGroupId    = preSelectedGroupId
            selectedGroupName  = preSelectedGroupName
            selectedVisibility = "group"
            tvVisibilityLabel.text      = "📁 ${preSelectedGroupName ?: "Groupe"}"
            tvVisibilityLabel.isEnabled = false
            tvVisibilityLabel.alpha     = 0.7f
        }

        setupObservers()
        setupListeners()
        addPredefinedTags()
    }

    // ==================== OBSERVERS ====================

    private fun setupObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            btnPublish.isEnabled   = !loading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show() }
        }

        viewModel.publishSuccess.observe(viewLifecycleOwner) { postId ->
            if (postId.isNullOrEmpty()) return@observe

            Log.d("Publish", "Publication réussie postId=$postId")

            val currentUserId = auth.currentUser?.uid ?: return@observe

            // Capturer les valeurs MAINTENANT avant popBackStack
            val capturedVisibility = selectedVisibility
            val capturedGroupId    = selectedGroupId
            val capturedGroupName  = selectedGroupName
            val capturedTags       = getSelectedTags()
            val capturedLocation = tvLocationLabel.text.toString().trim()
                .takeIf { it.isNotEmpty() && it != "Ajouter un lieu" } ?: ""

            firestore.collection("users").document(currentUserId).get()
                .addOnSuccessListener { userDoc ->
                    val senderName = userDoc.getString("pseudo")
                        ?: userDoc.getString("fullName")
                        ?: "Quelqu'un"

                    when (capturedVisibility) {
                        "group" -> {
                            if (capturedGroupId != null) {
                                // Notif 1 : publication dans le groupe
                                sendGroupPostNotification(
                                    groupId    = capturedGroupId,
                                    groupName  = capturedGroupName,
                                    postId     = postId,
                                    senderName = senderName,
                                    senderId   = currentUserId
                                )
                                // Notif 2 : tag et/ou lieu (seulement si au moins un est présent)
                                val hasTag      = capturedTags.isNotEmpty()
                                val hasLocation = capturedLocation.isNotEmpty()
                                if (hasTag || hasLocation) {
                                    sendTagLocationNotificationToGroupMembers(
                                        groupId      = capturedGroupId,
                                        postId       = postId,
                                        senderName   = senderName,
                                        senderId     = currentUserId,
                                        tags         = capturedTags,
                                        locationName = capturedLocation
                                    )
                                }
                            }
                        }
                        "public" -> {
                            // Notif unique avec tag + lieu pour tous les users
                            sendPublicPostNotification(
                                postId       = postId,
                                senderName   = senderName,
                                senderId     = currentUserId,
                                tags         = capturedTags,
                                locationName = capturedLocation
                            )
                        }
                    }
                }

            viewModel.resetPublishSuccess()
            Toast.makeText(requireContext(), "Publié avec succès !", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
    }

    // ==================== VISIBILITY DIALOG ====================

    private fun showVisibilityDialog() {
        val dialog     = BottomSheetDialog(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.bottom_sheet_visibility, null)

        val container    = dialogView.findViewById<LinearLayout>(R.id.containerGroups)
        val btnOk        = dialogView.findViewById<MaterialButton>(R.id.btnConfirm)
        val optionPublic = dialogView.findViewById<LinearLayout>(R.id.optionPublic)

        container.removeAllViews()

        var selectedType      = "public"
        var tempGroupId: String?   = null
        var tempGroupName: String? = null

        val currentUserId = auth.currentUser?.uid ?: return

        optionPublic.setOnClickListener {
            selectedType  = "public"
            tempGroupId   = null
            tempGroupName = null
        }

        firestore.collection("groups").get()
            .addOnSuccessListener { groupsResult ->
                for (doc in groupsResult) {
                    val docId   = doc.id
                    val docName = doc.getString("name") ?: "Sans nom"

                    firestore.collection("groups")
                        .document(docId)
                        .collection("members")
                        .document(currentUserId)
                        .get()
                        .addOnSuccessListener { memberDoc ->
                            if (memberDoc.exists() && isAdded) {
                                val row = LinearLayout(requireContext()).apply {
                                    orientation = LinearLayout.HORIZONTAL
                                    setPadding(12, 12, 12, 12)
                                    @Suppress("DEPRECATION")
                                    background = resources.getDrawable(android.R.drawable.list_selector_background)
                                }
                                val icon = ImageView(requireContext()).apply {
                                    setImageResource(R.drawable.ic_groups)
                                    layoutParams = LinearLayout.LayoutParams(60, 60)
                                }
                                val text = TextView(requireContext()).apply {
                                    text = "👥 $docName"
                                    setPadding(20, 0, 0, 0)
                                }
                                row.addView(icon)
                                row.addView(text)
                                container.addView(row)

                                row.setOnClickListener {
                                    selectedType  = "group"
                                    tempGroupId   = docId
                                    tempGroupName = docName
                                }
                            }
                        }
                }
            }

        btnOk.setOnClickListener {
            selectedVisibility = selectedType
            selectedGroupId    = tempGroupId
            selectedGroupName  = tempGroupName
            tvVisibilityLabel.text = if (selectedType == "public") "Public" else tempGroupName ?: "Groupe sélectionné"
            dialog.dismiss()
        }

        dialog.setContentView(dialogView)
        dialog.show()
    }

    // ==================== IMAGE ====================

    private fun saveImage(uri: Uri): String {
        val file   = File(requireContext().filesDir, "post_${System.currentTimeMillis()}.jpg")
        val input  = requireContext().contentResolver.openInputStream(uri)
        val output = FileOutputStream(file)
        input?.copyTo(output)
        input?.close()
        output.close()
        return file.absolutePath
    }

    // ==================== PUBLISH ====================

    private fun publishPost() {
        val uri = selectedMediaUri ?: run {
            Toast.makeText(requireContext(), "Veuillez sélectionner une image", Toast.LENGTH_SHORT).show()
            return
        }

        val imagePath    = saveImage(uri)
        val title        = etTitle.text.toString().trim()
        val description  = etDescription.text.toString().trim()
        val locationName = tvLocationLabel.text.toString().trim().takeIf {
            it.isNotEmpty() && it != "Ajouter un lieu"
        } ?: ""
        val isPublic     = selectedVisibility == "public"
        val tags         = getSelectedTags()

        Log.d("Publish", "📸 $imagePath | 📝 $title | 📍 $locationName")
        Log.d("Publish", "👁️ $selectedVisibility | 🏷️ $tags | 👥 $selectedGroupId")

        viewModel.publishPost(
            imagePath    = imagePath,
            title        = title,
            description  = description,
            locationName = locationName,
            isPublic     = isPublic,
            groupId      = selectedGroupId ?: "",
            tags         = tags
        )
    }

    // ==================== TAGS ====================

    private fun getSelectedTags(): List<String> {
        val tags = mutableListOf<String>()
        for (i in 0 until chipGroupTags.childCount) {
            val v = chipGroupTags.getChildAt(i)
            if (v is Chip && v.isChecked) tags.add(v.text.toString())
        }
        return tags
    }

    private fun addPredefinedTags() {
        val predefinedTags = listOf(
            "Montagne", "Plage", "Culture", "Gastronomie", "Coucher de soleil",
            "Photo", "Randonnée", "Sport", "Urbain", "Road trip",
            "Camping", "Nature", "Écotourisme", "Festival", "Mer"
        )
        for (tag in predefinedTags) {
            chipGroupTags.addView(Chip(requireContext()).apply {
                text        = tag
                isCheckable = true
                isChecked   = false
                isClickable = true
            })
        }
    }

    // ==================== LISTENERS ====================

    private fun setupListeners() {
        btnPublish.setOnClickListener { publishPost() }
        btnSelectImage.setOnClickListener { pickImage.launch("image/*") }
        flMediaPreview.setOnClickListener { pickImage.launch("image/*") }
        tvVisibilityLabel.setOnClickListener {
            if (selectedGroupId == null) showVisibilityDialog()
        }
        layoutAddLocation.setOnClickListener { openMapsForLocation() }
    }

    // ==================== LOCATION ====================

    private val locationPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val lat     = result.data?.getDoubleExtra(ActivityLocationPicker.EXTRA_LAT, 0.0) ?: 0.0
            val lng     = result.data?.getDoubleExtra(ActivityLocationPicker.EXTRA_LNG, 0.0) ?: 0.0
            val address = result.data?.getStringExtra(ActivityLocationPicker.EXTRA_ADDRESS) ?: ""
            if (lat != 0.0 && lng != 0.0) {
                selectedLocation     = address
                tvLocationLabel.text = address
            }
        }
    }

    private fun openMapsForLocation() {
        locationPickerLauncher.launch(Intent(requireContext(), ActivityLocationPicker::class.java))
    }

    // ==================== NOTIFICATIONS ====================

    /**
     * GROUPE - Notif 1 : "X a publié dans le groupe Y"
     * → envoyée à tous les membres du groupe sauf l'auteur
     */
    private fun sendGroupPostNotification(
        groupId: String,
        groupName: String?,
        postId: String,
        senderName: String,
        senderId: String
    ) {
        val groupNameValue = groupName ?: "le groupe"
        Log.d("NOTIF", "Notif 1 groupe → groupId=$groupId postId=$postId")

        firestore.collection("groups")
            .document(groupId)
            .collection("members")
            .get()
            .addOnSuccessListener { members ->
                for (memberDoc in members) {
                    val memberId = memberDoc.id
                    if (memberId == senderId) continue

                    val notif = hashMapOf(
                        "type"         to "new_post_in_group",
                        "title"        to "📷 Nouvelle publication dans $groupNameValue",
                        "message"      to "$senderName a publié dans $groupNameValue",
                        "groupId"      to groupId,
                        "groupName"    to groupNameValue,
                        "postId"       to postId,
                        "locationName" to "",
                        "tag"          to "",
                        "senderId"     to senderId,
                        "senderName"   to senderName,
                        "isRead"       to false,
                        "status"       to "active",
                        "timestamp"    to System.currentTimeMillis()
                    )

                    firestore.collection("users")
                        .document(memberId)
                        .collection("notifications")
                        .add(notif)
                        .addOnSuccessListener { Log.d("NOTIF", "✅ Notif groupe → $memberId") }
                        .addOnFailureListener { e -> Log.e("NOTIF", "❌ $memberId : ${e.message}") }
                }
            }
            .addOnFailureListener { e -> Log.e("NOTIF", "❌ membres groupe: ${e.message}") }
    }

    /**
     * GROUPE - Notif 2 : tag et/ou lieu
     * → envoyée uniquement aux membres du groupe (pas à tout le monde)
     * → seulement si tag ou lieu présent
     */
    private fun sendTagLocationNotificationToGroupMembers(
        groupId: String,
        postId: String,
        senderName: String,
        senderId: String,
        tags: List<String>,
        locationName: String
    ) {
        val firstTag    = tags.firstOrNull()
        val hasLocation = locationName.isNotEmpty()

        val message = when {
            firstTag != null && hasLocation ->
                "$senderName a publié une photo de type \"$firstTag\" depuis $locationName"
            firstTag != null ->
                "$senderName a publié une photo de type \"$firstTag\""
            hasLocation ->
                "$senderName a publié une photo depuis $locationName"
            else -> return  // Rien à envoyer
        }

        val title = when {
            firstTag != null -> "🏷️ $firstTag"
            else             -> "📍 $locationName"
        }

        val notifType = when {
            firstTag != null -> "new_post_in_tag"
            else             -> "new_post_in_location"
        }

        Log.d("NOTIF", "Notif 2 tag/lieu groupe → type=$notifType msg=$message")

        firestore.collection("groups")
            .document(groupId)
            .collection("members")
            .get()
            .addOnSuccessListener { members ->
                for (memberDoc in members) {
                    val memberId = memberDoc.id
                    if (memberId == senderId) continue

                    val notif = hashMapOf(
                        "type"         to notifType,
                        "title"        to title,
                        "message"      to message,
                        "groupId"      to groupId,
                        "groupName"    to "",
                        "postId"       to postId,
                        "locationName" to locationName,
                        "tag"          to (firstTag ?: ""),
                        "senderId"     to senderId,
                        "senderName"   to senderName,
                        "isRead"       to false,
                        "status"       to "active",
                        "timestamp"    to System.currentTimeMillis()
                    )

                    firestore.collection("users")
                        .document(memberId)
                        .collection("notifications")
                        .add(notif)
                        .addOnSuccessListener { Log.d("NOTIF", "✅ Notif tag/lieu groupe → $memberId") }
                        .addOnFailureListener { e -> Log.e("NOTIF", "❌ $memberId : ${e.message}") }
                }
            }
            .addOnFailureListener { e -> Log.e("NOTIF", "❌ membres tag/lieu: ${e.message}") }
    }

    /**
     * PUBLIC - Notif unique avec tag + lieu
     * → envoyée à TOUS les users sauf l'auteur
     */
    private fun sendPublicPostNotification(
        postId: String,
        senderName: String,
        senderId: String,
        tags: List<String>,
        locationName: String
    ) {
        val firstTag    = tags.firstOrNull()
        val hasLocation = locationName.isNotEmpty()

        val message = when {
            firstTag != null && hasLocation ->
                "$senderName a publié une photo de type \"$firstTag\" depuis $locationName"
            firstTag != null ->
                "$senderName a publié une photo de type \"$firstTag\""
            hasLocation ->
                "$senderName a publié une photo depuis $locationName"
            else ->
                "$senderName a publié une nouvelle photo"
        }

        val title = when {
            firstTag != null -> "📸 Nouvelle publication · $firstTag"
            hasLocation      -> "📍 Nouvelle publication · $locationName"
            else             -> "📷 Nouvelle publication"
        }

        val notifType = when {
            firstTag != null -> "new_post_in_tag"
            hasLocation      -> "new_post_in_location"
            else             -> "new_post_public"
        }

        Log.d("NOTIF", "Notif public → type=$notifType msg=$message")

        firestore.collection("users").get()
            .addOnSuccessListener { users ->
                for (userDoc in users) {
                    val userId = userDoc.id
                    if (userId == senderId) continue

                    val notif = hashMapOf(
                        "type"         to notifType,
                        "title"        to title,
                        "message"      to message,
                        "groupId"      to "",
                        "groupName"    to "",
                        "postId"       to postId,
                        "locationName" to locationName,
                        "tag"          to (firstTag ?: ""),
                        "senderId"     to senderId,
                        "senderName"   to senderName,
                        "isRead"       to false,
                        "status"       to "active",
                        "timestamp"    to System.currentTimeMillis()
                    )

                    firestore.collection("users")
                        .document(userId)
                        .collection("notifications")
                        .add(notif)
                        .addOnSuccessListener { Log.d("NOTIF", "✅ Notif public → $userId") }
                        .addOnFailureListener { e -> Log.e("NOTIF", "❌ $userId : ${e.message}") }
                }
            }
            .addOnFailureListener { e -> Log.e("NOTIF", "❌ users: ${e.message}") }
    }
}