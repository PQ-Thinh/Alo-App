package com.example.alo.data.repository

import android.util.Log
import com.example.alo.data.remote.dto.MessageDto
import com.example.alo.domain.model.Message
import com.example.alo.domain.repository.MessageRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

class MessageRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : MessageRepository {

    override suspend fun getMessages(conversationId: String): List<Message> {
        return try {
            val dtos = supabaseClient.postgrest["messages"]
                .select {
                    filter { eq("conversation_id", conversationId) }
                    order("created_at", order = Order.DESCENDING)
                }
                .decodeList<MessageDto>()
            dtos.map { it.toDomain() }
        } catch (e: Exception) {
            Log.e("MessageRepo", "Lỗi lấy tin nhắn: ${e.message}")
            emptyList()
        }
    }

    override suspend fun sendMessage(conversationId: String, senderId: String, content: String) {
        try {
            val messageBody = mapOf(
                "conversation_id" to conversationId,
                "sender_id" to senderId,
                "encrypted_content" to content
            )
            supabaseClient.postgrest["messages"].insert(messageBody)
        } catch (e: Exception) {
            Log.e("MessageRepo", "Lỗi gửi tin nhắn: ${e.message}")
        }
    }

    override suspend fun addReaction(messageId: String, userId: String, reactionIcon: String) {
        try {
            val reactionBody = mapOf(
                "message_id" to messageId,
                "user_id" to userId,
                "reaction_icon" to reactionIcon
            )
            supabaseClient.postgrest["message_reactions"].upsert(reactionBody)
        } catch (e: Exception) {
            Log.e("MessageRepo", "Lỗi thả cảm xúc: ${e.message}")
        }
    }

    override suspend fun removeReaction(messageId: String, userId: String) {
        try {
            supabaseClient.postgrest["message_reactions"].delete {
                filter {
                    eq("message_id", messageId)
                    eq("user_id", userId)
                }
            }
        } catch (e: Exception) {
            Log.e("MessageRepo", "Lỗi thu hồi cảm xúc: ${e.message}")
        }
    }

    override fun subscribeToNewMessages(conversationId: String): Flow<Message> = callbackFlow {

//        val globalStatusJob = launch {
//            supabaseClient.realtime.status.collect { status ->
//                Log.e("REALTIME_TEST", "Trạng thái MẠNG TỔNG (WebSocket): $status")
//            }
//        }

        supabaseClient.realtime.connect()

        val channel = supabaseClient.channel("chat_room_updates_$conversationId")

        val insertFlow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "messages"
            filter("conversation_id", FilterOperator.EQ, conversationId)
        }

//        val channelStatusJob = launch {
//            channel.status.collect { status ->
//                Log.e("REALTIME_TEST", "Trạng thái KÊNH CHAT: $status")
//            }
//        }

        val updateFlow = channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
            table = "messages"
            filter("conversation_id", FilterOperator.EQ, conversationId)
        }

        val job = launch {
            insertFlow.collect { action ->
                Log.d("REALTIME_TEST", "1. Đã có sự kiện INSERT trên bảng messages!")
                try {
                    val newMessageDto = action.decodeRecord<MessageDto>()
                    val newMessage = newMessageDto.toDomain()
                    Log.d("REALTIME_TEST", "2. Parse thành công: ${newMessage.encryptedContent}")
                    send(newMessage)
                } catch (e: Exception) {
                    Log.e("REALTIME_TEST", "LỖI PARSE REALTIME: ${e.message}")
                }
            }
        }
        val jobUpdate = launch {
            updateFlow.collect { action ->
                try {
                    val msg = action.decodeRecord<MessageDto>().toDomain()
                    send(msg)
                } catch (e: Exception) {}
            }
        }
        channel.subscribe()

        awaitClose {
//            globalStatusJob.cancel()
//            channelStatusJob.cancel()
            job.cancel()
            jobUpdate.cancel()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    supabaseClient.realtime.removeChannel(channel)
                } catch (e: Exception) {
                    Log.e("MessageRepo", "Lỗi khi đóng channel: ${e.message}")
                }
            }
        }
    }

    override suspend fun markMessageAsSeen(messageId: String, userId: String) {
        try {
            val params = mapOf(
                "p_message_id" to messageId,
                "p_user_id" to userId
            )
            supabaseClient.postgrest.rpc("mark_message_seen", params)
        } catch (e: Exception) {
            Log.e("MessageRepo", "Lỗi mark as seen: ${e.message}")
        }
    }
}