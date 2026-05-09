package com.example.traveling.TravelShare.feed

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.traveling.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

data class CommentItem(
    val id: String,
    val userId: String,
    val authorName: String,
    val authorAvatar: String,
    val text: String,
    val timestamp: Long
)

class CommentAdapter(
    private var comments: List<CommentItem>,
    private val postId: String,
    private val onCommentDeleted: () -> Unit
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(comments[position])
    }

    override fun getItemCount() = comments.size

    fun updateComments(newComments: List<CommentItem>) {
        comments = newComments
        notifyDataSetChanged()
    }

    inner class CommentViewHolder(itemView: android.view.View) :
        RecyclerView.ViewHolder(itemView) {

        private val ivAvatar: CircleImageView = itemView.findViewById(R.id.ivCommentAvatar)
        private val tvAuthor: TextView = itemView.findViewById(R.id.tvCommentAuthor)
        private val tvText: TextView = itemView.findViewById(R.id.tvCommentText)
        private val tvTime: TextView = itemView.findViewById(R.id.tvCommentTime)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteComment)

        fun bind(comment: CommentItem) {
            tvAuthor.text = comment.authorName
            tvText.text = comment.text
            tvTime.text = getTimeAgo(comment.timestamp)

            if (comment.authorAvatar.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(comment.authorAvatar)
                    .placeholder(R.drawable.ic_default_avatar)
                    .into(ivAvatar)
            }

            // Afficher l'icône de suppression uniquement pour l'auteur du commentaire
            if (comment.userId == currentUserId) {
                btnDelete.visibility = android.view.View.VISIBLE
                btnDelete.setOnClickListener {
                    deleteComment(comment)
                }
            } else {
                btnDelete.visibility = android.view.View.GONE
            }
        }

        private fun deleteComment(comment: CommentItem) {
            firestore.collection("photos")
                .document(postId)
                .collection("comments")
                .document(comment.id)
                .delete()
                .addOnSuccessListener {
                    // Décrémenter le compteur de commentaires
                    firestore.collection("photos")
                        .document(postId)
                        .update("commentsCount", com.google.firebase.firestore.FieldValue.increment(-1))

                    Toast.makeText(itemView.context, "Commentaire supprimé", Toast.LENGTH_SHORT).show()
                    onCommentDeleted()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(itemView.context, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        private fun getTimeAgo(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < 60000 -> "À l'instant"
                diff < 3600000 -> "${diff / 60000} min"
                diff < 86400000 -> "${diff / 3600000} h"
                diff < 604800000 -> "${diff / 86400000} j"
                else -> {
                    val date = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
                    date.format(Date(timestamp))
                }
            }
        }
    }
}