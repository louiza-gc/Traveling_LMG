package com.example.traveling.TravelShare.Groups

data class GroupPostItem(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorAvatar: String,
    val imageUrl: String,
    val caption: String,
    val likesCount: Int,
    val commentsCount: Int,
    val timestamp: Long
)