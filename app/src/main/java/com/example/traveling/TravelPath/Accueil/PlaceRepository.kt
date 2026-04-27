package com.example.traveling.TravelPath.Accueil

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PlaceRepository {
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    suspend fun loadAllPlaces(): List<Place> = withContext(Dispatchers.IO) {
        try {
            val jsonString = appContext.assets.open("places.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<Place>>() {}.type
            Gson().fromJson(jsonString, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}