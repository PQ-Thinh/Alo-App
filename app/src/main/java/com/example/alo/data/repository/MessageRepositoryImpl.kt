package com.example.alo.data.repository

import android.util.Log
import com.example.alo.data.remote.dto.MessageDto
import com.example.alo.domain.model.Message
import com.example.alo.domain.repository.MessageRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

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

    override suspend fun getMessagesFlow(conversationId: String): Flow<List<Message>> {
        TODO("Not yet implemented")
    }


}