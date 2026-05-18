package com.example.traveling.TravelPath.Parcours

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.traveling.R
import com.example.traveling.TravelPath.Accueil.Place
import com.example.traveling.TravelPath.Accueil.PlaceRepository
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlin.math.*

class ItineraryMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var itinerary: Itinerary? = null
    private var places: List<Place> = emptyList()
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_itinerary_map)

        val params = window.attributes
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        params.width = (screenWidth * 0.9).toInt()
        params.height = (screenHeight * 0.7).toInt()
        window.attributes = params
        itinerary = intent.getSerializableExtra("itinerary") as? Itinerary
        if (itinerary == null || itinerary!!.placeIds.isEmpty()) {
            finish()
            return
        }
        setTitle(itinerary!!.name)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        loadPlacesAndDrawRoute()
    }

    private fun loadPlacesAndDrawRoute() {
        lifecycleScope.launch {
            try {
                val allPlaces = PlaceRepository.loadAllPlaces()
                places = allPlaces.filter { it.id in itinerary!!.placeIds }
                if (places.isNotEmpty()) {
                    addMarkersAndZoom()
                    drawRouteBetweenPoints()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun addMarkersAndZoom() {
        val builder = LatLngBounds.Builder()
        places.forEachIndexed { index, place ->
            val latLng = LatLng(place.location.lat, place.location.lon)
            builder.include(latLng)

            // Déterminer la couleur de fond du numéro selon le début/fin
            val markerIcon = createCustomMarker(index + 1, place.name)
            val markerOptions = MarkerOptions()
                .position(latLng)
                .title(place.name)
                .snippet("${place.location.city}, ${place.location.country}")
                .icon(markerIcon)
                .anchor(0.5f, 1f) // pour centrer la pointe du marqueur sur la position
            mMap.addMarker(markerOptions)
        }
        val bounds = builder.build()
        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 100)
        mMap.animateCamera(cameraUpdate)
    }

    private fun drawRouteBetweenPoints() {
        if (places.size < 2) return
        val points = places.map { LatLng(it.location.lat, it.location.lon) }
        for (i in 0 until points.size - 1) {
            fetchAndDrawRoute(points[i], points[i + 1])
        }
    }

    private fun fetchAndDrawRoute(origin: LatLng, destination: LatLng) {
        // Récupérez votre clé API depuis les ressources ou un fichier de configuration
        val apiKey = getString(R.string.openrouteservice_api_key)
        val url = "https://api.openrouteservice.org/v2/directions/driving-car?api_key=$apiKey&start=${origin.longitude},${origin.latitude}&end=${destination.longitude},${destination.latitude}"

        lifecycleScope.launch(Dispatchers.IO) {
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()

            try {
                val response = client.newCall(request).execute()
                val jsonString = response.body?.string()
                if (response.isSuccessful && jsonString != null) {
                    val decodedPath = decodePolylineFromJson(jsonString)
                    withContext(Dispatchers.Main) {
                        val polylineOptions = PolylineOptions()
                            .addAll(decodedPath)
                            .color(Color.parseColor("#00A8A8"))
                            .width(6f)
                        mMap.addPolyline(polylineOptions)
                    }
                } else {
                    // Gérer l'erreur
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    private fun decodePolylineFromJson(json: String): List<LatLng> {
        val points = mutableListOf<LatLng>()
        try {
            val jsonObject = JSONObject(json)
            val routes = jsonObject.getJSONArray("routes")
            if (routes.length() > 0) {
                val geometry = routes.getJSONObject(0).getString("geometry")
                // geometry est la polyligne encodée (Google format)
                points.addAll(decodePolyline(geometry))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return points
    }
    private fun createCustomMarker(stepNumber: Int, placeName: String): BitmapDescriptor {
        val markerView = layoutInflater.inflate(R.layout.marker_custom, null)
        val tvNumber = markerView.findViewById<TextView>(R.id.marker_number)
        val tvName = markerView.findViewById<TextView>(R.id.marker_name)
        tvNumber.text = stepNumber.toString()
        tvName.text = placeName
        markerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        markerView.layout(0, 0, markerView.measuredWidth, markerView.measuredHeight)
        val bitmap = Bitmap.createBitmap(markerView.measuredWidth, markerView.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        markerView.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = mutableListOf<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat
            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng
            val p = LatLng(lat / 1E5, lng / 1E5)
            poly.add(p)
        }
        return poly
    }
}