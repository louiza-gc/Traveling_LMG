package com.example.traveling.TravelShare.Profil

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.traveling.R
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        ivAvatar = view.findViewById(R.id.ivAvatar)
        tvPseudo = view.findViewById(R.id.tvPseudo)
        tvFullName = view.findViewById(R.id.tvFullName)
        progressBar = view.findViewById(R.id.progressBar)

        // état initial (loading)
        progressBar.visibility = View.VISIBLE
        tvPseudo.visibility = View.INVISIBLE
        tvFullName.visibility = View.INVISIBLE
        ivAvatar.visibility = View.INVISIBLE

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

                //afficher contenu
                progressBar.visibility = View.GONE
                tvPseudo.visibility = View.VISIBLE
                tvFullName.visibility = View.VISIBLE
                ivAvatar.visibility = View.VISIBLE
            }
            .addOnFailureListener {
                // en cas d'erreur
                progressBar.visibility = View.GONE
            }
    }
}