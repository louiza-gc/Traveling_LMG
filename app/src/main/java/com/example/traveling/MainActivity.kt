package com.example.traveling

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.traveling.TravelShare.Acceuil.feed_page
import com.example.traveling.TravelShare.Profil.account_profile
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val nav = findViewById<BottomNavigationView>(R.id.bottomNav)

        // écran par défaut
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, feed_page())
            .commit()

        nav.setOnItemSelectedListener {

            when (it.itemId) {

                R.id.nav_home -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container, feed_page())
                        .commit()
                }

                R.id.nav_profile -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container, account_profile())
                        .commit()
                }

                R.id.nav_groups -> {
                    // GroupsFragment()
                }

                R.id.nav_publish -> {
                    // PublishFragment()
                }

                R.id.nav_notifications -> {
                    // NotificationsFragment()
                }
            }

            true
        }
    }

    override fun onResume() {
        super.onResume()

        val nav = findViewById<BottomNavigationView>(R.id.bottomNav)

        // garder l'onglet actif correct
        nav.selectedItemId = R.id.nav_home
    }
}