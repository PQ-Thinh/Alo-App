package com.example.alo.domain.repository

import com.example.alo.domain.model.Participant

interface ParticipantRepository {
    suspend fun getParticipants(conversationId: String): List<Participant>
    suspend fun getParticipant(conversationId: String, userId: String): Participant?
    suspend fun addParticipant(conversationId: String, userId: String, role: String = "member", encryptedGroupKey: String? = null)
    suspend fun removeParticipant(conversationId: String, userId: String)
    suspend fun updateParticipantRole(conversationId: String, userId: String, role: String)
    suspend fun updateEncryptedGroupKey(conversationId: String, userId: String, encryptedGroupKey: String)
    suspend fun setNeedsKeyRewrap(conversationId: String, userId: String, needsRewrap: Boolean)
    suspend fun getParticipantsNeedingRewrap(userId: String): List<Participant>
}
