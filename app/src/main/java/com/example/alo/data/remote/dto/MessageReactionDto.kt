package com.example.alo.data.remote.dto

import com.example.alo.domain.model.MessageReaction
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MessageReactionDto(
    @SerialName("message_id") val messageId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("reaction_icon") val reactionIcon: String,
    @SerialName("created_at") val createdAt: String
) {
    fun toDomain(): MessageReaction = MessageReaction(messageId, userId, reactionIcon, createdAt)
}