package com.example.traveling.TravelShare.Notification

data class NotificationItem(
    val id: String,
    val type: String,
    val title: String,
    val message: String,
    val groupId: String = "",
    val groupName: String = "",
    val postId: String = "",
    val locationName: String = "",
    val tag: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val isRead: Boolean = false,
    val status: String = "pending",
    val timestamp: Long = 0
)