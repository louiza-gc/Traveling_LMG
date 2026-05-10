package com.example.traveling.TravelShare.Groups

data class GroupItem(
    val id: String,
    val name: String,
    val description: String = "",
    val memberCount: Int = 0,
    val isMine: Boolean = false,
    val photoPath: String = ""
)