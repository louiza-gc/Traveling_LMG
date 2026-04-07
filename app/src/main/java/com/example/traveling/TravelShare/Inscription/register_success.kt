package com.example.traveling.TravelShare.Inscription

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.traveling.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import androidx.core.net.toUri

class register_success : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register_success)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Récupération des vues
        val ivAvatarSuccess = findViewById<ShapeableImageView>(R.id.ivAvatarSuccess)
        val tvRecap = findViewById<TextView>(R.id.tvRecap)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)

        // Récupération des données passées via Intent
        val email = intent.getStringExtra("email")
        val nom = intent.getStringExtra("nom")
        val prenom = intent.getStringExtra("prenom")
        val phone = intent.getStringExtra("phone")
        val pseudo = intent.getStringExtra("pseudo")
        val country = intent.getStringExtra("country")
        val birthdate = intent.getStringExtra("birthdate")
        val avatarUriString = intent.getStringExtra("avatarUri")

        // Afficher l'avatar si disponible
        if (!avatarUriString.isNullOrEmpty()) {
            val avatarUri = avatarUriString.toUri()
            ivAvatarSuccess.setImageURI(avatarUri)
        }

        // Création du texte récapitulatif
        val recapText = """
            Email: $email
            Nom: $nom
            Prénom: $prenom
            Téléphone: $phone
            Pseudo: $pseudo
            Pays: $country
            Date de naissance: $birthdate
        """.trimIndent()

        tvRecap.text = recapText

        // Bouton Se connecter
        btnLogin.setOnClickListener {
            // ici tu peux ouvrir l'activité de login
            finish()
        }
    }
}