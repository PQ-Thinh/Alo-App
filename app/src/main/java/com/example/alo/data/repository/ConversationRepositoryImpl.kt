package com.example.alo.data.repository

import android.util.Log
import com.example.alo.data.remote.dto.ChatListDto
import com.example.alo.data.remote.dto.ConversationDto
import com.example.alo.domain.model.ChatList
import com.example.alo.domain.model.Conversation
import com.example.alo.domain.repository.ConversationRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject

class ConversationRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : ConversationRepository {

    override suspend fun getChatList(currentUserId: String): List<ChatList> {
        return supabaseClient.postgrest["chat_list_view"]
            .select {
                filter { eq("current_user_id", currentUserId) }
                order("last_message_time", Order.DESCENDING)
            }
            .decodeList<ChatListDto>()
            .map { it.toDomain() }
    }

    override suspend fun getConversations(userId: String): List<Conversation> {
        return try {
            val dtos = supabaseClient.postgrest["conversations"]
                .select(columns = Columns.raw("*, participants!inner(user_id)")) {
                    filter { eq("participants.user_id", userId) }
                }
                .decodeList<ConversationDto>()
            dtos.map { it.toDomain() }
        } catch (e: Exception) {
            Log.e("ConversationRepo", "Lỗi lấy danh sách phòng chat: ${e.message}")
            emptyList()
        }
    }
}