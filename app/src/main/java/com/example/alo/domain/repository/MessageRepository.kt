package com.example.alo.domain.repository

import com.example.alo.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    suspend fun getMessages(conversationId: String): List<Message>
    suspend fun sendMessage(conversationId: String, senderId: String, content: String)
    suspend fun addReaction(messageId: String, userId: String, reactionIcon: String)
    suspend fun removeReaction(messageId: String, userId: String)
    fun subscribeToNewMessages(conversationId: String, onTyping: (String) -> Unit): Flow<Message>
    suspend fun markMessageAsSeen(messageId: String, userId: String)
    suspend fun sendTypingEvent(conversationId: String, userId: String)

}