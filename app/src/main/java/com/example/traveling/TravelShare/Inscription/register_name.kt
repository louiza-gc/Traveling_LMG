package com.example.traveling.TravelShare.Inscription

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.traveling.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class register_name : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register_name)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etNom = findViewById<TextInputEditText>(R.id.etNom)
        val etPrenom = findViewById<TextInputEditText>(R.id.etPrenom)
        val btnContinue = findViewById<MaterialButton>(R.id.btnContinue)

        btnContinue.setOnClickListener {

            val nom = etNom.text.toString().trim()
            val prenom = etPrenom.text.toString().trim()

            val email = intent.getStringExtra("email")
            val password = intent.getStringExtra("password")

            if (nom.isEmpty()) {
                showError("Veuillez entrer votre nom")
                return@setOnClickListener
            }

            if (prenom.isEmpty()) {
                showError("Veuillez entrer votre prénom")
                return@setOnClickListener
            }

            if (email.isNullOrEmpty() || password.isNullOrEmpty()) {
                showError("Erreur : données email/password manquantes")
                return@setOnClickListener
            }

            val intent = Intent(this, register_birthdate::class.java)
            intent.putExtra("email", email)
            intent.putExtra("nom", nom)
            intent.putExtra("prenom", prenom)
            intent.putExtra("password", password)

            startActivity(intent)
        }
    }

    private fun showError(message: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Erreur")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}