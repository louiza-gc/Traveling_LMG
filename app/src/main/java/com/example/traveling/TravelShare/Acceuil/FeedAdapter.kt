package com.example.traveling.TravelShare.feed

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.traveling.R
import de.hdodenhof.circleimageview.CircleImageView

data class PublicationItem(
    val id: String,
    val authorName: String,
    val authorAvatar: String,
    val location: String,
    val imageUrl: String,
    val likesCount: Int,
    val commentsCount: Int,
    val sharesCount: Int,
    val isLiked: Boolean = false,
    val title: String = "",
    val description: String = "",
    val timestamp: Long = 0,
    val tags: List<String> = emptyList()
)

class FeedAdapter(
    private var publications: List<PublicationItem>,
    private val onLikeClick: (PublicationItem, Int) -> Unit,
    private val onItemClick: (PublicationItem) -> Unit
) : RecyclerView.Adapter<FeedAdapter.ViewHolder>() {

    override fun getItemId(position: Int): Long {
        return publications[position].id.hashCode().toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_publication, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(publications[position], position)
        holder.itemView.setOnClickListener {
            onItemClick(publications[position])
        }
    }

    override fun getItemCount() = publications.size

    fun updateData(newPublications: List<PublicationItem>) {
        publications = newPublications.toList()  // Force une nouvelle liste
        notifyDataSetChanged()
        Log.d("FeedAdapter", "updateData: ${publications.size} publications")
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAuthorAvatar: CircleImageView = itemView.findViewById(R.id.ivAuthorAvatar)
        private val tvAuthorName: TextView = itemView.findViewById(R.id.tvAuthorName)
        private val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        private val ivPostImage: ImageView = itemView.findViewById(R.id.ivPostImage)
        private val ivLike: ImageView = itemView.findViewById(R.id.ivLike)
        private val tvLikeCount: TextView = itemView.findViewById(R.id.tvLikeCount)
        private val tvCommentCount: TextView = itemView.findViewById(R.id.tvCommentCount)
        private val tvShareCount: TextView = itemView.findViewById(R.id.tvShareCount)
        private val layoutLike: View = itemView.findViewById(R.id.layoutLike)

        fun bind(item: PublicationItem, position: Int) {
            tvAuthorName.text = item.authorName
            tvLocation.text = item.location
            tvLikeCount.text = item.likesCount.toString()
            tvCommentCount.text = item.commentsCount.toString()
            tvShareCount.text = item.sharesCount.toString()

            // Charger l'image de la publication
            Glide.with(itemView.context)
                .load(item.imageUrl)
                .placeholder(R.drawable.placeholder_photo)
                .into(ivPostImage)

            // Charger la photo de profil (optionnel, si tu as une URL)
            if (item.authorAvatar.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(item.authorAvatar)
                    .placeholder(R.drawable.ic_default_avatar)
                    .into(ivAuthorAvatar)
            }

            // Icône like (cœur plein ou vide)
            val likeIcon = if (item.isLiked) R.drawable.ic_like else R.drawable.ic_dislike
            ivLike.setImageResource(likeIcon)

            // Click like
            layoutLike.setOnClickListener {
                onLikeClick(item, position)
            }
        }
    }
}