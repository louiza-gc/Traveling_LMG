package com.example.traveling.TravelPath.Parcours

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.traveling.R
import com.example.traveling.TravelPath.Accueil.PlaceRepository
import kotlinx.coroutines.launch

class ItineraryDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_itinerary_detail)

        val itinerary = intent.getSerializableExtra("itinerary") as? Itinerary
        if (itinerary == null) {
            Toast.makeText(this, "Itinéraire introuvable", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        findViewById<TextView>(R.id.detail_title).text = itinerary.name
        findViewById<TextView>(R.id.detail_desc).text = itinerary.description

        lifecycleScope.launch {
            val allPlaces = PlaceRepository.loadAllPlaces()
            val places = allPlaces.filter { it.id in itinerary.placeIds }

            val totalCost = places.sumOf { it.details.costEstimate.adult }
            val totalDuration = places.sumOf { it.details.typicalDurationMinutes }
            val avgEffort = if (places.isNotEmpty()) places.map { it.details.effortLevel }.average() else 0.0

            findViewById<TextView>(R.id.detail_cost).text = "Coût total : %.2f €".format(totalCost)
            findViewById<TextView>(R.id.detail_duration).text = "Durée totale : ${totalDuration} min"
            findViewById<TextView>(R.id.detail_effort).text = "Effort moyen : %.1f/5".format(avgEffort)

            val steps = places.mapIndexed { index, place ->
                Step(
                    order = index + 1,
                    placeId = place.id,
                    arrivalTime = "",
                    departureTime = "",
                    durationMinutes = place.details.typicalDurationMinutes,
                    cost = place.details.costEstimate.adult,
                    effort = place.details.effortLevel,
                    distanceFromPreviousKm = 0.0
                )
            }
            val recyclerSteps = findViewById<RecyclerView>(R.id.recyclerSteps)
            recyclerSteps.layoutManager = LinearLayoutManager(this@ItineraryDetailActivity)
            recyclerSteps.adapter = StepsAdapter(steps)
        }
    }

    private inner class StepsAdapter(private val steps: List<Step>) :
        RecyclerView.Adapter<StepsAdapter.StepViewHolder>() {

        inner class StepViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
            val title: TextView = itemView.findViewById(android.R.id.text1)
            val subtitle: TextView = itemView.findViewById(android.R.id.text2)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): StepViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            return StepViewHolder(view)
        }

        override fun onBindViewHolder(holder: StepViewHolder, position: Int) {
            val step = steps[position]
            holder.title.text = "${step.order}. ${step.placeId}"
            holder.subtitle.text = "Coût: ${step.cost} € | Effort: ${step.effort}/5 | Durée: ${step.durationMinutes} min"
        }

        override fun getItemCount() = steps.size
    }
}