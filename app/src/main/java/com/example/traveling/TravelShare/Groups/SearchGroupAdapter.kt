package com.example.traveling.TravelShare.Groups

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.traveling.R
import de.hdodenhof.circleimageview.CircleImageView
import java.io.File

data class SearchGroupItem(
    val id: String,
    val name: String,
    val description: String,
    val memberCount: Int,
    val photoPath: String,
    val isMember: Boolean
)

class SearchGroupAdapter(
    private val groups: List<SearchGroupItem>,
    private val onJoinClick: (SearchGroupItem, Int) -> Unit
) : RecyclerView.Adapter<SearchGroupAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_group, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(groups[position], position)
    }

    override fun getItemCount() = groups.size

    inner class ViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val ivGroupPhoto: CircleImageView = itemView.findViewById(R.id.ivGroupPhoto)
        private val tvGroupName: TextView = itemView.findViewById(R.id.tvGroupName)
        private val tvGroupInfo: TextView = itemView.findViewById(R.id.tvGroupInfo)
        private val btnJoin: Button = itemView.findViewById(R.id.btnJoinGroup)

        fun bind(group: SearchGroupItem, position: Int) {
            tvGroupName.text = group.name
            tvGroupInfo.text = "👥 ${group.memberCount} membre${if (group.memberCount > 1) "s" else ""}"

            // Photo du groupe
            if (group.photoPath.isNotEmpty()) {
                val file = File(group.photoPath)
                if (file.exists()) {
                    Glide.with(itemView.context)
                        .load(file)
                        .placeholder(R.drawable.ic_group_default)
                        .into(ivGroupPhoto)
                } else {
                    ivGroupPhoto.setImageResource(R.drawable.ic_group_default)
                }
            } else {
                ivGroupPhoto.setImageResource(R.drawable.ic_group_default)
            }

            // Bouton Rejoindre
            if (group.isMember) {
                btnJoin.text = "Membre"
                btnJoin.isEnabled = false
                btnJoin.alpha = 0.5f
            } else {
                btnJoin.text = "➕ Rejoindre"
                btnJoin.isEnabled = true
                btnJoin.alpha = 1f
                btnJoin.setOnClickListener {
                    onJoinClick(group, position)
                }
            }
        }
    }
}