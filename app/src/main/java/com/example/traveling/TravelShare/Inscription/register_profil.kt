package com.example.traveling.TravelShare.Inscription

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.traveling.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream

class register_profil : AppCompatActivity() {

    private val PICK_IMAGE_REQUEST = 100

    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    private var avatarUri: Uri? = null
    private lateinit var ivAvatar: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register_profil)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()

        val etPhone = findViewById<EditText>(R.id.etPhone)
        val etPseudo = findViewById<TextInputEditText>(R.id.etPseudo)
        val btnValidate = findViewById<MaterialButton>(R.id.btnValidate)

        ivAvatar = findViewById(R.id.ivAvatar)
        val flAvatar = findViewById<FrameLayout>(R.id.flAvatar)

        flAvatar.setOnClickListener {
            openGallery()
        }

        btnValidate.setOnClickListener {

            val phone = etPhone.text.toString().trim()
            val pseudo = etPseudo.text.toString().trim()

            if (pseudo.isEmpty()) return@setOnClickListener showError("Pseudo obligatoire")
            if (phone.isEmpty()) return@setOnClickListener showError("Téléphone obligatoire")
            if (!isValidPhone(phone)) return@setOnClickListener showError("Numéro invalide")

            db.collection("users")
                .whereEqualTo("pseudo", pseudo)
                .get()
                .addOnSuccessListener { result ->

                    if (!result.isEmpty) {
                        showError("Pseudo déjà utilisé")
                    } else {
                        createAccount(phone, pseudo)
                    }
                }
        }
    }

    private fun createAccount(phone: String, pseudo: String) {

        val email = intent.getStringExtra("email")!!
        val password = intent.getStringExtra("password")!!
        val nom = intent.getStringExtra("nom")
        val prenom = intent.getStringExtra("prenom")
        val birthdate = intent.getStringExtra("birthdate")
        val country = intent.getStringExtra("country")

        val avatarPath = if (avatarUri != null) {
            saveImageToInternalStorage(avatarUri!!)
        } else {
            "" // vide = on utilisera avatar par défaut dans profil
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->

                if (task.isSuccessful) {

                    val uid = auth.currentUser!!.uid

                    val user = hashMapOf(
                        "email" to email,
                        "nom" to nom,
                        "prenom" to prenom,
                        "phone" to phone,
                        "pseudo" to pseudo,
                        "country" to country,
                        "birthdate" to birthdate,
                        "avatarPath" to avatarPath
                    )

                    db.collection("users").document(uid)
                        .set(user)
                        .addOnSuccessListener {

                            val intent = Intent(this, register_success::class.java)
                            intent.putExtra("email", email)
                            intent.putExtra("nom", nom)
                            intent.putExtra("prenom", prenom)
                            intent.putExtra("phone", phone)
                            intent.putExtra("pseudo", pseudo)
                            intent.putExtra("country", country)
                            intent.putExtra("birthdate", birthdate)
                            intent.putExtra("avatarPath", avatarPath)

                            startActivity(intent)
                            finish()
                        }
                        .addOnFailureListener {
                            showError("Erreur Firestore")
                        }

                } else {
                    showError("Email ou mot de passe incorrect")
                }
            }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            avatarUri = data?.data
            ivAvatar.setImageURI(avatarUri)
        }
    }

    private fun saveImageToInternalStorage(uri: Uri): String {
        val file = File(filesDir, "avatar_${System.currentTimeMillis()}.jpg")

        val inputStream = contentResolver.openInputStream(uri)
        val outputStream = FileOutputStream(file)

        inputStream?.copyTo(outputStream)

        inputStream?.close()
        outputStream.close()

        return file.absolutePath
    }

    private fun isValidPhone(phone: String): Boolean {
        return phone.matches(Regex("^[0-9]{8,15}$"))
    }

    private fun showError(message: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Erreur")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}