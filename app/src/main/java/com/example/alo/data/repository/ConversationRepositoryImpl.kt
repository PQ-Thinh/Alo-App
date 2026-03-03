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
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
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
    override suspend fun getOrCreateDirectConversation(currentUserId: String, targetUserId: String): String? {
        return try {
            val params = mapOf(
                "p_user_id_1" to currentUserId,
                "p_user_id_2" to targetUserId
            )
            val response = supabaseClient.postgrest.rpc("get_or_create_direct_conversation", params)

            val conversationId = response.decodeAs<String>()

            Log.d("ConversationRepo", "Đã lấy/tạo phòng chat ID: $conversationId")
            conversationId
        } catch (e: Exception) {
            Log.e("ConversationRepo", "Lỗi tạo phòng chat: ${e.message}")
            null
        }
    }

    override suspend fun resetUnreadCount(conversationId: String, userId: String) {
        try {
            val params = mapOf(
                "conv_id" to conversationId,
                "u_id" to userId
            )
            supabaseClient.postgrest.rpc("reset_unread_count", params)
        } catch (e: Exception) {
            Log.e("ConversationRepo", "Lỗi reset unread_count: ${e.message}")
        }
    }

    override fun subscribeToChatListUpdates(currentUserId: String): Flow<Unit> = callbackFlow {
        val channel = supabaseClient.channel("chat_list_update_$currentUserId")

        val insertMessageFlow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "messages"
        }

        val job = launch {
            insertMessageFlow.collect {
                trySend(Unit)
            }
        }

        channel.subscribe()

        awaitClose {
            job.cancel()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    supabaseClient.realtime.removeChannel(channel)
                } catch (e: Exception) {
                    Log.e("ConversationRepo", "Lỗi đóng channel: ${e.message}")
                }
            }
        }
    }
}