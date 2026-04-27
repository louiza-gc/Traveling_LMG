package com.example.traveling.TravelPath.Accueil

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

        val itineraries = intent.getSerializableExtra("itineraries") as? List<Itinerary> ?: emptyList()
        if (itineraries.isEmpty()) {
            Toast.makeText(this, "Aucun itinéraire généré", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val adapter = ItineraryAdapter(itineraries) { selected ->
            val intent = Intent(this, ItineraryDetailActivity::class.java)
            intent.putExtra("itinerary", selected)
            startActivity(intent)
        }
        recycler.adapter = adapter
    }
}