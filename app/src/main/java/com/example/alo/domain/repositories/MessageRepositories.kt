package com.example.alo.domain.repositories

import com.example.alo.domain.model.Message

interface MessageRepositories {
    suspend fun getMessage(messageId: String): Message?
}