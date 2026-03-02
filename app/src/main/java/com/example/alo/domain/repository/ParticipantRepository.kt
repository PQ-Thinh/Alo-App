package com.example.alo.domain.repository

import com.example.alo.domain.model.Participant

interface ParticipantRepository {
    suspend fun getParticipants(conversationId: String): List<Participant>
    suspend fun addParticipant(conversationId: String, userId: String, role: String = "member")
}