package com.example.traveling.TravelPath.Parcours

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.traveling.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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
        findViewById<TextView>(R.id.detail_cost).text = "Coût total : %.2f €".format(itinerary.totalCost)
        findViewById<TextView>(R.id.detail_duration).text = "Durée totale : ${itinerary.totalDurationMinutes} min"
        findViewById<TextView>(R.id.detail_effort).text = "Effort moyen : %.1f/5".format(itinerary.averageEffort)

        val recyclerSteps = findViewById<RecyclerView>(R.id.recyclerSteps)
        recyclerSteps.layoutManager = LinearLayoutManager(this)
        recyclerSteps.adapter = StepsAdapter(itinerary.steps)

        val btnSave = findViewById<Button>(R.id.btn_save_itinerary)
        btnSave.setOnClickListener {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                Toast.makeText(this, "Connectez-vous pour sauvegarder", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val data = hashMapOf(
                "name" to itinerary.name,
                "description" to itinerary.description,
                "placeIds" to itinerary.steps.map { it.placeId },
                "totalCost" to itinerary.totalCost,
                "totalDurationMinutes" to itinerary.totalDurationMinutes,
                "averageEffort" to itinerary.averageEffort,
                "createdBy" to uid,
                "createdAt" to System.currentTimeMillis()
            )
            FirebaseFirestore.getInstance().collection("itineraries")
                .add(data)
                .addOnSuccessListener {
                    Toast.makeText(this, "Parcours sauvegardé", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
                }
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
            holder.title.text = "${step.order}. ${step.arrivalTime} → ${step.departureTime} (${step.durationMinutes} min)"
            holder.subtitle.text = "Coût: ${step.cost} € | Effort: ${step.effort}/5 | Dist: %.1f km".format(step.distanceFromPreviousKm)
        }

        override fun getItemCount() = steps.size
    }
}