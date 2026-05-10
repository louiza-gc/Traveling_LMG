package com.example.traveling.TravelShare.Groups

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.traveling.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class GroupMembersActivity : AppCompatActivity() {

    private lateinit var rvMembers: RecyclerView
    private lateinit var memberAdapter: MemberAdapter
    private val membersList = mutableListOf<MemberItem>()
    private val processedUserIds = mutableSetOf<String>()

    private var groupId: String = ""
    private var currentUserId: String = ""
    private var isAdmin: Boolean = false

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_members)

        rvMembers = findViewById(R.id.rvMembers)
        groupId = intent.getStringExtra("group_id") ?: ""
        currentUserId = auth.currentUser?.uid ?: ""

        rvMembers.layoutManager = LinearLayoutManager(this)

        checkIfAdminAndLoad()
    }

    private fun checkIfAdminAndLoad() {
        firestore.collection("groups")
            .document(groupId)
            .collection("members")
            .document(currentUserId)
            .get()
            .addOnSuccessListener { doc ->
                isAdmin = doc.getString("role") == "admin"
                loadMembers()
            }
    }

    private fun loadMembers() {
        membersList.clear()
        processedUserIds.clear()

        firestore.collection("groups")
            .document(groupId)
            .collection("members")
            .get()
            .addOnSuccessListener { membersResult ->

                for (memberDoc in membersResult.documents) {
                    val userId = memberDoc.id
                    val role = memberDoc.getString("role") ?: "member"

                    if (processedUserIds.contains(userId)) {
                        continue
                    }
                    processedUserIds.add(userId)

                    // Récupérer les infos utilisateur
                    firestore.collection("users")
                        .document(userId)
                        .get()
                        .addOnSuccessListener { userDoc ->
                            val name = userDoc.getString("pseudo")
                                ?: userDoc.getString("fullName")
                                ?: "Utilisateur"
                            val avatar = userDoc.getString("avatarPath") ?: ""

                            val member = MemberItem(userId, name, avatar, role)

                            if (!membersList.any { it.userId == userId }) {
                                membersList.add(member)
                                updateAdapter()
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateAdapter() {
        val sortedList = membersList.sortedByDescending { it.role == "admin" }

        memberAdapter = MemberAdapter(sortedList, isAdmin) { member ->
            removeMember(member)
        }
        rvMembers.adapter = memberAdapter
    }

    private fun removeMember(member: MemberItem) {
        firestore.collection("groups")
            .document(groupId)
            .collection("members")
            .document(member.userId)
            .delete()
            .addOnSuccessListener {
                firestore.collection("groups")
                    .document(groupId)
                    .update("memberCount", com.google.firebase.firestore.FieldValue.increment(-1))
                    .addOnSuccessListener {
                        Toast.makeText(this, "${member.name} retiré du groupe", Toast.LENGTH_SHORT).show()
                        membersList.removeAll { it.userId == member.userId }
                        updateAdapter()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}