package com.example.traveling.TravelPath.Parcours

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.traveling.R
import com.example.traveling.TravelPath.Accueil.Place

class PlacesSelectionAdapter(
    private val places: List<Place>,
    private val selectedIds: MutableSet<String>,
    private val onSelectionChanged: (placeId: String, isSelected: Boolean) -> Unit
) : RecyclerView.Adapter<PlacesSelectionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.place_image)
        val name: TextView = view.findViewById(R.id.place_name)
        val city: TextView = view.findViewById(R.id.place_city)
        val checkBox: CheckBox = view.findViewById(R.id.checkbox_select)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_place_selection, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val place = places[position]
        holder.name.text = place.name
        holder.city.text = place.location.city
        holder.checkBox.isChecked = selectedIds.contains(place.id)
        Glide.with(holder.itemView.context)
            .load(place.media.thumbnail)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .into(holder.image)

        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            onSelectionChanged(place.id, isChecked)
        }
        holder.itemView.setOnClickListener {
            holder.checkBox.isChecked = !holder.checkBox.isChecked
        }
    }

    override fun getItemCount() = places.size
}