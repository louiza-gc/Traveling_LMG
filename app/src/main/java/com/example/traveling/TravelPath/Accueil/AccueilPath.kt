package com.example.traveling.TravelPath.Accueil

import android.os.Bundle
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.traveling.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

class AccueilPath : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accueil_path)

        val recycler = findViewById<RecyclerView>(R.id.recyclerCategories)
        val fab = findViewById<FloatingActionButton>(R.id.fab_generate_home)
        fab.setOnClickListener {
            val intent = Intent(this, PreferencesActivity::class.java)
            // Vous pouvez passer une catégorie par défaut ou null pour que l'utilisateur choisisse
            startActivity(intent)
        }
        val categories = listOf(
            Category("Nature", R.drawable.nature),
            Category("Monuments", R.drawable.monuments),
            Category("Musées", R.drawable.musee),
            Category("Restaurants", R.drawable.food),
            Category("Vie nocturne", R.drawable.nightlife)
        )

        recycler.layoutManager = GridLayoutManager(this, 2)
        recycler.adapter = CategoryAdapter(categories)
        recycler.setHasFixedSize(true)
    }
}