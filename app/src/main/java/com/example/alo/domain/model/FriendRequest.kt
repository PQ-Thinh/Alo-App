package com.example.alo.domain.model

import com.example.alo.domain.helperEnum.FriendRequestStatus

data class FriendRequest(
    val id: String,
    val senderId: String,
    val receiverId: String,
    val status: FriendRequestStatus,
    val createdAt: String
)