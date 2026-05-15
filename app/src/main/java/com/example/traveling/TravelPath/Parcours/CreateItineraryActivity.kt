package com.example.traveling.TravelPath.Parcours

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.traveling.R
import com.example.traveling.TravelPath.Accueil.Place
import com.example.traveling.TravelPath.Accueil.PlaceRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreateItineraryActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etDescription: EditText
    private lateinit var recycler: RecyclerView
    private lateinit var btnSave: Button
    private lateinit var progressBar: ProgressBar

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var allPlaces: List<Place> = emptyList()
    private var selectedPlaceIds = mutableSetOf<String>()
    private lateinit var adapter: PlacesSelectionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_itinerary)

        etName = findViewById(R.id.etItineraryName)
        etDescription = findViewById(R.id.etItineraryDescription)
        recycler = findViewById(R.id.recyclerPlacesForItinerary)
        btnSave = findViewById(R.id.btnSaveItinerary)
        progressBar = findViewById(R.id.progressBarItinerary)

        recycler.layoutManager = LinearLayoutManager(this)
        loadPlaces()

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val desc = etDescription.text.toString().trim()
            val uid = auth.currentUser?.uid
            if (uid == null) {
                Toast.makeText(this, "Connectez-vous pour sauvegarder", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (name.isEmpty()) {
                showError("Donnez un nom au parcours")
                return@setOnClickListener
            }
            if (selectedPlaceIds.isEmpty()) {
                showError("Sélectionnez au moins un lieu")
                return@setOnClickListener
            }
            saveItinerary(name, desc, uid)
        }
    }

    private fun loadPlaces() {
        progressBar.visibility = ProgressBar.VISIBLE
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    PlaceRepository.init(applicationContext)
                    allPlaces = PlaceRepository.loadAllPlaces()
                }
                adapter = PlacesSelectionAdapter(allPlaces, selectedPlaceIds) { placeId, isSelected ->
                    if (isSelected) selectedPlaceIds.add(placeId)
                    else selectedPlaceIds.remove(placeId)
                }
                recycler.adapter = adapter
            } catch (e: Exception) {
                showError("Erreur chargement lieux")
            } finally {
                progressBar.visibility = ProgressBar.GONE
            }
        }
    }

    private fun saveItinerary(name: String, description: String, uid: String) {
        progressBar.visibility = ProgressBar.VISIBLE
        val data = hashMapOf(
            "name" to name,
            "description" to description,
            "placeIds" to selectedPlaceIds.toList(),
            "createdBy" to uid,
            "createdAt" to System.currentTimeMillis()
        )
        db.collection("itineraries").add(data)
            .addOnSuccessListener {
                Toast.makeText(this, "Parcours enregistré !", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_OK)
                finish()
            }
            .addOnFailureListener { e ->
                showError("Erreur Firestore : ${e.message}")
                progressBar.visibility = ProgressBar.GONE
            }
    }

    private fun showError(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Erreur")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}