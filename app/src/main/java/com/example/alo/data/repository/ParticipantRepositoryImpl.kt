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
            val dtos = supabaseClient.postgrest[com.example.alo.core.utils.Constant.TABLE_PARTICIPANTS]
                .select { filter { eq("conversation_id", conversationId) } }
                .decodeList<ParticipantDto>()
            dtos.map { it.toDomain() }
        } catch (e: Exception) {
            Log.e("ParticipantRepo", "Lỗi lấy danh sách thành viên: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getParticipant(conversationId: String, userId: String): Participant? {
        return try {
            val dto = supabaseClient.postgrest[com.example.alo.core.utils.Constant.TABLE_PARTICIPANTS]
                .select { 
                    filter { 
                        and {
                            eq("conversation_id", conversationId)
                            eq("user_id", userId)
                        }
                    }
                }
                .decodeSingle<ParticipantDto>()
            dto.toDomain()
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun addParticipant(conversationId: String, userId: String, role: String, encryptedGroupKey: String?) {
        try {
            val participantBody = mutableMapOf<String, String?>(
                "conversation_id" to conversationId,
                "user_id" to userId,
                "role" to role
            )
            if (encryptedGroupKey != null) {
                participantBody["encrypted_group_key"] = encryptedGroupKey
            }
            supabaseClient.postgrest[com.example.alo.core.utils.Constant.TABLE_PARTICIPANTS].insert(participantBody)
        } catch (e: Exception) {
            Log.e("ParticipantRepo", "Lỗi thêm thành viên: ${e.message}")
            throw e
        }
    }

    override suspend fun removeParticipant(conversationId: String, userId: String) {
        try {
            supabaseClient.postgrest[com.example.alo.core.utils.Constant.TABLE_PARTICIPANTS].delete {
                filter {
                    and {
                        eq("conversation_id", conversationId)
                        eq("user_id", userId)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ParticipantRepo", "Lỗi xóa thành viên: ${e.message}")
            throw e
        }
    }

    override suspend fun updateParticipantRole(conversationId: String, userId: String, role: String) {
        try {
            supabaseClient.postgrest[com.example.alo.core.utils.Constant.TABLE_PARTICIPANTS].update({
                set("role", role)
            }) {
                filter {
                    and {
                        eq("conversation_id", conversationId)
                        eq("user_id", userId)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ParticipantRepo", "Lỗi cập nhật vai trò: ${e.message}")
            throw e
        }
    }
}
