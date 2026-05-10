package com.example.traveling.TravelShare.Groups

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.traveling.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView
import java.io.File

class GroupDetailsActivity : AppCompatActivity() {

    private lateinit var tvGroupName: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvMembers: TextView
    private lateinit var tvInvite: TextView
    private lateinit var tvLeaveGroup: TextView
    private lateinit var tvDeleteGroup: TextView
    private lateinit var rvGroupPosts: RecyclerView
    private lateinit var tvNoPosts: TextView
    private lateinit var ivGroupPhoto: CircleImageView

    private var groupId: String = ""
    private var groupName: String = ""
    private var isAdmin: Boolean = false
    private var currentUserId: String = ""

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_details)

        initViews()
        getIntentData()
        checkIfAdmin()
        loadGroupInfo()
        loadGroupPosts()

        setupClickListeners()
    }

    private fun initViews() {
        tvGroupName = findViewById(R.id.tvGroupName)
        tvDescription = findViewById(R.id.tvDescription)
        tvMembers = findViewById(R.id.tvMembers)
        tvInvite = findViewById(R.id.tvInvite)
        tvLeaveGroup = findViewById(R.id.tvLeaveGroup)
        tvDeleteGroup = findViewById(R.id.tvDeleteGroup)
        rvGroupPosts = findViewById(R.id.rvGroupPosts)
        tvNoPosts = findViewById(R.id.tvNoPosts)
        ivGroupPhoto = findViewById(R.id.ivGroupPhoto)

        rvGroupPosts.layoutManager = LinearLayoutManager(this)
    }

    private fun getIntentData() {
        groupId = intent.getStringExtra("group_id") ?: ""
        groupName = intent.getStringExtra("group_name") ?: "Groupe"
        currentUserId = auth.currentUser?.uid ?: ""

        tvGroupName.text = groupName
    }

    private fun checkIfAdmin() {
        firestore.collection("groups")
            .document(groupId)
            .collection("members")
            .document(currentUserId)
            .get()
            .addOnSuccessListener { doc ->
                isAdmin = doc.getString("role") == "admin"
                if (isAdmin) {
                    tvDeleteGroup.visibility = android.view.View.VISIBLE
                    tvLeaveGroup.visibility = android.view.View.GONE
                } else {
                    tvLeaveGroup.visibility = android.view.View.VISIBLE
                    tvDeleteGroup.visibility = android.view.View.GONE
                }
            }
    }

    private fun loadGroupInfo() {
        firestore.collection("groups")
            .document(groupId)
            .get()
            .addOnSuccessListener { doc ->
                val description = doc.getString("description") ?: ""
                if (description.isNotEmpty()) {
                    tvDescription.text = description
                    tvDescription.visibility = android.view.View.VISIBLE
                }

                val photoPath = doc.getString("photoPath") ?: ""
                if (photoPath.isNotEmpty()) {
                    val file = File(photoPath)
                    if (file.exists()) {
                        ivGroupPhoto.setImageURI(Uri.fromFile(file))
                    } else {
                        ivGroupPhoto.setImageResource(R.drawable.ic_group_default)
                    }
                } else {
                    ivGroupPhoto.setImageResource(R.drawable.ic_group_default)
                }
            }
    }

    private fun loadGroupPosts() {
        firestore.collection("photos")
            .whereEqualTo("groupId", groupId)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty()) {
                    tvNoPosts.visibility = android.view.View.VISIBLE
                    rvGroupPosts.visibility = android.view.View.GONE
                } else {
                    tvNoPosts.visibility = android.view.View.GONE
                    rvGroupPosts.visibility = android.view.View.VISIBLE
                }
            }
    }

    private fun setupClickListeners() {
        tvMembers.setOnClickListener {
            val intent = Intent(this, GroupMembersActivity::class.java)
            intent.putExtra("group_id", groupId)
            startActivity(intent)
        }

        tvInvite.setOnClickListener {
            val intent = Intent(this, InviteMembersActivity::class.java)
            intent.putExtra("group_id", groupId)
            startActivity(intent)
        }

        tvLeaveGroup.setOnClickListener {
            leaveGroup()
        }

        tvDeleteGroup.setOnClickListener {
            deleteGroup()
        }
    }

    private fun leaveGroup() {
        if (isAdmin) {
            Toast.makeText(this, "Vous êtes admin, vous ne pouvez pas quitter, seulement supprimer le groupe", Toast.LENGTH_LONG).show()
            return
        }

        // Supprimer de la sous-collection members
        firestore.collection("groups")
            .document(groupId)
            .collection("members")
            .document(currentUserId)
            .delete()
            .addOnSuccessListener {
                // Mettre à jour memberCount
                firestore.collection("groups")
                    .document(groupId)
                    .update("memberCount", com.google.firebase.firestore.FieldValue.increment(-1))
                    .addOnSuccessListener {
                        Toast.makeText(this, "🚪 Groupe quitté", Toast.LENGTH_SHORT).show()
                        finish()
                    }
            }
    }

    private fun deleteGroup() {
        firestore.collection("groups")
            .document(groupId)
            .collection("members")
            .get()
            .addOnSuccessListener { members ->
                for (member in members) {
                    member.reference.delete()
                }

                firestore.collection("groups")
                    .document(groupId)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "🗑️ Groupe supprimé", Toast.LENGTH_SHORT).show()
                        finish()
                    }
            }
    }
}