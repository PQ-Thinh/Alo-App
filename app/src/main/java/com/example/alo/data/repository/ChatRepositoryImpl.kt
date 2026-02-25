package com.example.alo.data.repository

import android.util.Log
import com.example.alo.data.remote.dto.AttachmentDto
import com.example.alo.data.remote.dto.ConversationDto
import com.example.alo.data.remote.dto.MessageDto
import com.example.alo.data.remote.dto.ParticipantDto
import com.example.alo.domain.model.Attachment
import com.example.alo.domain.model.Conversation
import com.example.alo.domain.model.Message
import com.example.alo.domain.model.Participant
import com.example.alo.domain.repositories.ChatRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : ChatRepository {

    override suspend fun getConversations(userId: String): List<Conversation> {
        return try {
            val dtos = supabaseClient.postgrest["conversations"]
                .select(columns = Columns.raw("*, participants!inner(user_id)")) {
                    filter {
                        eq("participants.user_id", userId)
                    }
                }
                .decodeList<ConversationDto>()
            dtos.map { it.toDomain() }
        } catch (e: Exception) {
            Log.e("ChatRepo", "Lỗi lấy danh sách phòng chat: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getMessages(conversationId: String): List<Message> {
        return try {
            val dtos = supabaseClient.postgrest["messages"]
                .select {
                    filter { eq("conversation_id", conversationId) }
                    order("created_at", order = Order.DESCENDING) // Load tin nhắn mới nhất trước
                }
                .decodeList<MessageDto>()
            dtos.map { it.toDomain() }
        } catch (e: Exception) {
            Log.e("ChatRepo", "Lỗi lấy tin nhắn: ${e.message}")
            emptyList()
        }
    }

    override suspend fun sendMessage(conversationId: String, senderId: String, content: String) {
        try {
            val messageBody = mapOf(
                "conversation_id" to conversationId,
                "sender_id" to senderId,
                "encrypted_content" to content // Tạm thời đẩy raw, sau này bạn nhúng logic mã hóa vào đây
            )
            supabaseClient.postgrest["messages"].insert(messageBody)
        } catch (e: Exception) {
            Log.e("ChatRepo", "Lỗi gửi tin nhắn: ${e.message}")
        }
    }
    override suspend fun sendAttachment(messageId: String, fileUrl: String, fileType: String, fileName: String, fileSize: Int) {
        try {
            val attachmentBody = mapOf(
                "message_id" to messageId,
                "file_url" to fileUrl,
                "file_type" to fileType,
                "file_name" to fileName,
                "file_size" to fileSize
            )
            supabaseClient.postgrest["attachments"].insert(attachmentBody)
        } catch (e: Exception) {
            Log.e("ChatRepo", "Lỗi gửi file đính kèm: ${e.message}")
        }
    }

    override suspend fun getAttachments(messageId: String): List<Attachment> {
        return try {
            val dtos = supabaseClient.postgrest["attachments"]
                .select { filter { eq("message_id", messageId) } }
                .decodeList<AttachmentDto>()
            dtos.map { it.toDomain() }
        } catch (e: Exception) {
            Log.e("ChatRepo", "Lỗi lấy tệp đính kèm: ${e.message}")
            emptyList()
        }
    }

    // --- Bảng Message_Reactions ---
    override suspend fun addReaction(messageId: String, userId: String, reactionIcon: String) {
        try {
            val reactionBody = mapOf(
                "message_id" to messageId,
                "user_id" to userId,
                "reaction_icon" to reactionIcon
            )
            supabaseClient.postgrest["message_reactions"].upsert(reactionBody)
        } catch (e: Exception) {
            Log.e("ChatRepo", "Lỗi thả cảm xúc: ${e.message}")
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
            Log.e("ChatRepo", "Lỗi thu hồi cảm xúc: ${e.message}")
        }
    }

    // --- Bảng Participants ---
    override suspend fun getParticipants(conversationId: String): List<Participant> {
        return try {
            val dtos = supabaseClient.postgrest["participants"]
                .select { filter { eq("conversation_id", conversationId) } }
                .decodeList<ParticipantDto>()
            dtos.map { it.toDomain() }
        } catch (e: Exception) {
            Log.e("ChatRepo", "Lỗi lấy danh sách thành viên: ${e.message}")
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
            Log.e("ChatRepo", "Lỗi thêm thành viên: ${e.message}")
        }
    }
}