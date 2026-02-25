package com.example.alo.domain.model

data class MessageReaction(
    val messageId: String,
    val userId: String,
    val reactionIcon: String,
    val createdAt: String
)