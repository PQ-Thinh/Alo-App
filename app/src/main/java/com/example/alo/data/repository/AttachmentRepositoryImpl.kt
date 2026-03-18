package com.example.alo.data.repository

import android.util.Log
import com.example.alo.data.remote.dto.AttachmentDto
import com.example.alo.data.utils.AttachmentInsertDto
import com.example.alo.domain.model.Attachment
import com.example.alo.domain.repository.AttachmentRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import javax.inject.Inject

class AttachmentRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : AttachmentRepository {
    override suspend fun uploadImage(
        byteArray: ByteArray,
        fileName: String
    ): String {
        return try {
            val bucket = supabaseClient.storage["chat_images"]

            bucket.upload(fileName, byteArray)

            val publicUrl = bucket.publicUrl(fileName)
            publicUrl
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun sendAttachment(messageId: String?, fileUrl: String, fileType: String, fileName: String, fileSize: Int) {
        try {
            val attachmentBody = AttachmentInsertDto(
                messageId = messageId,
                fileUrl = fileUrl,
                fileType = fileType,
                fileName = fileName,
                fileSize = fileSize
            )
            supabaseClient.postgrest["attachments"].insert(attachmentBody)
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun getAttachments(messageId: String): List<Attachment> {
        return try {
            val dtos = supabaseClient.postgrest["attachments"]
                .select { filter { eq("message_id", messageId) } }
                .decodeList<AttachmentDto>()
            dtos.map { it.toDomain() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun uploadDocument(
        byteArray: ByteArray,
        fileName: String
    ): String {
        return try {
            val bucket = supabaseClient.storage["chat_files"]

            bucket.upload(fileName, byteArray)

            val publicUrl = bucket.publicUrl(fileName)
            publicUrl
        } catch (e: Exception) {
            throw e
        }
    }
}