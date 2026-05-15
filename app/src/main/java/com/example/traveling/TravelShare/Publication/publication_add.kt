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
    private var currentPostId: String = ""

    private var publishObserverAttached = false

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

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

        ivPreview = view.findViewById(R.id.ivPreview)
        btnSelectImage = view.findViewById(R.id.btnSelectMultiple)
        flMediaPreview = view.findViewById(R.id.flMediaPreview)
        etTitle = view.findViewById(R.id.etTitle)
        etDescription = view.findViewById(R.id.etDescription)
        chipGroupTags = view.findViewById(R.id.chipGroupTags)
        tvLocationLabel = view.findViewById(R.id.tvLocationLabel)
        tvVisibilityLabel = view.findViewById(R.id.tvVisibilityLabel)
        btnPublish = view.findViewById(R.id.btnPublish)
        progressBar = view.findViewById(R.id.progressBar)
        layoutAddLocation = view.findViewById(R.id.layoutAddLocation)

        val preSelectedGroupId = arguments?.getString("groupId")
            ?: activity?.intent?.getStringExtra("groupId")
        val preSelectedGroupName = arguments?.getString("groupName")
            ?: activity?.intent?.getStringExtra("groupName")

        if (!preSelectedGroupId.isNullOrEmpty()) {
            selectedGroupId = preSelectedGroupId
            selectedGroupName = preSelectedGroupName
            selectedVisibility = "group"
            tvVisibilityLabel.text = "📁 ${preSelectedGroupName ?: "Groupe"}"
            tvVisibilityLabel.isEnabled = false
            tvVisibilityLabel.alpha = 0.7f
        }

        setupObservers()
        setupListeners()
        addPredefinedTags()
    }

    // ==================== VISIBILITY DIALOG ====================

    private fun showVisibilityDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.bottom_sheet_visibility, null)

        val container = dialogView.findViewById<LinearLayout>(R.id.containerGroups)
        val btnOk = dialogView.findViewById<MaterialButton>(R.id.btnConfirm)
        val optionPublic = dialogView.findViewById<LinearLayout>(R.id.optionPublic)

        container.removeAllViews()

        var selectedType = "public"
        var tempGroupId: String? = null
        var tempGroupName: String? = null

        val currentUserId = auth.currentUser?.uid ?: return

        optionPublic.setOnClickListener {
            selectedType = "public"
            tempGroupId = null
            tempGroupName = null
        }

        firestore.collection("groups")
            .get()
            .addOnSuccessListener { groupsResult ->
                for (doc in groupsResult) {
                    val docId = doc.id
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
                                    selectedType = "group"
                                    tempGroupId = docId
                                    tempGroupName = docName
                                }
                            }
                        }
                }
            }

        btnOk.setOnClickListener {
            selectedVisibility = selectedType
            selectedGroupId = tempGroupId
            selectedGroupName = tempGroupName

            tvVisibilityLabel.text = if (selectedType == "public") {
                "Public"
            } else {
                tempGroupName ?: "Groupe sélectionné"
            }
            dialog.dismiss()
        }

        dialog.setContentView(dialogView)
        dialog.show()
    }

    // ==================== IMAGE ====================

    private fun saveImage(uri: Uri): String {
        val file = File(
            requireContext().filesDir,
            "post_${System.currentTimeMillis()}.jpg"
        )
        val input = requireContext().contentResolver.openInputStream(uri)
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

        val capturedGroupId = selectedGroupId
        val capturedGroupName = selectedGroupName
        val capturedVisibility = selectedVisibility

        val imagePath = saveImage(uri)
        val title = etTitle.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val locationName = tvLocationLabel.text.toString()
        val isPublic = capturedVisibility == "public"
        val tags = getSelectedTags()

        Log.d("Publish", "========== PUBLICATION ==========")
        Log.d("Publish", "📸 Image: $imagePath")
        Log.d("Publish", "📝 Titre: $title")
        Log.d("Publish", "📍 Lieu: $locationName")
        Log.d("Publish", "👁️ Visibilité: $capturedVisibility")
        Log.d("Publish", "🏷️ Tags: $tags")
        Log.d("Publish", "👥 Groupe ID: ${capturedGroupId ?: "aucun"}")

        if (!publishObserverAttached) {
            publishObserverAttached = true
            viewModel.publishSuccess.observe(viewLifecycleOwner) { success ->
                if (success == true) {
                    val currentUserId = auth.currentUser?.uid ?: return@observe

                    firestore.collection("users")
                        .document(currentUserId)
                        .get()
                        .addOnSuccessListener { userDoc ->
                            val senderName = userDoc.getString("pseudo")
                                ?: userDoc.getString("fullName")
                                ?: "Quelqu'un"

                            firestore.collection("photos")
                                .whereEqualTo("authorId", currentUserId)
                                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                                .limit(1)
                                .get()
                                .addOnSuccessListener { result ->
                                    val postDoc = result.documents.firstOrNull()
                                    if (postDoc != null) {
                                        currentPostId = postDoc.id

                                        if (capturedVisibility == "group" && capturedGroupId != null) {
                                            sendGroupPostNotification(capturedGroupId, capturedGroupName, currentPostId, senderName)
                                        }

                                        if (locationName.isNotEmpty() && locationName != "Ajouter un lieu") {
                                            sendLocationBasedNotification(locationName, currentPostId, senderName)
                                        }

                                        if (tags.isNotEmpty()) {
                                            sendTagBasedNotification(tags, currentPostId, senderName)
                                        }
                                    }
                                }
                        }

                    Toast.makeText(requireContext(), "Publié avec succès !", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            }
        }

        viewModel.publishPost(
            imagePath = imagePath,
            title = title,
            description = description,
            locationName = locationName,
            isPublic = isPublic,
            groupId = capturedGroupId ?: "",
            tags = tags
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
            "Montagne", "Plage", "Culture",
            "Gastronomie", "Coucher de soleil", "Photo",
            "Randonnée", "Sport", "Urbain",
            "Road trip", "Camping", "Nature",
            "Écotourisme", "Festival", "Mer"
        )
        for (tag in predefinedTags) {
            val chip = Chip(requireContext()).apply {
                text = tag
                isCheckable = true
                isChecked = false
                isClickable = true
            }
            chipGroupTags.addView(chip)
        }
        Log.d("Tags", "${predefinedTags.size} tags ajoutés")
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
            val lat = result.data?.getDoubleExtra(ActivityLocationPicker.EXTRA_LAT, 0.0) ?: 0.0
            val lng = result.data?.getDoubleExtra(ActivityLocationPicker.EXTRA_LNG, 0.0) ?: 0.0
            val address = result.data?.getStringExtra(ActivityLocationPicker.EXTRA_ADDRESS) ?: ""

            if (lat != 0.0 && lng != 0.0) {
                selectedLocation = address
                tvLocationLabel.text = address
            }
        }
    }

    private fun openMapsForLocation() {
        val intent = Intent(requireContext(), ActivityLocationPicker::class.java)
        locationPickerLauncher.launch(intent)
    }

    // ==================== OBSERVERS ====================

    private fun setupObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            btnPublish.isEnabled = !loading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }
    }

    // ==================== NOTIFICATIONS ====================

    private fun sendGroupPostNotification(groupId: String, groupName: String?, postId: String, senderName: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val groupNameValue = groupName ?: "le groupe"

        Log.d("NOTIF_GROUP", "=== sendGroupPostNotification ===")
        Log.d("NOTIF_GROUP", "groupId=$groupId | groupName=$groupNameValue | postId=$postId | senderName=$senderName")

        firestore.collection("groups")
            .document(groupId)
            .collection("members")
            .get()
            .addOnSuccessListener { members ->
                for (memberDoc in members) {
                    val memberId = memberDoc.id
                    if (memberId == currentUserId) continue

                    val notificationData = hashMapOf(
                        "type" to "new_post_in_group",
                        "title" to "📷 Nouvelle publication dans le groupe",
                        "message" to "$senderName a publié dans $groupNameValue",
                        "groupId" to groupId,
                        "groupName" to groupNameValue,
                        "postId" to postId,
                        "senderId" to currentUserId,
                        "senderName" to senderName,
                        "isRead" to false,
                        "status" to "active",
                        "timestamp" to System.currentTimeMillis()
                    )

                    firestore.collection("users")
                        .document(memberId)
                        .collection("notifications")
                        .add(notificationData)
                }
            }
    }

    private fun sendLocationBasedNotification(locationName: String, postId: String, senderName: String) {
        val currentUserId = auth.currentUser?.uid ?: return

        Log.d("NOTIF_LOCATION", "=== sendLocationBasedNotification ===")
        Log.d("NOTIF_LOCATION", "lieu: $locationName | postId: $postId")

        firestore.collection("users")
            .get()
            .addOnSuccessListener { users ->
                for (userDoc in users) {
                    val userId = userDoc.id
                    if (userId == currentUserId) continue

                    val followedLocations = userDoc.get("followedLocations") as? List<String> ?: emptyList()

                    if (followedLocations.any { locationName.contains(it, ignoreCase = true) }) {
                        val notificationData = hashMapOf(
                            "type" to "new_post_in_location",
                            "title" to "📍 Nouvelle photo à $locationName",
                            "message" to "$senderName a publié une photo à $locationName",
                            "postId" to postId,
                            "locationName" to locationName,
                            "senderId" to currentUserId,
                            "senderName" to senderName,
                            "isRead" to false,
                            "status" to "active",
                            "timestamp" to System.currentTimeMillis()
                        )

                        firestore.collection("users")
                            .document(userId)
                            .collection("notifications")
                            .add(notificationData)
                    }
                }
            }
    }

    private fun sendTagBasedNotification(tags: List<String>, postId: String, senderName: String) {
        val currentUserId = auth.currentUser?.uid ?: return

        Log.d("NOTIF_TAG", "=== sendTagBasedNotification ===")
        Log.d("NOTIF_TAG", "tags: $tags | postId: $postId")

        firestore.collection("users")
            .get()
            .addOnSuccessListener { users ->
                for (userDoc in users) {
                    val userId = userDoc.id
                    if (userId == currentUserId) continue

                    val followedTags = userDoc.get("followedTags") as? List<String> ?: emptyList()
                    val matchingTags = tags.filter { it in followedTags }

                    if (matchingTags.isNotEmpty()) {
                        val notificationData = hashMapOf(
                            "type" to "new_post_in_tag",
                            "title" to "🏷️ Nouvelle photo sur ${matchingTags.first()}",
                            "message" to "$senderName a publié une photo sur ${matchingTags.joinToString(", ")}",
                            "postId" to postId,
                            "tags" to matchingTags,
                            "senderId" to currentUserId,
                            "senderName" to senderName,
                            "isRead" to false,
                            "status" to "active",
                            "timestamp" to System.currentTimeMillis()
                        )

                        firestore.collection("users")
                            .document(userId)
                            .collection("notifications")
                            .add(notificationData)
                    }
                }
            }
    }
}