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
import java.util.Date

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

    // Sélecteur d'image
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedMediaUri = it
            Glide.with(this).load(it).into(ivPreview)
            Toast.makeText(requireContext(), "Image sélectionnée", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[PublicationViewModel::class.java]

        // Initialisation des vues
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

        setupObservers()
        setupListeners()

        // Pour les tags
        addPredefinedTags()
    }

    private fun showVisibilityDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_visibility, null)

        val container = view.findViewById<LinearLayout>(R.id.containerGroups)
        val btnOk = view.findViewById<MaterialButton>(R.id.btnConfirm)
        val optionPublic = view.findViewById<LinearLayout>(R.id.optionPublic)

        container.removeAllViews()

        var selectedType = "public"
        var tempSelectedGroupId: String? = null

        optionPublic.setOnClickListener {
            selectedType = "public"
            tempSelectedGroupId = null
        }

        val groupMap = mutableMapOf<LinearLayout, String>()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("groups")
            .whereArrayContains("members", uid)
            .get()
            .addOnSuccessListener { result ->
                for (doc in result) {
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
                        text = "👥 ${doc.getString("name")}"
                        setPadding(20, 0, 0, 0)
                    }

                    row.addView(icon)
                    row.addView(text)
                    container.addView(row)
                    groupMap[row] = doc.id

                    row.setOnClickListener {
                        selectedType = "group"
                        tempSelectedGroupId = groupMap[row]
                    }
                }
            }

        btnOk.setOnClickListener {
            selectedVisibility = selectedType
            selectedGroupId = tempSelectedGroupId
            tvVisibilityLabel.text = if (selectedType == "public") "Public" else "Groupe sélectionné"
            dialog.dismiss()
        }

        dialog.setContentView(view)
        dialog.show()
    }

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

    private fun publishPost() {
        val uri = selectedMediaUri
        if (uri == null) {
            Toast.makeText(requireContext(), "Veuillez sélectionner une image", Toast.LENGTH_SHORT).show()
            return
        }

        val imagePath = saveImage(uri)
        val title = etTitle.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val locationName = tvLocationLabel.text.toString()
        val isPublic = selectedVisibility == "public"

        val tags = mutableListOf<String>()
        for (i in 0 until chipGroupTags.childCount) {
            val v = chipGroupTags.getChildAt(i)
            if (v is Chip && v.isChecked) {
                tags.add(v.text.toString())
            }
        }

        Log.d("Publish", "========== PUBLICATION ==========")
        Log.d("Publish", "📸 Image: $imagePath")
        Log.d("Publish", "📝 Titre: $title")
        Log.d("Publish", "📍 Lieu: $locationName")
        Log.d("Publish", "👁️ Public: $isPublic")
        Log.d("Publish", "🏷️ Tags: $tags")

        viewModel.publishPost(
            imagePath = imagePath,
            title = title,
            description = description,
            locationName = locationName,
            isPublic = isPublic,
            groupId = selectedGroupId ?: "",
            tags = tags
        )
    }

    private fun setupListeners() {
        btnPublish.setOnClickListener {
            publishPost()
        }

        btnSelectImage.setOnClickListener {
            pickImage.launch("image/*")
        }

        flMediaPreview.setOnClickListener {
            pickImage.launch("image/*")
        }

        tvVisibilityLabel.setOnClickListener {
            showVisibilityDialog()
        }

        layoutAddLocation.setOnClickListener {
            openMapsForLocation()
        }
    }

    private val locationPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val lat = result.data?.getDoubleExtra(ActivityLocationPicker.EXTRA_LAT, 0.0) ?: 0.0
            val lng = result.data?.getDoubleExtra(ActivityLocationPicker.EXTRA_LNG, 0.0) ?: 0.0
            val address = result.data?.getStringExtra(ActivityLocationPicker.EXTRA_ADDRESS) ?: ""

            if (lat != 0.0 && lng != 0.0) {
                selectedLocation = address
                tvLocationLabel.text = "$address"
            }
        }
    }

    private fun openMapsForLocation() {
        val intent = Intent(requireContext(), ActivityLocationPicker::class.java)
        locationPickerLauncher.launch(intent)
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner) {
            progressBar.visibility = if (it) View.VISIBLE else View.GONE
        }

        viewModel.publishSuccess.observe(viewLifecycleOwner) {
            if (it == true) {
                Toast.makeText(requireContext(), "Publié avec succès !", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }

        viewModel.error.observe(viewLifecycleOwner) {
            it?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun addPredefinedTags() {
        // Liste des tags disponibles pour la recherche/filtre
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

        Log.d("Tags", "${predefinedTags.size} tags ajoutés au ChipGroup")
    }
}