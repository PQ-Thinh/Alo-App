package com.example.alo.domain.repositories

import com.example.alo.domain.model.MessageReaction


interface MessageReactionRepositories{
    suspend fun getMessageReactions(messageId: String): List<MessageReaction>
}