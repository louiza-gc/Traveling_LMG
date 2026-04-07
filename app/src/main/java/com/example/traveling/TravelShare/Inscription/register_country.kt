package com.example.traveling.TravelShare.Inscription

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.traveling.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class register_country : AppCompatActivity() {

    private lateinit var rvCountries: RecyclerView
    private lateinit var etSearch: TextInputEditText
    private lateinit var btnContinue: MaterialButton
    private lateinit var btnBack: ImageButton

    private lateinit var adapter: CountryAdapter

    private var selectedCountry: String = ""

    private val countryList = listOf(
        "France", "Algeria", "Morocco", "Tunisia", "Germany",
        "Spain", "Italy", "United Kingdom", "United States",
        "Canada", "Brazil", "Japan", "China", "India", "Australia"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register_country)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialisation
        rvCountries = findViewById(R.id.rvCountries)
        etSearch = findViewById(R.id.etSearch)
        btnContinue = findViewById(R.id.btnContinue)
        btnBack = findViewById(R.id.btnBack)

        // RecyclerView setup
        adapter = CountryAdapter(countryList.toMutableList()) { country ->
            selectedCountry = country
            etSearch.setText(country)
        }

        rvCountries.layoutManager = LinearLayoutManager(this)
        rvCountries.adapter = adapter

        // Recherche
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterCountries(s.toString())
            }
        })

        // Récupérer tous les champs précédents
        val email = intent.getStringExtra("email")
        val nom = intent.getStringExtra("nom")
        val prenom = intent.getStringExtra("prenom")
        val birthdate = intent.getStringExtra("birthdate")

        // Bouton Continuer
        btnContinue.setOnClickListener {
            if (selectedCountry.isEmpty()) {
                Toast.makeText(this, "Veuillez sélectionner un pays", Toast.LENGTH_SHORT).show()
            } else {

                val intent = Intent(this, register_profil::class.java)
                intent.putExtra("country", selectedCountry)
                intent.putExtra("birthdate", birthdate)
                intent.putExtra("email", email)
                intent.putExtra("nom", nom)
                intent.putExtra("prenom", prenom)

                startActivity(intent)
            }
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun filterCountries(query: String) {
        val filtered = countryList.filter {
            it.lowercase().contains(query.lowercase())
        }
        adapter.updateList(filtered)
    }
}