package com.example.alo.domain.repository

import com.example.alo.domain.model.ChatList
import com.example.alo.domain.model.Conversation

interface ConversationRepository {
    suspend fun getChatList(currentUserId: String): List<ChatList>
    suspend fun getConversations(userId: String): List<Conversation>
    suspend fun getOrCreateDirectConversation(currentUserId: String, targetUserId: String): String?
}