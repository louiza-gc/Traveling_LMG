package com.example.traveling.TravelShare.Profil

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.traveling.R
import com.example.traveling.TravelShare.Connection.login
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView

class account_profil : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_account_profil)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val nav = findViewById<BottomNavigationView>(R.id.bottomNav)
        nav.selectedItemId = R.id.nav_profile

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val ivAvatar = findViewById<CircleImageView>(R.id.ivAvatar)
        val tvPseudo = findViewById<TextView>(R.id.tvPseudo)
        val tvFullName = findViewById<TextView>(R.id.tvFullName)

        val user = auth.currentUser

        if (user == null) {
            startActivity(Intent(this, login::class.java))
            finish()
            return
        }

        val uid = user.uid

        // récupérer données Firestore
        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->

                if (doc.exists()) {

                    val pseudo = doc.getString("pseudo") ?: "User"
                    val nom = doc.getString("nom") ?: ""
                    val prenom = doc.getString("prenom") ?: ""

                    tvPseudo.text = pseudo
                    tvFullName.text = "$prenom $nom"

                    val avatarPath = doc.getString("avatarPath")

                    if (!avatarPath.isNullOrEmpty()) {
                        val file = java.io.File(avatarPath)

                        if (file.exists()) {
                            ivAvatar.setImageURI(android.net.Uri.fromFile(file))
                        } else {
                            ivAvatar.setImageResource(R.drawable.ic_default_avatar)
                        }
                    } else {
                        ivAvatar.setImageResource(R.drawable.ic_default_avatar)
                    }
                }
            }
            .addOnFailureListener {
                tvPseudo.text = "Erreur chargement"
            }

    }

}