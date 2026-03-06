package com.example.alo.domain.model

data class ChatList(
    val currentUserId: String,
    val conversationId: String,
    val isGroup: Boolean,
    val lastMessagePreview: String?,
    val lastMessageTime: String?,
    val lastMessageSenderId: String?,
    val unreadCount: Int,
    val chatName: String?,
    val chatAvatar: String?,
    val targetUserId: String? = null,
    val targetLastSeen: String? = null
)