package com.example.traveling.TravelPath.Accueil

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.traveling.R

class ItineraryDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_itinerary_detail)

        val itinerary = intent.getSerializableExtra("itinerary") as? Itinerary
        if (itinerary == null) {
            Toast.makeText(this, "Erreur : itinéraire introuvable", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        findViewById<TextView>(R.id.detail_title).text = itinerary.name
        findViewById<TextView>(R.id.detail_desc).text = itinerary.description
        findViewById<TextView>(R.id.detail_cost).text = "Coût total : %.2f €".format(itinerary.totalCost)
        findViewById<TextView>(R.id.detail_duration).text = "Durée totale : ${itinerary.totalDurationMinutes} min"
        findViewById<TextView>(R.id.detail_effort).text = "Effort moyen : %.1f/5".format(itinerary.averageEffort)

        val recyclerSteps = findViewById<RecyclerView>(R.id.recyclerSteps)
        recyclerSteps.layoutManager = LinearLayoutManager(this)
        recyclerSteps.adapter = StepsAdapter(itinerary.steps)
    }

    private inner class StepsAdapter(private val steps: List<Step>) :
        RecyclerView.Adapter<StepsAdapter.StepViewHolder>() {

        inner class StepViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val title: TextView = itemView.findViewById(android.R.id.text1)
            val subtitle: TextView = itemView.findViewById(android.R.id.text2)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            return StepViewHolder(view)
        }

        override fun onBindViewHolder(holder: StepViewHolder, position: Int) {
            val step = steps[position]
            holder.title.text = "${step.order}. ${step.arrivalTime} → ${step.departureTime} (${step.durationMinutes} min)"
            holder.subtitle.text = "Coût: ${step.cost} € | Effort: ${step.effort}/5 | Dist: %.1f km".format(step.distanceFromPreviousKm)
        }

        override fun getItemCount() = steps.size
    }
}