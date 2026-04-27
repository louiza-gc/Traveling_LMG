package com.example.traveling.TravelPath.Accueil

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.traveling.R
import com.google.gson.Gson

class PlaceDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_place_detail)

        // Récupération du JSON passé par l'intent
        val placeJson = intent.getStringExtra("place_json")
        if (placeJson.isNullOrEmpty()) {
            Toast.makeText(this, "Erreur : aucun lieu reçu", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Désérialisation du JSON en objet Place
        val place = try {
            Gson().fromJson(placeJson, Place::class.java)
        } catch (e: Exception) {
            Toast.makeText(this, "Erreur de lecture des données", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Remplissage des vues
        findViewById<TextView>(R.id.place_name).text = place.name
        findViewById<TextView>(R.id.place_description).text = place.details.description

        // Coût avec gestion null
        val costAdult = place.details.costEstimate?.adult ?: 0.0
        findViewById<TextView>(R.id.place_cost).text = "Coût : %.2f €".format(costAdult)

        // Effort
        val effort = place.details.effortLevel
        findViewById<TextView>(R.id.place_effort).text = "Effort : $effort/5"

        // Durée
        findViewById<TextView>(R.id.place_duration).text = "Durée : ${place.details.typicalDurationMinutes} min"

        // Adresse
        findViewById<TextView>(R.id.place_address).text = "Adresse : ${place.location.address}"

        // Tags
        if (place.details.tags.isNotEmpty()) {
            findViewById<TextView>(R.id.place_tags).text = "Tags : ${place.details.tags.joinToString(", ")}"
        } else {
            findViewById<TextView>(R.id.place_tags).visibility = android.view.View.GONE
        }

        // Image
        val imageView = findViewById<ImageView>(R.id.place_image)
        val thumbnail = place.media.thumbnail
        if (thumbnail.isNotBlank()) {
            Glide.with(this)
                .load(thumbnail)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .into(imageView)
        } else {
            imageView.setImageResource(android.R.drawable.ic_menu_gallery)
        }
    }
    private fun showPlaceDialog(place: Place) {
        // Gonfler le layout de l'activité détail (devenu le contenu du dialog)
        val dialogView = layoutInflater.inflate(R.layout.activity_place_detail, null)

        // Remplir les vues (les ids sont ceux de votre layout)
        dialogView.findViewById<TextView>(R.id.place_name).text = place.name
        dialogView.findViewById<TextView>(R.id.place_description).text = place.details.description
        dialogView.findViewById<TextView>(R.id.place_cost).text = "Coût : %.2f €".format(place.details.costEstimate.adult)
        dialogView.findViewById<TextView>(R.id.place_effort).text = "Effort : ${place.details.effortLevel}/5"
        dialogView.findViewById<TextView>(R.id.place_duration).text = "Durée : ${place.details.typicalDurationMinutes} min"
        dialogView.findViewById<TextView>(R.id.place_address).text = "Adresse : ${place.location.address}"

        val tagsText = if (place.details.tags.isNotEmpty()) "Tags : ${place.details.tags.joinToString(", ")}" else ""
        dialogView.findViewById<TextView>(R.id.place_tags).text = tagsText

        // Image
        val imageView = dialogView.findViewById<ImageView>(R.id.place_image)
        if (place.media.thumbnail.isNotBlank()) {
            Glide.with(this).load(place.media.thumbnail).into(imageView)
        } else {
            imageView.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        // Créer et afficher le dialog
        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Fermer", null)
            .show()
    }
}