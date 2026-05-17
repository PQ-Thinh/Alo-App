package com.example.alo.domain.repository

import com.example.alo.domain.model.ChatList
import com.example.alo.domain.model.Conversation
import kotlinx.coroutines.flow.Flow

interface ConversationRepository {
    suspend fun getChatList(currentUserId: String): List<ChatList>
    suspend fun getConversations(userId: String): List<Conversation>
    suspend fun getOrCreateDirectConversation(currentUserId: String, targetUserId: String): String?
    suspend fun createGroupConversation(
        name: String,
        avatarUrl: String?,
        userIds: List<String>,
        encryptedKeys: Map<String, String>
    ): ChatList?
    suspend fun resetUnreadCount(conversationId: String, userId: String)
    fun subscribeToChatListUpdates(currentUserId: String): Flow<Unit>
    suspend fun updateGroupMetadata(conversationId: String, name: String?, avatarUrl: String?, status: String?)
    suspend fun deleteConversation(conversationId: String)
}
