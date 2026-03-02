package com.example.alo.data.repository

import android.util.Log
import com.example.alo.data.remote.dto.ParticipantDto
import com.example.alo.domain.model.Participant
import com.example.alo.domain.repository.ParticipantRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject

class ParticipantRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : ParticipantRepository {

    override suspend fun getParticipants(conversationId: String): List<Participant> {
        return try {
            val dtos = supabaseClient.postgrest["participants"]
                .select { filter { eq("conversation_id", conversationId) } }
                .decodeList<ParticipantDto>()
            dtos.map { it.toDomain() }
        } catch (e: Exception) {
            Log.e("ParticipantRepo", "Lỗi lấy danh sách thành viên: ${e.message}")
            emptyList()
        }
    }

    override suspend fun addParticipant(conversationId: String, userId: String, role: String) {
        try {
            val participantBody = mapOf(
                "conversation_id" to conversationId,
                "user_id" to userId,
                "role" to role
            )
            supabaseClient.postgrest["participants"].insert(participantBody)
        } catch (e: Exception) {
            Log.e("ParticipantRepo", "Lỗi thêm thành viên: ${e.message}")
        }
    }
}