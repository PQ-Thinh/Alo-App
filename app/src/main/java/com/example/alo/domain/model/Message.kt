package com.example.alo.domain.model

data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String?, // Có thể null nếu user bị xóa
    val replyToId: String?,
    val encryptedContent: String,
    val messageType: String, // text, image, file...
    val isEdited: Boolean,
    val seenBy: List<String>, // Danh sách ID những người đã xem
    val createdAt: String,
    val deletedAt: String?
)