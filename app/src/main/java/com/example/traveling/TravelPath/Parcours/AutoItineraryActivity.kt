package com.example.traveling.TravelPath.Parcours

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.traveling.R

class AutoItineraryActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etCity: EditText
    private lateinit var btnNext: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auto_itinerary)

        etName = findViewById(R.id.etAutoName)
        etCity = findViewById(R.id.etAutoCity)
        btnNext = findViewById(R.id.btnNext)

        // Récupérer les données envoyées par TravelShare
        val preFilledName = intent.getStringExtra("itinerary_name")
        val preFilledCity = intent.getStringExtra("city")

        if (!preFilledName.isNullOrEmpty()) {
            etName.setText(preFilledName)
        }
        if (!preFilledCity.isNullOrEmpty()) {
            etCity.setText(preFilledCity)
        }

        btnNext.setOnClickListener {
            val name = etName.text.toString().trim()
            val city = etCity.text.toString().trim()
            if (name.isEmpty() || city.isEmpty()) {
                Toast.makeText(this, "Veuillez saisir un nom et une ville", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(this, PreferencesActivity::class.java)
            intent.putExtra("itinerary_name", name)
            intent.putExtra("city", city)
            startActivity(intent)
        }
    }
}