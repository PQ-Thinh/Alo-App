package com.example.alo.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface PresenceRepository {
    val onlineUsers: StateFlow<Set<String>>
    suspend fun subscribeAndTrack(currentUserId: String)
    suspend fun unsubscribe()
}