package com.example.traveling.TravelShare.Inscription

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
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
        // Récupérer l'email envoyé depuis register_email
        val email = intent.getStringExtra("email")
        if (email != null) {
            Toast.makeText(this, "Email reçu : $email", Toast.LENGTH_SHORT).show()
        }

        // Récupération des champs et bouton
        val etNom = findViewById<TextInputEditText>(R.id.etNom)
        val etPrenom = findViewById<TextInputEditText>(R.id.etPrenom)
        val btnContinue = findViewById<MaterialButton>(R.id.btnContinue)

        // Exemple d'action sur le bouton Continuer
        btnContinue.setOnClickListener {
            val nom = etNom.text.toString().trim()
            val prenom = etPrenom.text.toString().trim()

            if (nom.isEmpty() || prenom.isEmpty()) {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Nom : $nom\nPrénom : $prenom", Toast.LENGTH_SHORT).show()
                // Intent vers register_birthdate
                val intent = Intent(this, register_birthdate::class.java)
                intent.putExtra("email", email)
                intent.putExtra("nom", nom)
                intent.putExtra("prenom", prenom)

                startActivity(intent)
            }
        }
    }
}