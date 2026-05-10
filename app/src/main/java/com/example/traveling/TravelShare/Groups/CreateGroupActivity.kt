package com.example.traveling.TravelShare.Groups

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.traveling.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream

class CreateGroupActivity : AppCompatActivity() {

    private val PICK_IMAGE_REQUEST = 100

    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    private var groupPhotoUri: Uri? = null
    private lateinit var ivGroupPhoto: ImageView
    private lateinit var etGroupName: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var btnCreate: MaterialButton
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_group)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()

        ivGroupPhoto = findViewById(R.id.ivGroupPhoto)
        etGroupName = findViewById(R.id.etGroupName)
        etDescription = findViewById(R.id.etDescription)
        btnCreate = findViewById(R.id.btnCreateGroup)
        progressBar = findViewById(R.id.progressBar)

        val tvChangePhoto = findViewById<TextView>(R.id.tvChangePhoto)
        tvChangePhoto.setOnClickListener {
            openGallery()
        }

        btnCreate.setOnClickListener {
            createGroup()
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
            groupPhotoUri = data?.data
            ivGroupPhoto.setImageURI(groupPhotoUri)
        }
    }

    private fun saveImageToInternalStorage(uri: Uri): String {
        val file = File(filesDir, "group_${System.currentTimeMillis()}.jpg")

        val inputStream = contentResolver.openInputStream(uri)
        val outputStream = FileOutputStream(file)

        inputStream?.copyTo(outputStream)

        inputStream?.close()
        outputStream.close()

        return file.absolutePath
    }


    private fun createGroup() {
        val name = etGroupName.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "Veuillez entrer un nom", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = android.view.View.VISIBLE
        btnCreate.isEnabled = false

        val currentUserId = auth.currentUser?.uid ?: run {
            progressBar.visibility = android.view.View.GONE
            btnCreate.isEnabled = true
            Toast.makeText(this, "Utilisateur non connecté", Toast.LENGTH_SHORT).show()
            return
        }

        val groupPhotoPath = if (groupPhotoUri != null) {
            saveImageToInternalStorage(groupPhotoUri!!)
        } else {
            "" // pas de photo
        }

        val description = etDescription.text.toString().trim()

        val groupData = hashMapOf(
            "name" to name,
            "description" to description,
            "photoPath" to groupPhotoPath,
            "createdBy" to currentUserId,
            "createdAt" to System.currentTimeMillis(),
            "memberCount" to 1
        )

        db.collection("groups")
            .add(groupData)
            .addOnSuccessListener { docRef ->
                val memberData = hashMapOf(
                    "userId" to currentUserId,
                    "role" to "admin",
                    "joinedAt" to System.currentTimeMillis()
                )

                docRef.collection("members")
                    .document(currentUserId)
                    .set(memberData)
                    .addOnSuccessListener {
                        progressBar.visibility = android.view.View.GONE
                        Toast.makeText(this, "Groupe créé !", Toast.LENGTH_SHORT).show()

                        val resultIntent = Intent()
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        progressBar.visibility = android.view.View.GONE
                        btnCreate.isEnabled = true
                        Toast.makeText(this, "Erreur membre: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = android.view.View.GONE
                btnCreate.isEnabled = true
                Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}