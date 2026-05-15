package com.example.traveling.TravelShare.Notification

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.traveling.R

class NotificationAdapter(
    private val notifications: List<NotificationItem>,
    private val onItemClick: (NotificationItem, Int) -> Unit,
    private val onAccept: (NotificationItem, Int) -> Unit,
    private val onDecline: (NotificationItem, Int) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(notifications[position], position)
    }

    override fun getItemCount() = notifications.size

    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvTitle: TextView = itemView.findViewById(R.id.tvNotificationTitle)
        private val tvMessage: TextView = itemView.findViewById(R.id.tvNotificationMessage)
        private val tvTime: TextView = itemView.findViewById(R.id.tvNotificationTime)
        private val llActionButtons: LinearLayout = itemView.findViewById(R.id.llActionButtons)
        private val btnAccept: Button = itemView.findViewById(R.id.btnAccept)
        private val btnDecline: Button = itemView.findViewById(R.id.btnDecline)

        fun bind(notification: NotificationItem, position: Int) {
            tvTitle.text = notification.title
            tvMessage.text = notification.message
            tvTime.text = getTimeAgo(notification.timestamp)

            // Afficher les boutons seulement pour les invitations en attente
            if (notification.type == "group_invitation" && notification.status == "pending") {
                llActionButtons.visibility = View.VISIBLE

                btnAccept.setOnClickListener {
                    onAccept(notification, position)
                }

                btnDecline.setOnClickListener {
                    onDecline(notification, position)
                }
            } else {
                llActionButtons.visibility = View.GONE
            }

            // Style pour les notifications non lues
            if (!notification.isRead) {
                itemView.setBackgroundColor(itemView.context.getColor(R.color.notification_unread))
            } else {
                itemView.setBackgroundColor(itemView.context.getColor(android.R.color.white))
            }

            itemView.setOnClickListener {
                onItemClick(notification, position)
            }
        }

        private fun getTimeAgo(timestamp: Long): String {
            val diff = System.currentTimeMillis() - timestamp
            return when {
                diff < 60000 -> "À l'instant"
                diff < 3600000 -> "Il y a ${diff / 60000} min"
                diff < 86400000 -> "Il y a ${diff / 3600000} h"
                else -> "Il y a ${diff / 86400000} j"
            }
        }
    }
}