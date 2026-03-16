package com.example.alo.data.repository

import android.util.Log
import com.example.alo.data.remote.dto.MessageDto
import com.example.alo.data.utils.TypingPayload
import com.example.alo.domain.model.Message
import com.example.alo.domain.repository.MessageRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.broadcast
import io.github.jan.supabase.realtime.broadcastFlow
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

    private var activeChannel: RealtimeChannel? = null

    override suspend fun getMessages(conversationId: String): List<Message> {
        return try {
            val dtos = supabaseClient.postgrest["messages"]
                .select(columns = Columns.raw("*, message_reactions(*), attachments(*)")) {
                    filter { eq("conversation_id", conversationId) }
                    order("created_at", order = Order.DESCENDING)
                }
                .decodeList<MessageDto>()
            dtos.map { it.toDomain() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun sendMessage(
        conversationId: String,
        messageType: String,
        senderId: String,
        content: String,
        replyToId: String?
    ): String {
        val messageBody = mapOf(
            "conversation_id" to conversationId,
            "sender_id" to senderId,
            "message_type" to messageType,
            "encrypted_content" to content,
            "reply_to_id" to replyToId
        )
        val insertedMessage = supabaseClient.postgrest["messages"]
            .insert(messageBody) {
                select()
            }
            .decodeSingle<MessageDto>()
        return insertedMessage.id
    }

    override suspend fun addReaction(messageId: String, userId: String, reactionIcon: String) {
        try {
            val params = mapOf(
                "p_message_id" to messageId,
                "p_user_id" to userId,
                "p_icon" to reactionIcon
            )
            supabaseClient.postgrest.rpc("add_message_reaction", params)
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


    override fun subscribeToNewMessages(conversationId: String, onTyping: (String) -> Unit): Flow<Message> = callbackFlow {
        activeChannel?.unsubscribe()
        supabaseClient.realtime.connect()

        val channel = supabaseClient.channel("chat_room_updates_$conversationId"){
            broadcast {
                receiveOwnBroadcasts = false
            }
        }
        activeChannel = channel

        val insertFlow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "messages"
            filter("conversation_id", FilterOperator.EQ, conversationId)
        }


        val updateFlow = channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
            table = "messages"
            filter("conversation_id", FilterOperator.EQ, conversationId)
        }
        val typingFlow = channel.broadcastFlow<TypingPayload>(event = "typing")

        val job = launch {
            insertFlow.collect { action ->
                val newMessageDto = action.decodeRecord<MessageDto>()
                val newMessage = newMessageDto.toDomain()
                send(newMessage)
            }
        }
        val jobUpdate = launch {
            updateFlow.collect { action ->
                try {
                    val rawMsg = action.decodeRecord<MessageDto>()
                    val dtos = supabaseClient.postgrest["messages"]
                        .select(columns = Columns.raw("*, message_reactions(*)")) {
                            filter { eq("id", rawMsg.id) }
                        }
                        .decodeList<MessageDto>()

                    val fullUpdatedMsg = dtos.firstOrNull()?.toDomain()

                    if (fullUpdatedMsg != null) {
                        send(fullUpdatedMsg)
                    }
                } catch (e: Exception) {
                    Log.e("MessageRepo", "Lỗi fetch tin nhắn update realtime: ${e.message}")
                }
            }
        }
        val jobTyping = launch {
            typingFlow.collect { payload ->
                onTyping(payload.user_id)
            }
        }

        channel.subscribe()

        awaitClose {
            job.cancel()
            jobUpdate.cancel()
            jobTyping.cancel()
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

    override suspend fun sendTypingEvent(conversationId: String, userId: String) {
        try {
            activeChannel?.broadcast(event = "typing", message = TypingPayload(userId))
        } catch (e: Exception) {
            Log.e("MessageRepo", "Lỗi gửi typing: ${e.message}")
        }
    }

}