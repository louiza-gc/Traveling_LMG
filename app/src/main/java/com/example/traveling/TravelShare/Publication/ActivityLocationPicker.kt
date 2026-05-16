package com.example.traveling.TravelShare.Publication

import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.example.traveling.R
import java.util.Locale

class ActivityLocationPicker : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var selectedMarker: Marker? = null
    private var selectedLatLng: LatLng? = null
    private lateinit var tvAddress: TextView
    private lateinit var btnValidate: Button
    private lateinit var etSearchLocation: EditText
    private lateinit var btnSearch: ImageButton

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
        etSearchLocation = findViewById(R.id.etSearchLocation)
        btnSearch = findViewById(R.id.btnSearch)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Barre de recherche
        btnSearch.setOnClickListener {
            val query = etSearchLocation.text.toString().trim()
            if (query.isNotEmpty()) {
                searchLocation(query)
            } else {
                Toast.makeText(this, "🔎 Entrez un lieu à rechercher", Toast.LENGTH_SHORT).show()
            }
        }

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
                val defaultLocation = LatLng(48.8566, 2.3522)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f))
            }
        }

        // Gérer le clic long pour sélectionner un lieu
        mMap.setOnMapLongClickListener { latLng ->
            selectLocation(latLng)
        }
    }

    //  Recherche d'un lieu par adresse
    private fun searchLocation(query: String) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocationName(query, 1)

            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val latLng = LatLng(address.latitude, address.longitude)

                // Centrer la caméra sur le lieu trouvé
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))

                // Sélectionner automatiquement ce lieu
                selectLocation(latLng)

                Toast.makeText(this, address.getAddressLine(0), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "🔍 Lieu non trouvé", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Erreur de recherche", Toast.LENGTH_SHORT).show()
        }
    }

    private fun selectLocation(latLng: LatLng) {
        selectedMarker?.remove()

        selectedMarker = mMap.addMarker(
            MarkerOptions()
                .position(latLng)
                .title("Lieu sélectionné")
                .draggable(true)
        )

        selectedLatLng = latLng
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        getAddressFromLatLng(latLng)

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