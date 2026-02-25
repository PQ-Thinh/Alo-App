package com.example.alo.data.remote.dto

import com.example.alo.domain.model.Conversation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConversationDto(
    @SerialName("id") val id: String,
    @SerialName("is_group") val isGroup: Boolean = false,
    @SerialName("name") val name: String? = null,
    @SerialName("last_message_preview") val lastMessagePreview: String? = null,
    @SerialName("last_message_time") val lastMessageTime: String? = null,
    @SerialName("hidden_pin_hash") val hiddenPinHash: String? = null,
    @SerialName("created_at") val createdAt: String
) {
    fun toDomain(): Conversation = Conversation(
        id, isGroup, name, lastMessagePreview, lastMessageTime, hiddenPinHash, createdAt
    )
}