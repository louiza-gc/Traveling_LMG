package com.example.traveling.TravelPath.Parcours

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.traveling.R
import com.example.traveling.TravelPath.Accueil.PlaceRepository
import kotlinx.coroutines.launch

class PreferencesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preferences)

        // Récupération des extras envoyés par AutoItineraryActivity
        val itineraryName = intent.getStringExtra("itinerary_name") ?: ""
        val city = intent.getStringExtra("city") ?: "Paris"

        // Remplir la barre d'action
        supportActionBar?.title = if (itineraryName.isNotEmpty()) itineraryName else "Préférences"

        val cbDiscover = findViewById<CheckBox>(R.id.cb_discover)
        val cbCulture = findViewById<CheckBox>(R.id.cb_culture)
        val cbResto = findViewById<CheckBox>(R.id.cb_resto)
        val cbNature = findViewById<CheckBox>(R.id.cb_nature)
        val cbNightlife = findViewById<CheckBox>(R.id.cb_nightlife)
        val etBudget = findViewById<EditText>(R.id.et_budget)
        val seekEffort = findViewById<SeekBar>(R.id.seek_effort)
        val tvEffortValue = findViewById<TextView>(R.id.tv_effort_value)
        val btnGenerate = findViewById<Button>(R.id.btn_generate)

        seekEffort.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvEffortValue.text = "Niveau sélectionné : ${progress + 1}"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        tvEffortValue.text = "Niveau sélectionné : ${seekEffort.progress + 1}"

        btnGenerate.setOnClickListener {
            val activities = mutableListOf<String>()
            if (cbDiscover.isChecked) activities.add("découverte")
            if (cbCulture.isChecked) activities.add("culture")
            if (cbResto.isChecked) activities.add("restauration")
            if (cbNature.isChecked) activities.add("nature")
            if (cbNightlife.isChecked) activities.add("vie nocturne")

            val budget = etBudget.text.toString().toDoubleOrNull() ?: 100.0
            val effort = seekEffort.progress + 1

            val prefs = UserPreferences(
                activities = activities,
                favoritePlaces = emptyList(),
                mandatoryPlaces = emptyList(),
                budgetMax = budget,
                durationHours = 4,
                effortMax = effort,
                sensitivity = Sensitivity(),
                weatherForecast = WeatherForecast("sunny", 22, 50, 10)
            )

            lifecycleScope.launch {
                PlaceRepository.init(applicationContext)
                val allPlaces = PlaceRepository.loadAllPlaces()
                // Filtrer par ville (optionnel)
                val filtered = allPlaces.filter { it.location.city.equals(city, ignoreCase = true) }
                val itineraries = ItineraryPlanner.generateItineraries(filtered, prefs)
                val intent = Intent(this@PreferencesActivity, ItineraryActivity::class.java)
                intent.putExtra("itineraries", ArrayList(itineraries))
                intent.putExtra("itinerary_name", itineraryName)
                startActivity(intent)
            }
        }
    }
}