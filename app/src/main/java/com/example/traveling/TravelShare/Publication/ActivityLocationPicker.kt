package com.example.traveling.TravelShare.Publication

import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.example.traveling.R
import com.google.android.gms.location.LocationServices
import java.util.Locale

class ActivityLocationPicker : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var selectedMarker: Marker? = null
    private var selectedLatLng: LatLng? = null
    private lateinit var tvAddress: TextView
    private lateinit var btnValidate: Button

    companion object {
        const val EXTRA_LAT = "extra_lat"
        const val EXTRA_LNG = "extra_lng"
        const val EXTRA_ADDRESS = "extra_address"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_picker)

        tvAddress = findViewById(R.id.tvAddress)
        btnValidate = findViewById(R.id.btnValidate)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        btnValidate.setOnClickListener {
            if (selectedLatLng != null) {
                val intent = intent.apply {
                    putExtra(EXTRA_LAT, selectedLatLng!!.latitude)
                    putExtra(EXTRA_LNG, selectedLatLng!!.longitude)
                    putExtra(EXTRA_ADDRESS, tvAddress.text.toString())
                }
                setResult(RESULT_OK, intent)
                finish()
            } else {
                Toast.makeText(this, "Veuillez sélectionner un lieu sur la carte", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Vérifier les permissions
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        mMap.isMyLocationEnabled = true

        // Récupérer la localisation actuelle
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val userLatLng = LatLng(location.latitude, location.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 14f))
            } else {
                // Position par défaut (Paris)
                val defaultLocation = LatLng(48.8566, 2.3522)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f))
            }
        }

        // Gérer le clic long pour sélectionner un lieu
        mMap.setOnMapLongClickListener { latLng ->
            selectLocation(latLng)
        }
    }

    private fun selectLocation(latLng: LatLng) {
        // Supprimer l'ancien marqueur
        selectedMarker?.remove()

        // Ajouter un nouveau marqueur
        selectedMarker = mMap.addMarker(
            MarkerOptions()
                .position(latLng)
                .title("Lieu sélectionné")
                .draggable(true)  // Permettre de déplacer le marqueur
        )

        selectedLatLng = latLng

        // Centrer la caméra sur le marqueur
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))

        // Convertir les coordonnées en adresse
        getAddressFromLatLng(latLng)

        // Rendre le marqueur déplaçable
        mMap.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
            override fun onMarkerDragStart(marker: Marker) {}

            override fun onMarkerDrag(marker: Marker) {}

            override fun onMarkerDragEnd(marker: Marker) {
                selectedLatLng = marker.position
                getAddressFromLatLng(marker.position)
            }
        })
    }

    private fun getAddressFromLatLng(latLng: LatLng) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)

            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0].getAddressLine(0)
                tvAddress.text = address ?: "Adresse non trouvée"
            } else {
                tvAddress.text = "${latLng.latitude}, ${latLng.longitude}"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            tvAddress.text = "${latLng.latitude}, ${latLng.longitude}"
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission accordée, recharger la carte
                    val mapFragment = supportFragmentManager
                        .findFragmentById(R.id.map) as SupportMapFragment
                    mapFragment.getMapAsync(this)
                } else {
                    Toast.makeText(this, "Permission refusée", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}