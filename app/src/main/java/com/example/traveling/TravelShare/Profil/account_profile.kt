package com.example.traveling.TravelShare.Profil

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.traveling.R
import com.example.traveling.TravelPath.Accueil.AccueilPath
import com.example.traveling.TravelShare.Connection.login
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView
import java.io.File

class account_profile : Fragment(R.layout.fragment_account_profile) {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var ivAvatar: CircleImageView
    private lateinit var tvPseudo: TextView
    private lateinit var tvFullName: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvPostCount: TextView
    private lateinit var tvGroupCount: TextView
    private lateinit var tvLikesCount: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        ivAvatar = view.findViewById(R.id.ivAvatar)
        tvPseudo = view.findViewById(R.id.tvPseudo)
        tvFullName = view.findViewById(R.id.tvFullName)
        progressBar = view.findViewById(R.id.progressBar)
        tvPostCount = view.findViewById(R.id.tvPostCount)
        tvGroupCount = view.findViewById(R.id.tvGroupCount)
        tvLikesCount = view.findViewById(R.id.tvLikesCount)
        val btnMenu = view.findViewById<ImageButton>(R.id.btnMenu)

        btnMenu.setOnClickListener { v ->

            val popup = PopupMenu(requireContext(), v)
            popup.menuInflater.inflate(R.menu.menu_profil, popup.menu)

            //afficher icônes
            try {
                val field = popup.javaClass.getDeclaredField("mPopup")
                field.isAccessible = true
                val menuPopupHelper = field.get(popup)
                val classPopupHelper = Class.forName(menuPopupHelper.javaClass.name)
                val setForceIcons = classPopupHelper.getMethod("setForceShowIcon", Boolean::class.java)
                setForceIcons.invoke(menuPopupHelper, true)

                //fond blanc
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

                    R.id.menu_switch_travelpath -> {
                        val intent = Intent(requireContext(), AccueilPath::class.java)
                        startActivity(intent)
                        true
                    }

                    R.id.menu_logout -> {
                        FirebaseAuth.getInstance().signOut()

                        val intent = Intent(requireContext(), login::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)

                        true
                    }

                    else -> false
                }
            }

            popup.show()
        }

        // état initial (loading)
        progressBar.visibility = View.VISIBLE
        tvPseudo.visibility = View.INVISIBLE
        tvFullName.visibility = View.INVISIBLE
        ivAvatar.visibility = View.INVISIBLE
        tvPostCount.visibility = View.INVISIBLE
        tvGroupCount.visibility = View.INVISIBLE
        tvLikesCount.visibility = View.INVISIBLE

        val user = auth.currentUser

        if (user == null) {
            progressBar.visibility = View.GONE
            return
        }


        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { doc ->

                if (!doc.exists()) {
                    progressBar.visibility = View.GONE
                    return@addOnSuccessListener
                }

                val pseudo = doc.getString("pseudo") ?: ""
                val nom = doc.getString("nom") ?: ""
                val prenom = doc.getString("prenom") ?: ""

                tvPseudo.text = pseudo
                tvFullName.text = "$prenom $nom"

                val avatarPath = doc.getString("avatarPath")

                if (!avatarPath.isNullOrEmpty()) {
                    val file = File(avatarPath)

                    if (file.exists()) {
                        ivAvatar.setImageURI(Uri.fromFile(file))
                    } else {
                        ivAvatar.setImageResource(R.drawable.ic_default_avatar)
                    }
                } else {
                    ivAvatar.setImageResource(R.drawable.ic_default_avatar)
                }

                val userId = user.uid

                // posts
                db.collection("posts")
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnSuccessListener { result ->
                        tvPostCount.text = result.size().toString()
                    }

                // groups
                db.collection("groups")
                    .whereArrayContains("members", userId)
                    .get()
                    .addOnSuccessListener { result ->
                        tvGroupCount.text = result.size().toString()
                    }

                // likes
                db.collection("likes")
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnSuccessListener { result ->
                        tvLikesCount.text = result.size().toString()
                    }

                //afficher contenu
                progressBar.visibility = View.GONE
                tvPseudo.visibility = View.VISIBLE
                tvFullName.visibility = View.VISIBLE
                ivAvatar.visibility = View.VISIBLE
                tvPostCount.visibility = View.VISIBLE
                tvGroupCount.visibility = View.VISIBLE
                tvLikesCount.visibility = View.VISIBLE
            }
            .addOnFailureListener {
                // en cas d'erreur
                progressBar.visibility = View.GONE
            }
    }
}