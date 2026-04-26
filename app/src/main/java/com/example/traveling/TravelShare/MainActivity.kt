package com.example.traveling.TravelShare

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.traveling.R
import com.example.traveling.TravelShare.Acceuil.feed_page
import com.example.traveling.TravelShare.Anonyme.guest_fragment
import com.example.traveling.TravelShare.Profil.account_profile
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private var isGuest = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val nav = findViewById<BottomNavigationView>(R.id.bottomNav)
        isGuest = intent.getBooleanExtra("isGuest", false)

        // écran par défaut
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, feed_page())
            .commit()

        nav.setOnItemSelectedListener {

            when (it.itemId) {

                R.id.nav_home -> {
                    show(feed_page())
                }

                R.id.nav_profile -> {
                    if (isGuest) show(guest_fragment())
                    else show(account_profile())
                }

                R.id.nav_groups -> {
                    if (isGuest) show(guest_fragment())
                    else { /* GroupsFragment */ }
                }

                R.id.nav_publish -> {
                    if (isGuest) show(guest_fragment())
                    else { /* PublishFragment */ }
                }

                R.id.nav_notifications -> {
                    if (isGuest) show(guest_fragment())
                    else { /* NotificationsFragment */ }
                }
            }

            true
        }
    }


    private fun show(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }
}