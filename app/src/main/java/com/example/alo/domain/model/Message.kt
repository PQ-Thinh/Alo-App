package com.example.alo.domain.model

data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String?,
    val replyToId: String?,
    val encryptedContent: String,
    val rawEncryptedContent: String = "",
    val messageType: String,
    val isEdited: Boolean,
    val seenBy: List<String>,
    val createdAt: String,
    val deletedAt: String?,
    val reactions: List<MessageReaction> = emptyList(),
    val attachments: List<Attachment> = emptyList(),
)