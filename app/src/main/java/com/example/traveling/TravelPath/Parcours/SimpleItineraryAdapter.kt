package com.example.traveling.TravelPath.Parcours

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.traveling.R

class SimpleItineraryAdapter(
    private val items: List<Itinerary>,
    private val onItemClick: (Itinerary) -> Unit,
    private val onDeleteClick: (Itinerary, Int) -> Unit
) : RecyclerView.Adapter<SimpleItineraryAdapter.ViewHolder>() {

    class ViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(android.R.id.text1)
        val description: TextView = itemView.findViewById(android.R.id.text2)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete_itinerary)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_simple_itinerary, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.name.text = item.name
        holder.description.text = item.description
        holder.itemView.setOnClickListener { onItemClick(item) }
        holder.btnDelete.setOnClickListener { onDeleteClick(item, position) }
    }

    override fun getItemCount() = items.size
}