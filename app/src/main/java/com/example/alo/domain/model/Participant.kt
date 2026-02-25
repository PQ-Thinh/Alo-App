package com.example.alo.domain.model

data class Participant(
    val conversationId: String,
    val userId: String,
    val role: String,
    val unreadCount: Int,
    val encryptedGroupKey: String?,
    val joinedAt: String
)