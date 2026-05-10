package com.example.traveling.TravelShare.Groups

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.traveling.R
import de.hdodenhof.circleimageview.CircleImageView

data class MemberItem(
    val userId: String,
    val name: String,
    val avatar: String,
    val role: String = "member"
)

class MemberAdapter(
    private val members: List<MemberItem>,
    private val isAdmin: Boolean = false,
    private val onRemoveClick: ((MemberItem) -> Unit)? = null
) : RecyclerView.Adapter<MemberAdapter.MemberViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_member, parent, false)
        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.bind(members[position])
    }

    override fun getItemCount() = members.size

    inner class MemberViewHolder(itemView: android.view.View) :
        RecyclerView.ViewHolder(itemView) {

        private val ivAvatar: CircleImageView = itemView.findViewById(R.id.ivMemberAvatar)
        private val tvName: TextView = itemView.findViewById(R.id.tvMemberName)
        private val tvRole: TextView = itemView.findViewById(R.id.tvMemberRole)
        private val btnRemove: ImageView = itemView.findViewById(R.id.btnRemoveMember)

        fun bind(member: MemberItem) {
            tvName.text = member.name
            tvRole.text = if (member.role == "admin") "Admin" else "Membre"

            if (member.avatar.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(member.avatar)
                    .placeholder(R.drawable.ic_default_avatar)
                    .into(ivAvatar)
            }

            if (isAdmin && member.role != "admin") {
                btnRemove.visibility = android.view.View.VISIBLE
                btnRemove.setOnClickListener {
                    onRemoveClick?.invoke(member)
                }
            } else {
                btnRemove.visibility = android.view.View.GONE
            }
        }
    }
}