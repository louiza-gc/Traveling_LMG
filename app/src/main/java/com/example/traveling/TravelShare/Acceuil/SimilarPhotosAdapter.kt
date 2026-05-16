package com.example.traveling.TravelShare.Acceuil

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.traveling.R

data class SimilarPhotoItem(
    val id: String,
    val imageUrl: String
)

class SimilarPhotosAdapter(
    private val photos: List<SimilarPhotoItem>,
    private val onPhotoClick: (SimilarPhotoItem) -> Unit
) : RecyclerView.Adapter<SimilarPhotosAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_similar_photo, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(photos[position])
    }

    override fun getItemCount() = photos.size

    inner class ViewHolder(itemView: android.view.View) :
        RecyclerView.ViewHolder(itemView) {

        private val ivPhoto: ImageView = itemView.findViewById(R.id.ivSimilarPhoto)

        fun bind(photo: SimilarPhotoItem) {
            Glide.with(itemView.context)
                .load(photo.imageUrl)
                .placeholder(R.drawable.placeholder_photo)
                .into(ivPhoto)

            itemView.setOnClickListener {
                onPhotoClick(photo)
            }
        }
    }
}