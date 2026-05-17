package com.example.traveling.TravelShare

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.traveling.R
import com.example.traveling.TravelShare.Acceuil.page_feed
import com.example.traveling.TravelShare.Anonyme.guest_fragment
import com.example.traveling.TravelShare.Groups.GroupsFragment
import com.example.traveling.TravelShare.Notification.NotificationsFragment
import com.example.traveling.TravelShare.Profil.account_profile
import com.example.traveling.TravelShare.Publication.publication_add
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class MainActivity : AppCompatActivity() {

    private var isGuest = false
    private lateinit var nav: BottomNavigationView
    private var notifBadge: BadgeDrawable? = null
    private var unreadListener: ListenerRegistration? = null

    private val firestore     = FirebaseFirestore.getInstance()
    private val auth          = FirebaseAuth.getInstance()
    private val currentUserId get() = auth.currentUser?.uid ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        nav     = findViewById(R.id.bottomNav)
        isGuest = intent.getBooleanExtra("isGuest", false)

        supportFragmentManager.beginTransaction()
            .replace(R.id.container, page_feed())
            .commit()

        // Démarrer l'écoute des notifs non lues si connecté
        if (!isGuest && currentUserId.isNotEmpty()) {
            listenForUnreadNotifications()
        }

        nav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> show(page_feed())

                R.id.nav_profile -> {
                    if (isGuest) show(guest_fragment())
                    else show(account_profile())
                }

                R.id.nav_groups -> {
                    if (isGuest) show(guest_fragment())
                    else show(GroupsFragment())
                }

                R.id.nav_publish -> {
                    if (isGuest) show(guest_fragment())
                    else show(publication_add())
                }

                R.id.nav_notifications -> {
                    if (isGuest) show(guest_fragment())
                    else show(NotificationsFragment())
                }
            }
            true
        }
    }

    // ==================== BADGE ====================

    /**
     * Écoute en temps réel les notifs non lues dans Firestore
     * Met à jour le badge automatiquement
     */
    private fun listenForUnreadNotifications() {
        unreadListener = firestore
            .collection("users")
            .document(currentUserId)
            .collection("notifications")
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val unreadCount = snapshot.size()
                if (unreadCount > 0) {
                    showNotificationBadge(unreadCount)
                } else {
                    clearNotificationBadge()
                }
            }
    }

    private fun showNotificationBadge(count: Int) {
        val badge = nav.getOrCreateBadge(R.id.nav_notifications)
        badge.isVisible = true
        //  Affiche le nombre si <= 99, sinon juste le point rouge
        if (count <= 99) {
            badge.number = count
        } else {
            badge.clearNumber()  // juste le point sans chiffre
        }
        notifBadge = badge
    }

    private fun clearNotificationBadge() {
        nav.removeBadge(R.id.nav_notifications)
        notifBadge = null
    }

    // ==================== NAVIGATION ====================

    private fun show(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }

    // ==================== LIFECYCLE ====================

    override fun onDestroy() {
        super.onDestroy()
        // Stopper le listener Firestore pour éviter les fuites mémoire
        unreadListener?.remove()
    }
}