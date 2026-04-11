package com.example.traveling.TravelShare.Inscription

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.traveling.MainActivity
import com.example.traveling.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import java.io.File

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

        val ivAvatarSuccess = findViewById<ShapeableImageView>(R.id.ivAvatarSuccess)
        val tvRecap = findViewById<TextView>(R.id.tvRecap)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)

        val email = intent.getStringExtra("email")
        val nom = intent.getStringExtra("nom")
        val prenom = intent.getStringExtra("prenom")
        val phone = intent.getStringExtra("phone")
        val pseudo = intent.getStringExtra("pseudo")
        val country = intent.getStringExtra("country")
        val birthdate = intent.getStringExtra("birthdate")
        val avatarUriString = intent.getStringExtra("avatarPath")

        if (!avatarUriString.isNullOrEmpty()) {
            val file = File(avatarUriString)
            ivAvatarSuccess.setImageURI(android.net.Uri.fromFile(file))
        }

        tvRecap.text = """
            Email: $email
            Nom: $nom
            Prénom: $prenom
            Téléphone: $phone
            Pseudo: $pseudo
            Pays: $country
            Date de naissance: $birthdate
        """.trimIndent()

        btnLogin.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}