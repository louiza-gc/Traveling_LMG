package com.example.traveling.TravelPath.Parcours

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.traveling.R
import com.example.traveling.TravelShare.Connection.login
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MyItinerariesActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private val db = FirebaseFirestore.getInstance()
    private var userId: String? = null
    private var itinerariesList = mutableListOf<Itinerary>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_itineraries)

        userId = intent.getStringExtra("userId") ?: FirebaseAuth.getInstance().currentUser?.uid
        if (userId.isNullOrEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Connexion requise")
                .setMessage("Vous devez être connecté pour voir vos parcours.")
                .setPositiveButton("Se connecter") { _, _ ->
                    startActivity(Intent(this, login::class.java))
                    finish()
                }
                .setNegativeButton("Annuler") { _, _ -> finish() }
                .show()
            return
        }

        recycler = findViewById(R.id.recyclerMyItineraries)
        recycler.layoutManager = LinearLayoutManager(this)
        loadItineraries()
    }

    private fun loadItineraries() {
        db.collection("itineraries")
            .whereEqualTo("createdBy", userId)
            .get()
            .addOnSuccessListener { result ->
                itinerariesList = result.documents.mapNotNull { doc ->
                    Itinerary(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        description = doc.getString("description") ?: "",
                        placeIds = (doc.get("placeIds") as? List<String>) ?: emptyList()
                    )
                }.toMutableList()
                if (itinerariesList.isEmpty()) {
                    Toast.makeText(this, "Aucun parcours", Toast.LENGTH_SHORT).show()
                }
                val adapter = ItineraryWithImageAdapter(
                    items = itinerariesList,
                    onItemClick = { itinerary: Itinerary ->
                        val intent = Intent(this, ItineraryDetailActivity::class.java)
                        intent.putExtra("itinerary", itinerary)
                        startActivity(intent)
                    },
                    onDeleteClick = { itinerary: Itinerary, position: Int ->
                        deleteItinerary(itinerary, position)
                    }
                )
                recycler.adapter = adapter
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteItinerary(itinerary: Itinerary, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Supprimer")
            .setMessage("Supprimer \"${itinerary.name}\" ?")
            .setPositiveButton("Oui") { _, _ ->
                db.collection("itineraries").document(itinerary.id).delete()
                    .addOnSuccessListener {
                        itinerariesList.removeAt(position)
                        recycler.adapter?.notifyItemRemoved(position)
                        Toast.makeText(this, "Parcours supprimé", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Non", null).show()
    }
}