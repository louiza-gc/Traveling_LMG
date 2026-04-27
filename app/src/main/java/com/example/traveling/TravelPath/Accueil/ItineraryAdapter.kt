package com.example.traveling.TravelPath.Accueil

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.traveling.R

class ItineraryAdapter(
    private val items: List<Itinerary>,
    private val onItemClick: (Itinerary) -> Unit
) : RecyclerView.Adapter<ItineraryAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.itinerary_name)
        val desc: TextView = view.findViewById(R.id.itinerary_desc)
        val cost: TextView = view.findViewById(R.id.itinerary_cost)
        val duration: TextView = view.findViewById(R.id.itinerary_duration)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_itinerary, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.name.text = item.name
        holder.desc.text = item.description
        holder.cost.text = "%.2f €".format(item.totalCost)
        holder.duration.text = "${item.totalDurationMinutes} min"
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount() = items.size
}