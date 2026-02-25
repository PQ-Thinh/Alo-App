package com.example.alo.data.remote.dto

import com.example.alo.domain.model.Participant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ParticipantDto(
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("role") val role: String = "member",
    @SerialName("unread_count") val unreadCount: Int = 0,
    @SerialName("encrypted_group_key") val encryptedGroupKey: String? = null,
    @SerialName("joined_at") val joinedAt: String
) {
    fun toDomain(): Participant = Participant(
        conversationId, userId, role, unreadCount, encryptedGroupKey, joinedAt
    )
}