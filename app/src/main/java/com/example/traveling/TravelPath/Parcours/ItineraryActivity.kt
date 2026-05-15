package com.example.traveling.TravelPath.Parcours

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.traveling.R

class ItineraryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_itinerary)

        val recycler = findViewById<RecyclerView>(R.id.recyclerItineraries)
        recycler.layoutManager = LinearLayoutManager(this)

        // Récupérer les itinéraires générés
        val itineraries = intent.getSerializableExtra("itineraries") as? List<Itinerary> ?: emptyList()
        if (itineraries.isEmpty()) {
            Toast.makeText(this, "Aucun itinéraire généré", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Récupérer le nom personnalisé (ex: "Tour de Paris") passé depuis PreferencesActivity
        val itineraryName = intent.getStringExtra("itinerary_name") ?: "Parcours généré"
        supportActionBar?.title = itineraryName

        val adapter = ItineraryAdapter(itineraries) { selectedItinerary ->
            val intent = Intent(this, ItineraryDetailActivity::class.java)
            intent.putExtra("itinerary", selectedItinerary)
            startActivity(intent)
        }
        recycler.adapter = adapter
    }
}