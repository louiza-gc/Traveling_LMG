package com.example.traveling.TravelShare.Groups

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.traveling.R
import java.io.File

class GroupAdapter(
    private val groups: List<GroupItem>,
    private val onGroupClick: (GroupItem) -> Unit
) : RecyclerView.Adapter<GroupAdapter.GroupViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(groups[position])
    }

    override fun getItemCount() = groups.size

    inner class GroupViewHolder(itemView: android.view.View) :
        RecyclerView.ViewHolder(itemView) {

        private val ivGroupAvatar: ImageView = itemView.findViewById(R.id.ivGroupAvatar)
        private val tvGroupName: TextView = itemView.findViewById(R.id.tvGroupName)
        private val tvGroupInfo: TextView = itemView.findViewById(R.id.tvGroupInfo)

        fun bind(group: GroupItem) {
            tvGroupName.text = group.name
            tvGroupInfo.text = "👥 ${group.memberCount} membre${if (group.memberCount > 1) "s" else ""}"

            // Charger depuis le chemin local
            val photoFile = group.photoPath.let { File(it) }
            if (group.photoPath.isNotEmpty() && photoFile.exists()) {
                Glide.with(itemView.context)
                    .load(photoFile)
                    .placeholder(R.drawable.ic_group_default)
                    .into(ivGroupAvatar)
            } else {
                ivGroupAvatar.setImageResource(R.drawable.ic_group_default)
            }

            itemView.setOnClickListener {
                onGroupClick(group)
            }
        }
    }
}