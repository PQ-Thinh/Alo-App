package com.example.alo.data.repository

import android.util.Log
import com.example.alo.data.remote.dto.AttachmentDto
import com.example.alo.domain.model.Attachment
import com.example.alo.domain.repository.AttachmentRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject

class AttachmentRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : AttachmentRepository {

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
            Log.e("AttachmentRepo", "Lỗi gửi file đính kèm: ${e.message}")
        }
    }

    override suspend fun getAttachments(messageId: String): List<Attachment> {
        return try {
            val dtos = supabaseClient.postgrest["attachments"]
                .select { filter { eq("message_id", messageId) } }
                .decodeList<AttachmentDto>()
            dtos.map { it.toDomain() }
        } catch (e: Exception) {
            Log.e("AttachmentRepo", "Lỗi lấy tệp đính kèm: ${e.message}")
            emptyList()
        }
    }
}