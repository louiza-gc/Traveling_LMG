package com.example.traveling.TravelShare.Groups

import android.os.Bundle
import android.util.Log
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
    private var groupName: String = ""
    private var currentUserName: String = ""

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
        groupName = intent.getStringExtra("group_name") ?: ""

        rvUsers.layoutManager = LinearLayoutManager(this)

        // Récupérer le nom de l'utilisateur courant
        fetchCurrentUserName()

        btnSearchUser.setOnClickListener {
            searchUsers()
        }
    }

    private fun fetchCurrentUserName() {
        firestore.collection("users")
            .document(currentUserId)
            .get()
            .addOnSuccessListener { doc ->
                currentUserName = doc.getString("pseudo")
                    ?: doc.getString("fullName")
                            ?: "Utilisateur"
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
                // Envoyer la notification à l'utilisateur invité
                sendInvitationNotification(user)

                Toast.makeText(this, "💌 Invitation envoyée à ${user.name}", Toast.LENGTH_SHORT).show()
                usersList.remove(user)
                userAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendInvitationNotification(user: UserItem) {
        val notificationData = hashMapOf(
            "type" to "group_invitation",
            "title" to "💌 Invitation à rejoindre un groupe",
            "message" to "$currentUserName vous a invité à rejoindre le groupe '$groupName'",
            "groupId" to groupId,
            "groupName" to groupName,
            "senderId" to currentUserId,
            "senderName" to currentUserName,
            "isRead" to false,
            "status" to "pending",
            "timestamp" to System.currentTimeMillis()
        )

        firestore.collection("users")
            .document(user.userId)
            .collection("notifications")
            .add(notificationData)
            .addOnSuccessListener {
                Log.d("Invite", "Notification envoyée à ${user.name}")
            }
            .addOnFailureListener { e ->
                Log.e("Invite", "Erreur notification: ${e.message}")
            }
    }
}