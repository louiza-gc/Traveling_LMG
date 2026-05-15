package com.example.traveling.TravelShare.Notification

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.traveling.R
import com.example.traveling.TravelShare.Acceuil.photo_post
import com.example.traveling.TravelShare.Groups.GroupDetailsActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

class NotificationsFragment : Fragment() {

    private lateinit var rvNotifications: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var notificationAdapter: NotificationAdapter
    private val notificationsList = mutableListOf<NotificationItem>()

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid ?: ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvNotifications = view.findViewById(R.id.rvNotifications)
        tvEmpty = view.findViewById(R.id.tvEmpty)

        setupRecyclerView()
        loadNotifications()
    }

    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter(
            notificationsList,
            onItemClick = { notification, _ ->
                handleNotificationClick(notification)
            },
            onAccept = { notification, position ->
                acceptInvitation(notification, position)
            },
            onDecline = { notification, position ->
                declineInvitation(notification, position)
            }
        )
        rvNotifications.layoutManager = LinearLayoutManager(requireContext())
        rvNotifications.adapter = notificationAdapter
    }

    private fun loadNotifications() {
        if (currentUserId.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text = "Connectez-vous pour voir vos notifications"
            return
        }

        firestore.collection("users")
            .document(currentUserId)
            .collection("notifications")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(requireContext(), "Erreur: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                notificationsList.clear()

                snapshot?.documents?.forEach { doc ->
                    val data = doc.data
                    if (data != null) {
                        val notification = NotificationItem(
                            id = doc.id,
                            type = data["type"] as? String ?: "",
                            title = data["title"] as? String ?: "",
                            message = data["message"] as? String ?: "",
                            groupId = data["groupId"] as? String ?: "",
                            groupName = data["groupName"] as? String ?: "",
                            postId = data["postId"] as? String ?: "",
                            locationName = data["locationName"] as? String ?: "",
                            tag = data["tag"] as? String ?: "",
                            senderId = data["senderId"] as? String ?: "",
                            senderName = data["senderName"] as? String ?: "",
                            isRead = data["isRead"] as? Boolean ?: false,
                            status = data["status"] as? String ?: "pending",
                            timestamp = data["timestamp"] as? Long ?: System.currentTimeMillis()
                        )
                        notificationsList.add(notification)
                    }
                }

                if (notificationsList.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                    tvEmpty.text = "📭 Aucune notification"
                    rvNotifications.visibility = View.GONE
                } else {
                    tvEmpty.visibility = View.GONE
                    rvNotifications.visibility = View.VISIBLE
                    notificationAdapter.notifyDataSetChanged()
                }
            }
    }

    private fun acceptInvitation(notification: NotificationItem, position: Int) {
        val currentUserId = auth.currentUser?.uid ?: return

        // Récupérer le nom de l'utilisateur courant
        firestore.collection("users")
            .document(currentUserId)
            .get()
            .addOnSuccessListener { userDoc ->
                val currentUserName = userDoc.getString("pseudo")
                    ?: userDoc.getString("fullName")
                    ?: "Quelqu'un"

                // Ajouter l'utilisateur comme membre du groupe
                val memberData = hashMapOf(
                    "userId" to currentUserId,
                    "role" to "member",
                    "joinedAt" to System.currentTimeMillis()
                )

                firestore.collection("groups")
                    .document(notification.groupId)
                    .collection("members")
                    .document(currentUserId)
                    .set(memberData)
                    .addOnSuccessListener {
                        // 1. Mettre à jour memberCount
                        firestore.collection("groups")
                            .document(notification.groupId)
                            .update("memberCount", FieldValue.increment(1))

                        // 2. Mettre à jour le statut de la notification
                        firestore.collection("users")
                            .document(currentUserId)
                            .collection("notifications")
                            .document(notification.id)
                            .update("status", "accepted")

                        // 3. Notification : "X a rejoint le groupe" pour TOUS les membres
                        sendUserJoinedNotification(notification.groupId, notification.groupName, currentUserId, currentUserName)

                        // 4. Notification : "X a accepté votre invitation" pour l'inviteur
                        sendInvitationAcceptedNotification(notification, currentUserName)

                        // Supprimer la notification de la liste locale
                        notificationsList.removeAt(position)
                        notificationAdapter.notifyItemRemoved(position)

                        Toast.makeText(requireContext(), "Vous avez rejoint ${notification.groupName}", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
    }

    private fun declineInvitation(notification: NotificationItem, position: Int) {
        val currentUserId = auth.currentUser?.uid ?: return

        firestore.collection("users")
            .document(currentUserId)
            .collection("notifications")
            .document(notification.id)
            .update("status", "declined")
            .addOnSuccessListener {
                notificationsList.removeAt(position)
                notificationAdapter.notifyItemRemoved(position)
                Toast.makeText(requireContext(), "❌ Invitation refusée", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendUserJoinedNotification(groupId: String, groupName: String, newMemberId: String, newMemberName: String) {
        // Récupérer tous les membres du groupe (sauf le nouveau membre)
        firestore.collection("groups")
            .document(groupId)
            .collection("members")
            .get()
            .addOnSuccessListener { members ->
                for (memberDoc in members) {
                    val memberId = memberDoc.id
                    if (memberId != newMemberId) {
                        val notificationData = hashMapOf(
                            "type" to "user_joined_group",
                            "title" to "👋 Nouveau membre",
                            "message" to "$newMemberName a rejoint le groupe '$groupName'",
                            "groupId" to groupId,
                            "groupName" to groupName,
                            "senderId" to newMemberId,
                            "senderName" to newMemberName,
                            "isRead" to false,
                            "status" to "accepted",
                            "timestamp" to System.currentTimeMillis()
                        )

                        firestore.collection("users")
                            .document(memberId)
                            .collection("notifications")
                            .add(notificationData)
                    }
                }
            }
    }

    private fun sendInvitationAcceptedNotification(originalNotification: NotificationItem, currentUserName: String) {
        val notificationData = hashMapOf(
            "type" to "invitation_accepted",
            "title" to "✅ Invitation acceptée",
            "message" to "$currentUserName a accepté votre invitation à rejoindre le groupe '${originalNotification.groupName}'",
            "groupId" to originalNotification.groupId,
            "groupName" to originalNotification.groupName,
            "senderId" to auth.currentUser?.uid,
            "senderName" to currentUserName,
            "isRead" to false,
            "status" to "accepted",
            "timestamp" to System.currentTimeMillis()
        )

        firestore.collection("users")
            .document(originalNotification.senderId)
            .collection("notifications")
            .add(notificationData)
    }

    private fun handleNotificationClick(notification: NotificationItem) {
        // Marquer comme lue
        firestore.collection("users")
            .document(currentUserId)
            .collection("notifications")
            .document(notification.id)
            .update("isRead", true)

        when (notification.type) {
            "new_post_in_group" -> {
                // Rediriger vers la publication
                if (notification.postId.isNotEmpty()) {
                    val intent = Intent(requireContext(), photo_post::class.java)
                    intent.putExtra("post_id", notification.postId)
                    startActivity(intent)
                } else {
                    Toast.makeText(requireContext(), "Publication non trouvée", Toast.LENGTH_SHORT).show()
                }
            }
            "user_joined_group", "invitation_accepted" -> {
                // Rediriger vers le groupe
                val intent = Intent(requireContext(), GroupDetailsActivity::class.java)
                intent.putExtra("group_id", notification.groupId)
                intent.putExtra("group_name", notification.groupName)
                startActivity(intent)
            }
            else -> {
                Toast.makeText(requireContext(), notification.title, Toast.LENGTH_SHORT).show()
            }
        }
    }
}