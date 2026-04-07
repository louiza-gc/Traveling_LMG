package com.example.traveling.TravelShare.Inscription

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.traveling.R

class register_email : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register_email)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Récupération des vues
        val emailInput = findViewById<EditText>(R.id.etEmail)
        val nextButton = findViewById<Button>(R.id.btnContinue)

        // Action du bouton pour passer à la page suivante
        nextButton.setOnClickListener {
            val email = emailInput.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Veuillez entrer votre email", Toast.LENGTH_SHORT).show()
            } else {
                // Affichage du mail avec Toast
                Toast.makeText(this, "Email saisi : $email", Toast.LENGTH_SHORT).show()
                // Intent vers register_name
                val intent = Intent(this, register_name::class.java)
                intent.putExtra("email", email)
                startActivity(intent)
            }
        }
    }
}