package com.example.traveling.TravelPath.Accueil

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.traveling.R
import com.example.traveling.TravelPath.Parcours.MyItinerariesActivity
import com.example.traveling.TravelShare.Connection.login
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView
import java.io.File

class TravelPathProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var ivAvatar: CircleImageView
    private lateinit var tvPseudo: TextView
    private lateinit var tvFullName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvItineraryCount: TextView
    private lateinit var tvFavoritesCount: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_travelpath_profile)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_path_favorites -> {
                    if (this !is FavoritesActivity) {
                        Intent(this, FavoritesActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        }.also { startActivity(it) }
                    }
                    true
                }
                R.id.menu_path_itineraries -> {
                    if (this !is MyItinerariesActivity) {
                        Intent(this, MyItinerariesActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        }.also { startActivity(it) }
                    }
                    true
                }
                R.id.menu_path_profile -> true
                R.id.menu_path_home -> {
                    if (this !is AccueilPath) {
                        Intent(this, AccueilPath::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        }.also { startActivity(it) }
                    }
                    true
                }
                else -> false
            }
        }

        bottomNav.selectedItemId = when (this) {
            is AccueilPath -> R.id.menu_path_home
            is MyItinerariesActivity -> R.id.menu_path_itineraries
            is FavoritesActivity -> R.id.menu_path_favorites
            is TravelPathProfileActivity -> R.id.menu_path_profile
            else -> R.id.menu_path_home
        }
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        ivAvatar = findViewById(R.id.ivAvatar)
        tvPseudo = findViewById(R.id.tvPseudo)
        tvFullName = findViewById(R.id.tvFullName)
        tvEmail = findViewById(R.id.tvEmail)
        tvItineraryCount = findViewById(R.id.tvItineraryCount)
        tvFavoritesCount = findViewById(R.id.tvFavoritesCount)
        progressBar = findViewById(R.id.progressBar)

        val btnMenu = findViewById<ImageButton>(R.id.btnMenu)
        btnMenu.setOnClickListener { showMenu(it) }

        loadUserProfile()
    }

    private fun loadUserProfile() {
        val user = auth.currentUser
        if (user == null) {
            // Non connecté -> redirection vers login
            startActivity(Intent(this, login::class.java))
            finish()
            return
        }

        progressBar.visibility = View.VISIBLE

        // Récupérer les infos depuis Firestore (collection "users")
        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val pseudo = doc.getString("pseudo") ?: ""
                    val nom = doc.getString("nom") ?: ""
                    val prenom = doc.getString("prenom") ?: ""
                    val avatarPath = doc.getString("avatarPath") ?: ""

                    tvPseudo.text = pseudo.ifEmpty { user.email ?: "Utilisateur" }
                    tvFullName.text = if (nom.isNotEmpty() || prenom.isNotEmpty()) "$prenom $nom" else ""
                    tvEmail.text = user.email ?: ""

                    if (avatarPath.isNotEmpty()) {
                        val file = File(avatarPath)
                        if (file.exists()) {
                            ivAvatar.setImageURI(Uri.fromFile(file))
                        } else {
                            ivAvatar.setImageResource(R.drawable.ic_default_avatar)
                        }
                    } else {
                        ivAvatar.setImageResource(R.drawable.ic_default_avatar)
                    }
                } else {
                    // Si l'utilisateur n'a pas de document Firestore (ancien compte ?)
                    tvPseudo.text = user.email ?: "Utilisateur"
                    tvFullName.text = ""
                    tvEmail.text = user.email ?: ""
                    ivAvatar.setImageResource(R.drawable.ic_default_avatar)
                }

                // Charger les statistiques
                loadStatistics(user.uid)
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Erreur de chargement", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadStatistics(userId: String) {
        // Compter les itinéraires
        db.collection("itineraries")
            .whereEqualTo("createdBy", userId)
            .get()
            .addOnSuccessListener { result ->
                tvItineraryCount.text = result.size().toString()
            }

        // Compter les favoris depuis Firestore
        db.collection("favorites").document(userId).get()
            .addOnSuccessListener { doc ->
                val favorites = (doc.get("placeIds") as? List<String>) ?: emptyList()
                tvFavoritesCount.text = favorites.size.toString()
                progressBar.visibility = View.GONE
            }
            .addOnFailureListener {
                tvFavoritesCount.text = "0"
                progressBar.visibility = View.GONE
            }
    }

    private fun showMenu(view: View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.menu_travelpath_profile, popup.menu)

        // Pour afficher les icônes et fond blanc
        try {
            val field = popup.javaClass.getDeclaredField("mPopup")
            field.isAccessible = true
            val menuPopupHelper = field.get(popup)
            val classPopupHelper = Class.forName(menuPopupHelper.javaClass.name)
            val setForceIcons = classPopupHelper.getMethod("setForceShowIcon", Boolean::class.java)
            setForceIcons.invoke(menuPopupHelper, true)
            val setBackground = classPopupHelper.getMethod(
                "setBackgroundDrawable",
                android.graphics.drawable.Drawable::class.java
            )
            setBackground.invoke(
                menuPopupHelper,
                android.graphics.drawable.ColorDrawable(android.graphics.Color.WHITE)
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_my_itineraries -> {
                    startActivity(Intent(this, MyItinerariesActivity::class.java))
                    true
                }
                R.id.menu_switch_travelshare -> {
                    startActivity(Intent(this, com.example.traveling.TravelShare.MainActivity::class.java))
                    true
                }
                R.id.menu_logout -> {
                    FirebaseAuth.getInstance().signOut()
                    val intent = Intent(this, login::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }
}