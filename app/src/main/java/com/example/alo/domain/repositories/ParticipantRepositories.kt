package com.example.alo.domain.repositories

import com.example.alo.domain.model.Participant

interface ParticipantRepositories {
    suspend fun getParticipants(participantId: String): List<Participant>
}