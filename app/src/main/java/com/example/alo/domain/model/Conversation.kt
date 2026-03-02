package com.example.alo.domain.model

data class Conversation(
    val id: String,
    val isGroup: Boolean,
    val name: String?,
    val avatarId: String,
    val avatarUrl: String?,
    val lastMessagePreview: String?,
    val lastMessageTime: String?,
    val hiddenPinHash: String?,
    val createdAt: String
)