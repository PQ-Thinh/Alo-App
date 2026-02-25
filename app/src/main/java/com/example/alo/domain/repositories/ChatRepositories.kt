package com.example.alo.domain.repositories

import com.example.alo.domain.model.Attachment
import com.example.alo.domain.model.Conversation
import com.example.alo.domain.model.Message
import com.example.alo.domain.model.Participant

interface ChatRepository {
    suspend fun getConversations(userId: String): List<Conversation>
    suspend fun getMessages(conversationId: String): List<Message>
    suspend fun sendMessage(conversationId: String, senderId: String, content: String)

    // --- Bảng Attachments ---
    suspend fun sendAttachment(messageId: String, fileUrl: String, fileType: String, fileName: String, fileSize: Int)
    suspend fun getAttachments(messageId: String): List<Attachment>

    // --- Bảng Message_Reactions ---
    suspend fun addReaction(messageId: String, userId: String, reactionIcon: String)
    suspend fun removeReaction(messageId: String, userId: String)

    // --- Bảng Participants ---
    suspend fun getParticipants(conversationId: String): List<Participant>
    suspend fun addParticipant(conversationId: String, userId: String, role: String = "member")
}