package com.example.traveling.TravelShare.Acceuil

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.traveling.R
import com.example.traveling.TravelShare.Publication.ActivityLocationPicker
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class page_filters : AppCompatActivity() {

    private lateinit var tvStartDate: TextView
    private lateinit var tvEndDate: TextView
    private lateinit var ivStartCal: ImageView
    private lateinit var ivEndCal: ImageView
    private lateinit var actvAuteur: AutoCompleteTextView
    private lateinit var etLieu: EditText
    private lateinit var ivLocIcon: ImageView
    private lateinit var actvRayon: AutoCompleteTextView
    private lateinit var btnApply: Button
    private lateinit var btnReset: Button
    private lateinit var btnNotifFilter: ImageButton
    private lateinit var containerCheckboxes: LinearLayout

    private var selectedStartDate: Long = 0
    private var selectedEndDate: Long = 0
    private val selectedPlaceTypes = mutableListOf<String>()

    // Pour stocker les coordonnées du lieu sélectionné
    private var selectedLatitude: Double = 0.0
    private var selectedLongitude: Double = 0.0

    private val firestore = FirebaseFirestore.getInstance()
    private var authorsList = mutableListOf<String>()

    private val placeTypes = listOf(
        "Montagne", "Plage", "Culture",
        "Gastronomie", "Coucher de soleil", "Photo",
        "Randonnée", "Sport", "Urbain",
        "Road trip", "Camping", "Nature",
        "Écotourisme", "Festival", "Mer"
    )

    // Launcher pour ActivityLocationPicker
    private val locationPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val lat = result.data?.getDoubleExtra(ActivityLocationPicker.EXTRA_LAT, 0.0) ?: 0.0
            val lng = result.data?.getDoubleExtra(ActivityLocationPicker.EXTRA_LNG, 0.0) ?: 0.0
            val address = result.data?.getStringExtra(ActivityLocationPicker.EXTRA_ADDRESS) ?: ""

            if (lat != 0.0 && lng != 0.0 && address.isNotEmpty()) {
                selectedLatitude = lat
                selectedLongitude = lng
                etLieu.setText(address)
                Toast.makeText(this, "📍 Lieu sélectionné: $address", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page_filters)

        initViews()
        setupCheckboxes()
        setupDropdowns()
        loadAuthors()
        setupListeners()
    }

    private fun initViews() {
        tvStartDate = findViewById(R.id.tvStartDate)
        tvEndDate = findViewById(R.id.tvEndDate)
        ivStartCal = findViewById(R.id.ivStartCal)
        ivEndCal = findViewById(R.id.ivEndCal)
        actvAuteur = findViewById(R.id.actvAuteur)
        etLieu = findViewById(R.id.etLieu)
        ivLocIcon = findViewById(R.id.ivLocIcon)
        actvRayon = findViewById(R.id.actvRayon)
        btnApply = findViewById(R.id.btnApplyFilters)
        btnReset = findViewById(R.id.btnResetFilters)
        btnNotifFilter = findViewById(R.id.btnNotifFilter)
        containerCheckboxes = findViewById(R.id.containerCheckboxes)
    }

    private fun setupCheckboxes() {
        for (type in placeTypes) {
            val checkbox = CheckBox(this).apply {
                text = type
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 8, 0, 8)
                }
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        if (!selectedPlaceTypes.contains(type)) {
                            selectedPlaceTypes.add(type)
                        }
                    } else {
                        selectedPlaceTypes.remove(type)
                    }
                }
            }
            containerCheckboxes.addView(checkbox)
        }
    }

    private fun setupDropdowns() {
        val radiusList = listOf("1 km", "5 km", "10 km", "20 km", "50 km", "100 km")
        val radiusAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, radiusList)
        actvRayon.setAdapter(radiusAdapter)
    }

    private fun loadAuthors() {
        firestore.collection("users")
            .get()
            .addOnSuccessListener { result ->
                authorsList.clear()
                for (doc in result) {
                    val pseudo = doc.getString("pseudo")
                    if (!pseudo.isNullOrEmpty()) {
                        authorsList.add(pseudo)
                    }
                }
                val authorAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, authorsList)
                actvAuteur.setAdapter(authorAdapter)
            }
    }

    private fun setupListeners() {
        // Sélecteur de date début
        ivStartCal.setOnClickListener { showDatePicker(true) }
        tvStartDate.setOnClickListener { showDatePicker(true) }

        // Sélecteur de date fin
        ivEndCal.setOnClickListener { showDatePicker(false) }
        tvEndDate.setOnClickListener { showDatePicker(false) }

        // Clic sur l'icône localisation ou sur le champ lieu
        ivLocIcon.setOnClickListener { openLocationPicker() }
        etLieu.setOnClickListener { openLocationPicker() }

        btnApply.setOnClickListener { applyFiltersAndReturn() }
        btnReset.setOnClickListener { resetFilters() }
        btnNotifFilter.setOnClickListener {
            Toast.makeText(this, "Notifications de filtres à venir", Toast.LENGTH_SHORT).show()
        }
    }

    // Ouvre ActivityLocationPicker
    private fun openLocationPicker() {
        val intent = Intent(this, ActivityLocationPicker::class.java)
        locationPickerLauncher.launch(intent)
    }

    private fun showDatePicker(isStartDate: Boolean) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            val selectedCal = Calendar.getInstance().apply {
                set(year, month, dayOfMonth, 0, 0, 0)
            }
            val timestamp = selectedCal.timeInMillis
            val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedCal.time)

            if (isStartDate) {
                selectedStartDate = timestamp
                tvStartDate.text = dateStr
            } else {
                selectedEndDate = timestamp + (24 * 60 * 60 * 1000) - 1
                tvEndDate.text = dateStr
            }
        }, year, month, day).show()
    }

    private fun applyFiltersAndReturn() {
        val locationText = etLieu.text.toString().trim()
        val radiusText = actvRayon.text.toString().trim()

        // Vérifier si rayon sans lieu
        val finalRadius = if (radiusText.isNotEmpty() && locationText.isEmpty()) {
            Toast.makeText(this, "📍 Veuillez d'abord entrer un lieu pour utiliser le rayon", Toast.LENGTH_SHORT).show()
            ""
        } else {
            radiusText
        }

        Log.d("Filters", "=== APPLICATION FILTRES ===")
        Log.d("Filters", "Lieu: '$locationText'")
        Log.d("Filters", "Rayon: '$finalRadius'")
        Log.d("Filters", "Latitude: $selectedLatitude, Longitude: $selectedLongitude")

        val filters = FilterOptions(
            placeTypes = selectedPlaceTypes.toList(),
            author = actvAuteur.text.toString().trim(),
            location = locationText,
            radius = finalRadius,
            startDate = selectedStartDate,
            endDate = selectedEndDate
        )

        val intent = Intent().apply {
            putExtra("filters", filters)
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun resetFilters() {
        // Réinitialiser les checkboxes
        for (i in 0 until containerCheckboxes.childCount) {
            val view = containerCheckboxes.getChildAt(i)
            if (view is CheckBox) {
                view.isChecked = false
            }
        }
        selectedPlaceTypes.clear()

        // Réinitialiser les dates
        selectedStartDate = 0
        selectedEndDate = 0
        tvStartDate.text = ""
        tvEndDate.text = ""

        // Réinitialiser le lieu et les coordonnées
        etLieu.setText("")
        selectedLatitude = 0.0
        selectedLongitude = 0.0

        actvAuteur.setText("")
        actvRayon.setText("")

        Toast.makeText(this, "✅ Filtres réinitialisés", Toast.LENGTH_SHORT).show()

        val filters = FilterOptions(
            placeTypes = emptyList(),
            author = "",
            location = "",
            radius = "",
            startDate = 0,
            endDate = 0
        )
        val intent = Intent().apply {
            putExtra("filters", filters)
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }
}