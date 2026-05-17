package com.example.traveling.TravelPath.Parcours

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.traveling.R
import com.example.traveling.TravelPath.Accueil.PlaceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ItineraryWithImageAdapter(
    private val items: List<Itinerary>,
    private val onItemClick: (Itinerary) -> Unit,
    private val onDeleteClick: (Itinerary, Int) -> Unit
) : RecyclerView.Adapter<ItineraryWithImageAdapter.ViewHolder>() {

    class ViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.iv_itinerary_image)
        val name: TextView = itemView.findViewById(R.id.tv_itinerary_name)
        val desc: TextView = itemView.findViewById(R.id.tv_itinerary_desc)
        val count: TextView = itemView.findViewById(R.id.tv_itinerary_count)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete_itinerary)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_itinerary_with_image, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.name.text = item.name
        holder.desc.text = item.description
        holder.count.text = "${item.placeIds.size} lieu(x)"
        holder.itemView.setOnClickListener { onItemClick(item) }
        holder.btnDelete.setOnClickListener { onDeleteClick(item, position) }

        // Charger l'image du premier lieu du parcours
        if (item.placeIds.isNotEmpty()) {
            GlobalScope.launch {
                val allPlaces = withContext(Dispatchers.IO) { PlaceRepository.loadAllPlaces() }
                val firstPlace = allPlaces.find { it.id == item.placeIds.first() }
                val url = firstPlace?.media?.thumbnail ?: ""
                withContext(Dispatchers.Main) {
                    if (url.isNotBlank()) {
                        Glide.with(holder.itemView.context).load(url).into(holder.image)
                    } else {
                        holder.image.setImageResource(android.R.drawable.ic_menu_gallery)
                    }
                }
            }
        } else {
            holder.image.setImageResource(android.R.drawable.ic_menu_gallery)
        }
    }

    override fun getItemCount() = items.size
}