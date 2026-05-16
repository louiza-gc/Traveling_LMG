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
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class NotificationsFragment : Fragment() {

    private lateinit var rvNotifications: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var btnFollowedTags: MaterialButton
    private lateinit var notificationAdapter: NotificationAdapter
    private val notificationsList = mutableListOf<NotificationItem>()

    private val firestore     = FirebaseFirestore.getInstance()
    private val auth          = FirebaseAuth.getInstance()
    private val currentUserId get() = auth.currentUser?.uid ?: ""

    // Tags que l'user suit actuellement (chargés depuis Firestore)
    private var followedTags = mutableListOf<String>()

    private val allTags = listOf(
        "Montagne", "Plage", "Culture", "Gastronomie", "Coucher de soleil",
        "Photo", "Randonnée", "Sport", "Urbain", "Road trip",
        "Camping", "Nature", "Écotourisme", "Festival", "Mer"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_notifications, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvNotifications = view.findViewById(R.id.rvNotifications)
        tvEmpty         = view.findViewById(R.id.tvEmpty)
        btnFollowedTags = view.findViewById(R.id.btnFollowedTags)

        setupRecyclerView()
        loadFollowedTags()         // Charger les tags suivis d'abord
        loadNotifications()

        btnFollowedTags.setOnClickListener {
            showFollowedTagsBottomSheet()
        }
    }

    // ==================== SETUP ====================

    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter(
            notificationsList,
            onItemClick = { notification, _ -> handleNotificationClick(notification) },
            onAccept    = { notification, position -> acceptInvitation(notification, position) },
            onDecline   = { notification, position -> declineInvitation(notification, position) }
        )
        rvNotifications.layoutManager = LinearLayoutManager(requireContext())
        rvNotifications.adapter       = notificationAdapter
    }

    // ==================== TAGS SUIVIS ====================

    /**
     * Charge les tags suivis depuis Firestore
     */
    private fun loadFollowedTags() {
        if (currentUserId.isEmpty()) return

        firestore.collection("users").document(currentUserId).get()
            .addOnSuccessListener { doc ->
                @Suppress("UNCHECKED_CAST")
                val tags = doc.get("followedTags") as? List<String> ?: emptyList()
                followedTags = tags.toMutableList()
                updateFollowedTagsButton()
            }
    }

    /**
     * Met à jour le libellé du bouton selon le nb de tags suivis
     */
    private fun updateFollowedTagsButton() {
        btnFollowedTags.text = if (followedTags.isEmpty()) {
            "🏷️ Tags suivis"
        } else {
            "🏷️ Tags suivis (${followedTags.size})"
        }
    }

    /**
     * Bottom sheet pour choisir les tags suivis
     */
    private fun showFollowedTagsBottomSheet() {
        val dialog     = BottomSheetDialog(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.bottom_sheet_followed_tags, null)

        val chipGroup  = dialogView.findViewById<ChipGroup>(R.id.chipGroupFollowedTags)
        val btnConfirm = dialogView.findViewById<MaterialButton>(R.id.btnConfirmTags)

        // Créer une chip par tag, cocher ceux déjà suivis
        allTags.forEach { tag ->
            val chip = Chip(requireContext()).apply {
                text        = tag
                isCheckable = true
                isChecked   = followedTags.contains(tag)
                isClickable = true
            }
            chipGroup.addView(chip)
        }

        btnConfirm.setOnClickListener {
            // Récupérer les tags cochés
            val selectedTags = mutableListOf<String>()
            for (i in 0 until chipGroup.childCount) {
                val chip = chipGroup.getChildAt(i) as? Chip
                if (chip?.isChecked == true) selectedTags.add(chip.text.toString())
            }

            // Sauvegarder dans Firestore
            saveFollowedTags(selectedTags)
            dialog.dismiss()
        }

        dialog.setContentView(dialogView)
        dialog.show()
    }

    /**
     * Sauvegarde les tags suivis dans Firestore et met à jour localement
     */
    private fun saveFollowedTags(tags: List<String>) {
        if (currentUserId.isEmpty()) return

        firestore.collection("users").document(currentUserId)
            .update("followedTags", tags)
            .addOnSuccessListener {
                followedTags = tags.toMutableList()
                updateFollowedTagsButton()
                Toast.makeText(requireContext(), "Tags suivis mis à jour ", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                // Si le champ n'existe pas encore → set au lieu de update
                firestore.collection("users").document(currentUserId)
                    .set(mapOf("followedTags" to tags), com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener {
                        followedTags = tags.toMutableList()
                        updateFollowedTagsButton()
                        Toast.makeText(requireContext(), "Tags suivis mis à jour", Toast.LENGTH_SHORT).show()
                    }
            }
    }

    // ==================== LOAD NOTIFICATIONS ====================

    private fun loadNotifications() {
        if (currentUserId.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text       = "Connectez-vous pour voir vos notifications"
            return
        }

        firestore.collection("users")
            .document(currentUserId)
            .collection("notifications")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (!isAdded) return@addSnapshotListener
                if (error != null) {
                    Toast.makeText(requireContext(), "Erreur: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                notificationsList.clear()

                snapshot?.documents?.forEach { doc ->
                    val data = doc.data ?: return@forEach
                    val notif = NotificationItem(
                        id           = doc.id,
                        type         = data["type"]         as? String ?: "",
                        title        = data["title"]        as? String ?: "",
                        message      = data["message"]      as? String ?: "",
                        groupId      = data["groupId"]      as? String ?: "",
                        groupName    = data["groupName"]    as? String ?: "",
                        postId       = data["postId"]       as? String ?: "",
                        locationName = data["locationName"] as? String ?: "",
                        tag          = data["tag"]          as? String ?: "",
                        senderId     = data["senderId"]     as? String ?: "",
                        senderName   = data["senderName"]  as? String ?: "",
                        isRead       = data["isRead"]       as? Boolean ?: false,
                        status       = data["status"]       as? String ?: "pending",
                        timestamp    = data["timestamp"]    as? Long ?: System.currentTimeMillis()
                    )

                    // Filtrer les notifs de type tag/lieu selon les tags suivis
                    if (shouldShowNotification(notif)) {
                        notificationsList.add(notif)
                    }
                }

                if (notificationsList.isEmpty()) {
                    tvEmpty.visibility         = View.VISIBLE
                    tvEmpty.text               = "📭 Aucune notification"
                    rvNotifications.visibility = View.GONE
                } else {
                    tvEmpty.visibility         = View.GONE
                    rvNotifications.visibility = View.VISIBLE
                    notificationAdapter.notifyDataSetChanged()
                }
            }
    }

    /**
     * Décide si une notif doit être affichée selon son type et les tags suivis
     *
     * Règles :
     * - new_post_in_group      → toujours afficher (c'est une notif de groupe)
     * - new_post_in_tag        → afficher seulement si le tag est suivi
     *                            OU si l'user ne suit aucun tag (= pas de filtre)
     * - new_post_in_location   → toujours afficher (pas de filtre par lieu pour l'instant)
     * - new_post_public        → toujours afficher
     * - autres (invitation...) → toujours afficher
     */
    private fun shouldShowNotification(notif: NotificationItem): Boolean {
        return when (notif.type) {
            "new_post_in_tag" -> {
                // Si l'user suit au moins un tag → filtrer
                // Sinon → tout afficher
                if (followedTags.isEmpty()) true
                else followedTags.any { it.equals(notif.tag, ignoreCase = true) }
            }
            else -> true
        }
    }

    // ==================== CLICK HANDLER ====================

    private fun handleNotificationClick(notification: NotificationItem) {
        if (!notification.isRead) {
            firestore.collection("users")
                .document(currentUserId)
                .collection("notifications")
                .document(notification.id)
                .update("isRead", true)
        }

        when (notification.type) {
            "new_post_in_group",
            "new_post_in_tag",
            "new_post_in_location",
            "new_post_public" -> {
                if (notification.postId.isNotEmpty()) {
                    openPost(notification.postId)
                } else if (notification.groupId.isNotEmpty()) {
                    openGroup(notification.groupId, notification.groupName)
                } else {
                    Toast.makeText(requireContext(), "Publication introuvable", Toast.LENGTH_SHORT).show()
                }
            }
            "user_joined_group", "invitation_accepted" -> {
                if (notification.groupId.isNotEmpty()) openGroup(notification.groupId, notification.groupName)
            }
            "group_invitation" -> {
                if (notification.groupId.isNotEmpty()) openGroup(notification.groupId, notification.groupName)
            }
            else -> Toast.makeText(requireContext(), notification.title, Toast.LENGTH_SHORT).show()
        }
    }

    // ==================== NAVIGATION ====================

    private fun openPost(postId: String) {
        val intent = Intent(requireContext(), photo_post::class.java).apply {
            putExtra("post_id", postId)
        }
        startActivity(intent)
    }

    private fun openGroup(groupId: String, groupName: String) {
        // Vérifier que le groupe existe encore avant de rediriger
        firestore.collection("groups").document(groupId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val intent = Intent(requireContext(), GroupDetailsActivity::class.java).apply {
                        putExtra("group_id", groupId)
                        putExtra("group_name", groupName)
                    }
                    startActivity(intent)
                } else {
                    Toast.makeText(requireContext(), "Ce groupe n'existe plus", Toast.LENGTH_SHORT).show()
                    deleteNotificationsLinkedToGroup(groupId)
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Impossible de vérifier le groupe", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteNotificationsLinkedToGroup(groupId: String) {
        firestore.collection("users")
            .document(currentUserId)
            .collection("notifications")
            .whereEqualTo("groupId", groupId)
            .get()
            .addOnSuccessListener { notifs ->
                for (notif in notifs) notif.reference.delete()
            }
    }

    // ==================== INVITATION ====================

    private fun acceptInvitation(notification: NotificationItem, position: Int) {
        val uid = currentUserId.ifEmpty { return }

        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { userDoc ->
                val currentUserName = userDoc.getString("pseudo")
                    ?: userDoc.getString("fullName")
                    ?: "Quelqu'un"

                val memberData = hashMapOf(
                    "userId"   to uid,
                    "role"     to "member",
                    "joinedAt" to System.currentTimeMillis()
                )

                firestore.collection("groups")
                    .document(notification.groupId)
                    .collection("members")
                    .document(uid)
                    .set(memberData)
                    .addOnSuccessListener {
                        firestore.collection("groups")
                            .document(notification.groupId)
                            .update("memberCount", FieldValue.increment(1))

                        firestore.collection("users")
                            .document(uid)
                            .collection("notifications")
                            .document(notification.id)
                            .update("status", "accepted")

                        sendUserJoinedNotification(
                            groupId       = notification.groupId,
                            groupName     = notification.groupName,
                            newMemberId   = uid,
                            newMemberName = currentUserName
                        )
                        sendInvitationAcceptedNotification(notification, currentUserName)

                        notificationsList.removeAt(position)
                        notificationAdapter.notifyItemRemoved(position)

                        Toast.makeText(
                            requireContext(),
                            "Vous avez rejoint ${notification.groupName}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
    }

    private fun declineInvitation(notification: NotificationItem, position: Int) {
        val uid = currentUserId.ifEmpty { return }

        firestore.collection("users")
            .document(uid)
            .collection("notifications")
            .document(notification.id)
            .update("status", "declined")
            .addOnSuccessListener {
                notificationsList.removeAt(position)
                notificationAdapter.notifyItemRemoved(position)
                Toast.makeText(requireContext(), "Invitation refusée", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // ==================== NOTIFS INTERNES ====================

    private fun sendUserJoinedNotification(
        groupId: String,
        groupName: String,
        newMemberId: String,
        newMemberName: String
    ) {
        firestore.collection("groups")
            .document(groupId)
            .collection("members")
            .get()
            .addOnSuccessListener { members ->
                for (memberDoc in members) {
                    val memberId = memberDoc.id
                    if (memberId == newMemberId) continue

                    val notif = hashMapOf(
                        "type"         to "user_joined_group",
                        "title"        to "👋 Nouveau membre",
                        "message"      to "$newMemberName a rejoint le groupe \"$groupName\"",
                        "groupId"      to groupId,
                        "groupName"    to groupName,
                        "postId"       to "",
                        "locationName" to "",
                        "tag"          to "",
                        "senderId"     to newMemberId,
                        "senderName"   to newMemberName,
                        "isRead"       to false,
                        "status"       to "accepted",
                        "timestamp"    to System.currentTimeMillis()
                    )

                    firestore.collection("users")
                        .document(memberId)
                        .collection("notifications")
                        .add(notif)
                }
            }
    }

    private fun sendInvitationAcceptedNotification(
        originalNotification: NotificationItem,
        currentUserName: String
    ) {
        val notif = hashMapOf(
            "type"         to "invitation_accepted",
            "title"        to "✅ Invitation acceptée",
            "message"      to "$currentUserName a accepté votre invitation à rejoindre \"${originalNotification.groupName}\"",
            "groupId"      to originalNotification.groupId,
            "groupName"    to originalNotification.groupName,
            "postId"       to "",
            "locationName" to "",
            "tag"          to "",
            "senderId"     to (auth.currentUser?.uid ?: ""),
            "senderName"   to currentUserName,
            "isRead"       to false,
            "status"       to "accepted",
            "timestamp"    to System.currentTimeMillis()
        )

        firestore.collection("users")
            .document(originalNotification.senderId)
            .collection("notifications")
            .add(notif)
    }
}