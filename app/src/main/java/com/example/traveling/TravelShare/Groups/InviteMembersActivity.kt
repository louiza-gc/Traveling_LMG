package com.example.traveling.TravelShare.Groups

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.traveling.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class InviteMembersActivity : AppCompatActivity() {

    private lateinit var etSearchUser: EditText
    private lateinit var btnSearchUser: ImageButton
    private lateinit var rvUsers: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private val usersList = mutableListOf<UserItem>()

    private var groupId: String = ""
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_invite_members)

        etSearchUser = findViewById(R.id.etSearchUser)
        btnSearchUser = findViewById(R.id.btnSearchUser)
        rvUsers = findViewById(R.id.rvUsers)

        groupId = intent.getStringExtra("group_id") ?: ""

        rvUsers.layoutManager = LinearLayoutManager(this)

        btnSearchUser.setOnClickListener {
            searchUsers()
        }
    }

    private fun searchUsers() {
        val query = etSearchUser.text.toString().trim()
        if (query.isEmpty()) {
            Toast.makeText(this, "🔎 Entrez un pseudo à rechercher", Toast.LENGTH_SHORT).show()
            return
        }

        firestore.collection("users")
            .get()
            .addOnSuccessListener { result ->
                usersList.clear()

                firestore.collection("groups")
                    .document(groupId)
                    .collection("members")
                    .get()
                    .addOnSuccessListener { members ->
                        val memberIds = members.documents.map { it.id }.toSet()

                        for (doc in result) {
                            val userId = doc.id
                            val pseudo = doc.getString("pseudo") ?: doc.getString("fullName") ?: ""

                            if (pseudo.lowercase().contains(query.lowercase()) &&
                                userId != currentUserId &&
                                !memberIds.contains(userId)) {

                                val user = UserItem(
                                    userId = userId,
                                    name = pseudo,
                                    avatar = doc.getString("avatarPath") ?: ""
                                )
                                usersList.add(user)
                            }
                        }

                        if (usersList.isEmpty()) {
                            Toast.makeText(this, "Aucun utilisateur trouvé", Toast.LENGTH_SHORT).show()
                        }

                        userAdapter = UserAdapter(usersList) { user ->
                            inviteUser(user)
                        }
                        rvUsers.adapter = userAdapter
                    }
            }
    }

    private fun inviteUser(user: UserItem) {
        val invitationData = hashMapOf(
            "groupId" to groupId,
            "invitedUserId" to user.userId,
            "invitedBy" to currentUserId,
            "status" to "pending",
            "createdAt" to System.currentTimeMillis()
        )

        firestore.collection("group_invitations")
            .add(invitationData)
            .addOnSuccessListener {
                Toast.makeText(this, "💌 Invitation envoyée à ${user.name}", Toast.LENGTH_SHORT).show()
                usersList.remove(user)
                userAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}