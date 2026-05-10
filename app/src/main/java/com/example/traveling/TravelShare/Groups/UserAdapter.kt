package com.example.traveling.TravelShare.Groups

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.traveling.R
import de.hdodenhof.circleimageview.CircleImageView

data class UserItem(
    val userId: String,
    val name: String,
    val avatar: String
)

class UserAdapter(
    private val users: List<UserItem>,
    private val onInviteClick: (UserItem) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_invite, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount() = users.size

    inner class UserViewHolder(itemView: android.view.View) :
        RecyclerView.ViewHolder(itemView) {

        private val ivAvatar: CircleImageView = itemView.findViewById(R.id.ivUserAvatar)
        private val tvName: TextView = itemView.findViewById(R.id.tvUserName)
        private val btnInvite: Button = itemView.findViewById(R.id.btnInvite)

        fun bind(user: UserItem) {
            tvName.text = user.name

            if (user.avatar.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(user.avatar)
                    .placeholder(R.drawable.ic_default_avatar)
                    .into(ivAvatar)
            }

            btnInvite.setOnClickListener {
                onInviteClick(user)
            }
        }
    }
}