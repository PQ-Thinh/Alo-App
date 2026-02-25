package com.example.alo.domain.repositories

import com.example.alo.domain.model.Conversation


interface ConversationRepositories{
    suspend fun getConversation(conversationId: String): Conversation?
}