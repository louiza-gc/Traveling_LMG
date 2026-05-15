package com.example.traveling.TravelShare.Connection

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.traveling.TravelShare.MainActivity
import com.example.traveling.R
import com.example.traveling.TravelPath.Accueil.AccueilPath
import com.example.traveling.TravelShare.Inscription.register_email
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class login : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()

        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        val tvRegisterLink = findViewById<TextView>(R.id.tvRegisterLink)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val btnAnonymous = findViewById<MaterialButton>(R.id.btnAnonymous)

        // Mode invité : choix immédiat (pas d'authentification)
        btnAnonymous.setOnClickListener {
            showChoiceDialog(isGuest = true, userId = null)
        }

        // Connexion classique
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty()) {
                showError("Email obligatoire")
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                showError("Mot de passe obligatoire")
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            btnLogin.isEnabled = false

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    progressBar.visibility = View.GONE
                    btnLogin.isEnabled = true
                    if (task.isSuccessful) {
                        val uid = auth.currentUser?.uid ?: ""
                        showChoiceDialog(isGuest = false, userId = uid)
                    } else {
                        showError("Email et/ou mot de passe incorrect(s)")
                    }
                }
        }

        tvRegisterLink.setOnClickListener {
            startActivity(Intent(this, register_email::class.java))
        }
    }

    private fun showChoiceDialog(isGuest: Boolean, userId: String?) {
        val options = arrayOf("TravelShare", "TravelPath")
        AlertDialog.Builder(this)
            .setTitle("Choisissez votre application")
            .setItems(options) { _, which ->
                val intent = when (which) {
                    0 -> Intent(this, MainActivity::class.java)
                    else -> Intent(this, AccueilPath::class.java)
                }
                intent.putExtra("isGuest", isGuest)
                if (!isGuest && userId != null) {
                    intent.putExtra("userId", userId)
                }
                startActivity(intent)
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showError(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Erreur")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}