package com.example.traveling.TravelShare.Acceuil

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.traveling.R
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class page_filters : AppCompatActivity() {

    private lateinit var tvStartDate: TextView
    private lateinit var tvEndDate: TextView
    private lateinit var ivStartCal: ImageView
    private lateinit var ivEndCal: ImageView
    private lateinit var actvAuteur: AutoCompleteTextView
    private lateinit var etLieu: android.widget.EditText
    private lateinit var actvRayon: AutoCompleteTextView
    private lateinit var btnApply: Button
    private lateinit var btnReset: Button
    private lateinit var btnNotifFilter: ImageButton
    private lateinit var containerCheckboxes: LinearLayout

    private var selectedStartDate: Long = 0
    private var selectedEndDate: Long = 0
    private val selectedPlaceTypes = mutableListOf<String>()

    private val firestore = FirebaseFirestore.getInstance()
    private var authorsList = mutableListOf<String>()

    // Liste des types de lieux disponibles
    private val placeTypes = listOf(
        "Montagne", "Plage", "Culture",
        "Gastronomie", "Coucher de soleil", "Photo",
        "Randonnée", "Sport", "Urbain",
        "Road trip", "Camping", "Nature",
        "Écotourisme", "Festival", "Mer"
    )

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
        actvRayon = findViewById(R.id.actvRayon)
        btnApply = findViewById(R.id.btnApplyFilters)
        btnReset = findViewById(R.id.btnResetFilters)
        btnNotifFilter = findViewById(R.id.btnNotifFilter)
        containerCheckboxes = findViewById(R.id.containerCheckboxes)

        // Initialiser les dates par défaut
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -30)
        selectedStartDate = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 30)
        selectedEndDate = calendar.timeInMillis
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
                        selectedPlaceTypes.add(type)
                    } else {
                        selectedPlaceTypes.remove(type)
                    }
                }
            }
            containerCheckboxes.addView(checkbox)
        }
    }

    private fun setupDropdowns() {
        // Rayons
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
        ivStartCal.setOnClickListener {
            showDatePicker(true)
        }
        tvStartDate.setOnClickListener {
            showDatePicker(true)
        }

        // Sélecteur de date fin
        ivEndCal.setOnClickListener {
            showDatePicker(false)
        }
        tvEndDate.setOnClickListener {
            showDatePicker(false)
        }

        // Bouton Appliquer
        btnApply.setOnClickListener {
            applyFiltersAndReturn()
        }

        // Bouton Réinitialiser
        btnReset.setOnClickListener {
            resetFilters()
        }

        // Bouton notification (optionnel)
        btnNotifFilter.setOnClickListener {
            Toast.makeText(this, "Notifications de filtres à venir", Toast.LENGTH_SHORT).show()
        }
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
                // Ajouter la fin de journée pour la date de fin
                selectedEndDate = timestamp + (24 * 60 * 60 * 1000) - 1
                tvEndDate.text = dateStr
            }
        }, year, month, day).show()
    }

    private fun applyFiltersAndReturn() {
        val filters = FilterOptions(
            placeTypes = selectedPlaceTypes.toList(),
            author = actvAuteur.text.toString(),
            location = etLieu.text.toString(),
            radius = actvRayon.text.toString(),
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

        // Réinitialiser les dates (dernier mois)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -30)
        selectedStartDate = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 30)
        selectedEndDate = calendar.timeInMillis
        tvStartDate.text = ""
        tvEndDate.text = ""

        actvAuteur.setText("")
        etLieu.setText("")
        actvRayon.setText("")

        Toast.makeText(this, "Filtres réinitialisés", Toast.LENGTH_SHORT).show()
    }
}