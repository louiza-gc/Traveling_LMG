package com.example.traveling.TravelPath.Accueil

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.traveling.R

class ItemAdapter(
    private val items: List<Item>,
    private val onItemClick: (Item) -> Unit,
    private val onFavoriteClick: (Item) -> Unit,
    private val onAddClick: (Item) -> Unit
) : RecyclerView.Adapter<ItemAdapter.ViewHolder>() {

    class ViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.image)
        val title: TextView = itemView.findViewById(R.id.title)
        val city: TextView = itemView.findViewById(R.id.city)
        val country: TextView = itemView.findViewById(R.id.country)
        val btnFavorite: ImageButton = itemView.findViewById(R.id.btn_favorite)
        val btnAdd: ImageButton = itemView.findViewById(R.id.btn_add_to_list)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_monument, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.name
        holder.city.text = item.city
        holder.country.text = item.country

        Glide.with(holder.itemView.context)
            .load(item.imageUrl)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_menu_report_image)
            .into(holder.image)

        // Icône favori (cœur vide/plein)
        val favoriteRes = if (item.isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border
        holder.btnFavorite.setImageResource(favoriteRes)

        holder.itemView.setOnClickListener { onItemClick(item) }
        holder.btnFavorite.setOnClickListener {
            onFavoriteClick(item)
            item.isFavorite = !item.isFavorite
            notifyItemChanged(position)
        }
        holder.btnAdd.setOnClickListener {
            onAddClick(item)
        }
    }

    override fun getItemCount() = items.size
}