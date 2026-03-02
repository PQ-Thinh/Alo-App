package com.example.alo.domain.model

data class ChatList(
    val currentUserId: String,
    val conversationId: String,
    val isGroup: Boolean,
    val lastMessagePreview: String?,
    val lastMessageTime: String?,
    val unreadCount: Int,
    val chatName: String?,
    val chatAvatar: String?
)