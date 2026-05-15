package com.example.traveling.TravelShare.Profil

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.traveling.R
import com.example.traveling.TravelShare.feed.PublicationItem

class ProfilePhotoAdapter(
    private var photos: List<PublicationItem>,
    private val onPhotoClick: (PublicationItem) -> Unit,
    private val onPhotoDelete: (PublicationItem, Int) -> Unit
) : RecyclerView.Adapter<ProfilePhotoAdapter.PhotoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profile_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(photos[position], position)
    }

    override fun getItemCount() = photos.size

    fun updateData(newPhotos: List<PublicationItem>) {
        photos = newPhotos
        notifyDataSetChanged()
    }

    inner class PhotoViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val ivPhoto: ImageView = itemView.findViewById(R.id.ivProfilePhoto)
        private val layoutDelete: LinearLayout = itemView.findViewById(R.id.layoutDelete)
        private val btnDelete: ImageView = itemView.findViewById(R.id.btnDeletePhoto)
        private val tvLikeCount: TextView = itemView.findViewById(R.id.tvLikeCount)

        fun bind(photo: PublicationItem, position: Int) {
            Glide.with(itemView.context)
                .load(photo.imageUrl)
                .placeholder(R.drawable.placeholder_photo)
                .into(ivPhoto)

            // Afficher le nombre de likes
            tvLikeCount.text = photo.likesCount.toString()

            // Clic sur la photo → ouvrir le détail
            itemView.setOnClickListener {
                onPhotoClick(photo)
            }

            // Clic sur la corbeille → supprimer
            layoutDelete.setOnClickListener { view ->
                view.animate().scaleX(0.8f).scaleY(0.8f).setDuration(100).withEndAction {
                    view.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                }
                onPhotoDelete(photo, position)
            }
        }
    }
}