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
    @SerialName("seen_by") val seenBy: List<String>? = emptyList(),
    @SerialName("created_at") val createdAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null,
    
    // Mới - Quan hệ bảng video_calls
    @SerialName("video_calls")
    val videoCalls: List<VideoCallDto>? = null,

    @SerialName("message_reactions")
    val reactions: List<MessageReactionDto>? = null,
    @SerialName("attachments")
    val attachments: List<AttachmentDto>? = null,
) {
    fun toDomain(): Message {
        val videoCall = videoCalls?.firstOrNull()
        return Message(
            id = id,
            conversationId = conversationId,
            senderId = senderId,
            replyToId = replyToId,
            encryptedContent = encryptedContent,
            rawEncryptedContent = "",
            messageType = messageType,
            isEdited = isEdited,
            seenBy = seenBy ?: emptyList(),
            createdAt = createdAt,
            deletedAt = deletedAt,
            // Ưu tiên lấy từ bảng video_calls
            callDurationSec = videoCall?.durationSec,
            callDirection = videoCall?.direction,
            callVideo = videoCall?.isVideo,
            callReason = videoCall?.endReason,
            reactions = reactions?.map { it.toDomain() } ?: emptyList(),
            attachments = attachments?.map { it.toDomain() } ?: emptyList()
        )
    }
}
