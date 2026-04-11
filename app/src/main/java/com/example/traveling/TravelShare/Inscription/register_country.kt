package com.example.traveling.TravelShare.Inscription

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.traveling.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.util.Locale

class register_country : AppCompatActivity() {

    private lateinit var rvCountries: RecyclerView
    private lateinit var etSearch: TextInputEditText
    private lateinit var btnContinue: MaterialButton
    private lateinit var btnBack: ImageButton

    private lateinit var adapter: CountryAdapter

    private var selectedCountry: String? = null

    private val countryList: List<String> =
        Locale.getISOCountries().map { code ->
            Locale("", code).displayCountry
        }.sorted()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register_country)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        rvCountries = findViewById(R.id.rvCountries)
        etSearch = findViewById(R.id.etSearch)
        btnContinue = findViewById(R.id.btnContinue)
        btnBack = findViewById(R.id.btnBack)

        adapter = CountryAdapter(countryList.toMutableList()) { country ->
            selectedCountry = country
            etSearch.setText(country)
        }

        rvCountries.layoutManager = LinearLayoutManager(this)
        rvCountries.adapter = adapter

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterCountries(s.toString())
            }
        })

        btnContinue.setOnClickListener {

            val email = intent.getStringExtra("email")
            val password = intent.getStringExtra("password")
            val nom = intent.getStringExtra("nom")
            val prenom = intent.getStringExtra("prenom")
            val birthdate = intent.getStringExtra("birthdate")

            if (email.isNullOrEmpty() || password.isNullOrEmpty() ||
                nom.isNullOrEmpty() || prenom.isNullOrEmpty() ||
                birthdate.isNullOrEmpty()
            ) {
                showError("Données utilisateur manquantes")
                return@setOnClickListener
            }

            if (selectedCountry.isNullOrEmpty()) {
                showError("Veuillez sélectionner un pays")
                return@setOnClickListener
            }

            val nextIntent = Intent(this, register_profil::class.java)
            nextIntent.putExtra("country", selectedCountry)
            nextIntent.putExtra("birthdate", birthdate)
            nextIntent.putExtra("email", email)
            nextIntent.putExtra("nom", nom)
            nextIntent.putExtra("prenom", prenom)
            nextIntent.putExtra("password", password)

            startActivity(nextIntent)
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

    private fun showError(message: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Erreur")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}