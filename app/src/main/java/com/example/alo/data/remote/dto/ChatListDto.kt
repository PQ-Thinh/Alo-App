package com.example.alo.data.remote.dto

import com.example.alo.domain.model.ChatList
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatListDto(
    @SerialName("current_user_id") val currentUserId: String,
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("is_group") val isGroup: Boolean,
    @SerialName("last_message_preview") val lastMessagePreview: String?,
    @SerialName("last_message_time") val lastMessageTime: String?,
    @SerialName("last_message_sender_id") val lastMessageSenderId: String? = null,
    @SerialName("unread_count") val unreadCount: Int,
    @SerialName("chat_name") val chatName: String?,
    @SerialName("chat_avatar") val chatAvatar: String?,
    @SerialName("target_user_id")
    val targetUserId: String? = null,
    @SerialName("target_last_seen")
    val targetLastSeen: String? = null
) {
    fun toDomain(): ChatList {
        return ChatList(
            currentUserId = currentUserId,
            conversationId = conversationId,
            isGroup = isGroup,
            lastMessagePreview = lastMessagePreview,
            lastMessageTime = lastMessageTime,
            unreadCount = unreadCount,
            chatName = chatName,
            chatAvatar = chatAvatar,
            lastMessageSenderId = lastMessageSenderId
        )
    }
}