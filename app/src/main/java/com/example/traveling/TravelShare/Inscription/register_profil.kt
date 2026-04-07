package com.example.traveling.TravelShare.Inscription

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.traveling.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class register_profil : AppCompatActivity() {
    private val PICK_IMAGE_REQUEST = 100
    private lateinit var ivAvatar: ImageView
    private var avatarUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register_profil)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Récupérer tous les champs précédents
        val email = intent.getStringExtra("email")
        val nom = intent.getStringExtra("nom")
        val prenom = intent.getStringExtra("prenom")
        val birthdate = intent.getStringExtra("birthdate")
        val country = intent.getStringExtra("country")

        if (email != null && nom != null && prenom != null) {
            Toast.makeText(this, "Email: $email\nNom: $nom\nPrénom: $prenom", Toast.LENGTH_SHORT).show()
        }

        // Récupérer les champs téléphone et pseudo
        val etPhone = findViewById<EditText>(R.id.etPhone)
        val etPseudo = findViewById<TextInputEditText>(R.id.etPseudo)
        val btnValidate = findViewById<MaterialButton>(R.id.btnValidate)

        // Récupération ImageView avatar
        ivAvatar = findViewById(R.id.ivAvatar)
        val flAvatar = findViewById<FrameLayout>(R.id.flAvatar)

        // Cliquer sur l'avatar pour changer l'image
        flAvatar.setOnClickListener {
            openGallery()
        }

        btnValidate.setOnClickListener {
            val phone = etPhone.text.toString().trim()
            val pseudo = etPseudo.text.toString().trim()

            if (phone.isEmpty() || pseudo.isEmpty()) {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
            } else {
                // Ici, tu peux envoyer toutes les infos à ton serveur ou base de données
                Toast.makeText(this, "Inscription complète !\nPseudo: $pseudo\nTéléphone: $phone", Toast.LENGTH_LONG).show()

                val intent = Intent(this, register_success::class.java)
                intent.putExtra("phone", phone)
                intent.putExtra("pseudo", pseudo)
                intent.putExtra("country", country)
                intent.putExtra("birthdate", birthdate)
                intent.putExtra("email", email)
                intent.putExtra("nom", nom)
                intent.putExtra("prenom", prenom)
                intent.putExtra("avatarUri", avatarUri.toString())

                // Exemple : retour à l'accueil ou autre activité
                startActivity(intent)
                finish()
            }
        }

    }

    // Fonction pour ouvrir la galerie
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    // Récupérer l'image sélectionnée
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = data?.data
            if (imageUri != null) {
                ivAvatar.setImageURI(imageUri)
                avatarUri = imageUri
            }
        }
    }
}