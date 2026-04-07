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
import kotlinx.serialization.json.*
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

            conversationId
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun createGroupConversation(
        name: String,
        avatarUrl: String?,
        userIds: List<String>,
        encryptedKeys: Map<String, String>
    ): ChatList? {
        return try {
            val params = buildJsonObject {
                put("p_name", name)
                put("p_avatar_url", avatarUrl)
                putJsonArray("p_user_ids") {
                    userIds.forEach { add(it) }
                }
                putJsonObject("p_encrypted_keys") {
                    encryptedKeys.forEach { (key, value) ->
                        put(key, value)
                    }
                }
            }
            val response = supabaseClient.postgrest.rpc("create_group_conversation", params)
            val dto = response.decodeSingle<ChatListDto>()
            dto.toDomain()
        } catch (e: Exception) {
            Log.e("ConversationRepo", "Lỗi createGroupConversation: ${e.message}", e)
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
            throw e
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
                    throw e
                }
            }
        }
    }

    override suspend fun updateGroupMetadata(conversationId: String, name: String?, avatarUrl: String?, status: String?) {
        try {
            supabaseClient.postgrest["conversations"]
                .update({
                    if (name != null) set("name", name)
                    if (avatarUrl != null) set("avatar_url", avatarUrl)
                    if (status != null) set("status", status)
                }) {
                    filter { eq("id", conversationId) }
                }
        } catch (e: Exception) {
            Log.e("ConversationRepo", "Lỗi updateGroupMetadata: ${e.message}", e)
            throw e
        }
    }

    override suspend fun deleteConversation(conversationId: String) {
        try {
            supabaseClient.postgrest["conversations"]
                .delete {
                    filter { eq("id", conversationId) }
                }
        } catch (e: Exception) {
            Log.e("ConversationRepo", "Lỗi deleteConversation: ${e.message}", e)
            throw e
        }
    }
}