package com.example.traveling.TravelPath.Parcours

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.traveling.R
import com.example.traveling.TravelPath.Accueil.Place
import com.example.traveling.TravelPath.Accueil.PlaceRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ItineraryDetailActivity : AppCompatActivity() {

    private lateinit var btnAddPlace: Button
    private lateinit var btnExportPdf: Button
    private lateinit var btnViewOnMap: Button
    private lateinit var btnSaveItinerary: Button
    private lateinit var recycler: RecyclerView
    private lateinit var tvName: TextView
    private lateinit var tvDesc: TextView
    private lateinit var tvCost: TextView
    private lateinit var tvDuration: TextView
    private lateinit var tvEffort: TextView

    private var itinerary: Itinerary? = null
    private var allPlaces: List<Place> = emptyList()
    private var currentPlaces: MutableList<Place> = mutableListOf()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_itinerary_detail_parcours)

        itinerary = intent.getSerializableExtra("itinerary") as? Itinerary
        if (itinerary == null) {
            Toast.makeText(this, "Itinéraire introuvable", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        userId = auth.currentUser?.uid
        if (userId.isNullOrEmpty()) {
            Toast.makeText(this, "Connectez-vous pour modifier", Toast.LENGTH_SHORT).show()
        }

        tvName = findViewById(R.id.tv_itinerary_name)
        tvDesc = findViewById(R.id.tv_itinerary_description)
        tvCost = findViewById(R.id.tv_total_cost)
        tvDuration = findViewById(R.id.tv_total_duration)
        tvEffort = findViewById(R.id.tv_average_effort)
        recycler = findViewById(R.id.recycler_places_in_itinerary)
        btnAddPlace = findViewById(R.id.btn_add_place)
        btnExportPdf = findViewById(R.id.btn_export_pdf)
        btnViewOnMap = findViewById(R.id.btn_view_on_map)
        btnSaveItinerary = findViewById(R.id.btn_save_itinerary)

        recycler.layoutManager = LinearLayoutManager(this)

        loadPlacesAndDisplay()

        btnAddPlace.setOnClickListener {
            if (userId.isNullOrEmpty()) {
                Toast.makeText(this, "Connectez-vous pour ajouter des lieux", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showAddPlaceDialog()
        }

        btnExportPdf.setOnClickListener { exportToPdf() }
        btnViewOnMap.setOnClickListener {
            val intent = Intent(this, ItineraryMapActivity::class.java)
            intent.putExtra("itinerary", itinerary)
            startActivity(intent)
        }
        btnSaveItinerary.setOnClickListener { saveItineraryToFirestore() }
    }

    private fun loadPlacesAndDisplay() {
        lifecycleScope.launch {
            allPlaces = PlaceRepository.loadAllPlaces()
            currentPlaces = allPlaces.filter { it.id in itinerary!!.placeIds }.toMutableList()
            updateDisplay()
        }
    }

    private fun updateDisplay() {
        if (itinerary == null) return

        val totalCost = currentPlaces.sumOf { it.details.costEstimate.adult }
        val totalDuration = currentPlaces.sumOf { it.details.typicalDurationMinutes }
        val avgEffort = if (currentPlaces.isNotEmpty()) currentPlaces.map { it.details.effortLevel }.average() else 0.0

        tvName.text = itinerary!!.name
        tvDesc.text = itinerary!!.description
        tvCost.text = "Coût total : %.2f €".format(totalCost)
        tvDuration.text = "Durée totale : ${totalDuration} min"
        tvEffort.text = "Effort moyen : %.1f/5".format(avgEffort)

        recycler.adapter = PlacesInItineraryAdapter(currentPlaces)
    }

    private fun showAddPlaceDialog() {
        val availablePlaces = allPlaces.filter { it.id !in currentPlaces.map { p -> p.id } }
        if (availablePlaces.isEmpty()) {
            Toast.makeText(this, "Tous les lieux sont déjà dans le parcours", Toast.LENGTH_SHORT).show()
            return
        }
        val names = availablePlaces.map { it.name }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Ajouter un lieu")
            .setItems(names) { _, which ->
                val selectedPlace = availablePlaces[which]
                addPlaceToItinerary(selectedPlace.id, selectedPlace.name)
            }
            .show()
    }

    private fun addPlaceToItinerary(placeId: String, placeName: String) {
        if (userId.isNullOrEmpty()) return
        val newPlace = allPlaces.find { it.id == placeId } ?: return
        currentPlaces.add(newPlace)
        updateItineraryOrder()
        Toast.makeText(this, "Lieu ajouté : $placeName", Toast.LENGTH_SHORT).show()
    }

    private fun removePlaceAtIndex(index: Int) {
        if (index < 0 || index >= currentPlaces.size) return
        val removed = currentPlaces.removeAt(index)
        updateItineraryOrder()
        Toast.makeText(this, "Lieu supprimé : ${removed.name}", Toast.LENGTH_SHORT).show()
    }

    private fun movePlaceUp(index: Int) {
        if (index <= 0) return
        Collections.swap(currentPlaces, index, index - 1)
        updateItineraryOrder()
    }

    private fun movePlaceDown(index: Int) {
        if (index >= currentPlaces.size - 1) return
        Collections.swap(currentPlaces, index, index + 1)
        updateItineraryOrder()
    }

    private fun updateItineraryOrder() {
        // Mettre à jour l'ordre dans Firestore
        val newPlaceIds = currentPlaces.map { it.id }
        if (userId.isNullOrEmpty()) return
        db.collection("itineraries").document(itinerary!!.id)
            .update("placeIds", newPlaceIds)
            .addOnSuccessListener {
                itinerary = itinerary!!.copy(placeIds = newPlaceIds)
                updateDisplay()
                Toast.makeText(this, "Ordre mis à jour", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveItineraryToFirestore() {
        if (itinerary == null) return
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Connectez-vous pour sauvegarder", Toast.LENGTH_SHORT).show()
            return
        }
        if (currentPlaces.isEmpty()) {
            Toast.makeText(this, "Aucun lieu à sauvegarder", Toast.LENGTH_SHORT).show()
            return
        }
        val data = hashMapOf(
            "name" to itinerary!!.name,
            "description" to itinerary!!.description,
            "placeIds" to currentPlaces.map { it.id },
            "createdBy" to uid,
            "createdAt" to System.currentTimeMillis(),
            "totalCost" to currentPlaces.sumOf { it.details.costEstimate.adult },
            "totalDurationMinutes" to currentPlaces.sumOf { it.details.typicalDurationMinutes },
            "averageEffort" to (if (currentPlaces.isNotEmpty()) currentPlaces.map { it.details.effortLevel }.average() else 0.0)
        )
        db.collection("itineraries").add(data)
            .addOnSuccessListener {
                Toast.makeText(this, "Parcours sauvegardé !", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erreur : ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // ==================== ADAPTATEUR POUR LES LIEUX AVEC BOUTONS ====================

    private inner class PlacesInItineraryAdapter(private val places: List<Place>) :
        RecyclerView.Adapter<PlacesInItineraryAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvName: TextView = itemView.findViewById(R.id.tv_place_name)
            val tvDetails: TextView = itemView.findViewById(R.id.tv_place_details)
            val btnMoveUp: ImageButton = itemView.findViewById(R.id.btn_move_up)
            val btnMoveDown: ImageButton = itemView.findViewById(R.id.btn_move_down)
            val btnRemove: ImageButton = itemView.findViewById(R.id.btn_remove_place)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_place_in_itinerary, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val place = places[position]
            holder.tvName.text = place.name
            holder.tvDetails.text = "${place.location.city} - Coût: ${place.details.costEstimate.adult}€ - Effort: ${place.details.effortLevel}/5 - Durée: ${place.details.typicalDurationMinutes} min"
            holder.btnMoveUp.setOnClickListener { movePlaceUp(position) }
            holder.btnMoveDown.setOnClickListener { movePlaceDown(position) }
            holder.btnRemove.setOnClickListener { removePlaceAtIndex(position) }
        }

        override fun getItemCount() = places.size
    }


    private fun exportToPdf() {
        if (itinerary == null) return
        val places = allPlaces.filter { it.id in itinerary!!.placeIds }
        if (places.isEmpty()) {
            Toast.makeText(this, "Aucun lieu à exporter", Toast.LENGTH_SHORT).show()
            return
        }

        val pdfDocument = PdfDocument()
        val pageWidth = 595   // A4 portrait (points)
        val pageHeight = 842
        val leftMargin = 40f
        var pageNumber = 1
        var y = 40f
        var page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas

        // Charger le logo
        val logoBitmap = getBitmapFromDrawable(R.drawable.ic_travelpath_logo)
        val logoWidth = 50
        val logoHeight = 50

        // Polices
        val titlePaint = Paint().apply {
            textSize = 22f
            isFakeBoldText = true
            color = android.graphics.Color.parseColor("#00A8A8")
        }
        val headerPaint = Paint().apply {
            textSize = 12f
            color = android.graphics.Color.GRAY
        }
        val normalPaint = Paint().apply {
            textSize = 11f
            color = android.graphics.Color.BLACK
        }
        val boldPaint = Paint().apply {
            textSize = 12f
            isFakeBoldText = true
            color = android.graphics.Color.BLACK
        }
        val sectionPaint = Paint().apply {
            textSize = 14f
            isFakeBoldText = true
            color = android.graphics.Color.parseColor("#00A8A8")
        }

        fun drawText(text: String, x: Float = leftMargin, paint: Paint = normalPaint, lineHeight: Float = paint.textSize + 4) {
            canvas.drawText(text, x, y, paint)
            y += lineHeight
        }

        fun drawLine(yPos: Float, width: Float = pageWidth - 2 * leftMargin) {
            val linePaint = Paint().apply { color = android.graphics.Color.LTGRAY; strokeWidth = 1f }
            canvas.drawLine(leftMargin, yPos, leftMargin + width, yPos, linePaint)
        }

        fun finishPageAndCreateNew() {
            pdfDocument.finishPage(page)
            pageNumber++
            page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            canvas = page.canvas
            y = 40f
            // Réafficher le logo et l'en-tête léger sur les pages suivantes
            canvas.drawBitmap(logoBitmap, null, android.graphics.RectF(leftMargin, 20f, leftMargin + logoWidth, 20f + logoHeight), null)
            drawText("TravelPath - Suite", leftMargin, headerPaint, 20f)
            drawLine(y - 8f)
            y += 12f
        }

        // --- Page 1 : Logo et titre principal ---
        canvas.drawBitmap(logoBitmap, null, android.graphics.RectF(leftMargin, 20f, leftMargin + logoWidth, 20f + logoHeight), null)
        drawText("TravelPath", leftMargin, titlePaint, 28f)
        drawText("Récapitulatif de votre parcours", leftMargin, headerPaint, 18f)
        y += 8f
        drawLine(y - 8f)
        y += 12f

        // --- Informations générales ---
        drawText("Nom du parcours", leftMargin, boldPaint)
        drawText(itinerary!!.name, leftMargin, normalPaint)
        y += 4f
        drawText("Description", leftMargin, boldPaint)
        drawText(itinerary!!.description, leftMargin, normalPaint)
        y += 8f

        val totalCost = places.sumOf { it.details.costEstimate.adult }
        val totalDuration = places.sumOf { it.details.typicalDurationMinutes }
        val avgEffort = if (places.isNotEmpty()) places.map { it.details.effortLevel }.average() else 0.0

        drawText("💰 Coût total : %.2f €".format(totalCost), leftMargin, boldPaint)
        drawText("⏱️ Durée totale : ${totalDuration} min", leftMargin, boldPaint)
        drawText("💪 Effort moyen : %.1f/5".format(avgEffort), leftMargin, boldPaint)
        y += 12f
        drawLine(y - 8f)
        y += 12f

        // --- Liste des étapes ---
        drawText("📌 Étapes du parcours", leftMargin, sectionPaint, 20f)
        y += 8f

        for ((index, place) in places.withIndex()) {
            val line1 = "${index + 1}. ${place.name} — ${place.location.city}"
            drawText(line1, leftMargin, boldPaint)
            val line2 = "   Coût: %.2f € | Effort: ${place.details.effortLevel}/5 | Durée: ${place.details.typicalDurationMinutes} min".format(place.details.costEstimate.adult)
            drawText(line2, leftMargin, normalPaint)
            if (place.details.tags.isNotEmpty()) {
                drawText("   Tags: ${place.details.tags.joinToString(", ")}", leftMargin, normalPaint)
            }
            y += 4f
            drawLine(y - 4f, pageWidth - 2 * leftMargin)
            y += 8f

            if (y > pageHeight - 60) {
                finishPageAndCreateNew()
            }
        }

        // Pied de page (dernière page)
        val footerPaint = Paint().apply { textSize = 8f; color = android.graphics.Color.GRAY }
        val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("Document généré le $date", leftMargin,
            (pageHeight - 20).toFloat(), footerPaint)

        pdfDocument.finishPage(page)

        // Sauvegarde et partage
        val file = File(cacheDir, "itinerary_${itinerary!!.id}.pdf")
        try {
            FileOutputStream(file).use { out -> pdfDocument.writeTo(out) }
            pdfDocument.close()
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Exporter le parcours"))
        } catch (e: Exception) {
            Toast.makeText(this, "Erreur PDF: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun getBitmapFromDrawable(drawableId: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(this, drawableId)!!
        val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    // ==================== ADAPTATEUR POUR LES ÉTAPES ====================

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
            val place = allPlaces.find { it.id == step.placeId }
            val name = place?.name ?: step.placeId
            holder.title.text = "${step.order}. $name"
            holder.subtitle.text = "Coût: ${step.cost} € | Effort: ${step.effort}/5 | Durée: ${step.durationMinutes} min"
        }
        override fun getItemCount() = steps.size
    }
}