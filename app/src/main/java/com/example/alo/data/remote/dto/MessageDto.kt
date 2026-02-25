package com.example.alo.data.remote.dto

import com.example.alo.domain.model.Message
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MessageDto(
    @SerialName("id") val id: String,
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("sender_id") val senderId: String? = null,
    @SerialName("reply_to_id") val replyToId: String? = null,
    @SerialName("encrypted_content") val encryptedContent: String,
    @SerialName("message_type") val messageType: String = "text",
    @SerialName("is_edited") val isEdited: Boolean = false,
    @SerialName("seen_by") val seenBy: List<String> = emptyList(),
    @SerialName("created_at") val createdAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null
) {
    fun toDomain(): Message = Message(
        id, conversationId, senderId, replyToId, encryptedContent,
        messageType, isEdited, seenBy, createdAt, deletedAt
    )
}