package com.example.alo.domain.repository

import com.example.alo.domain.model.Attachment

interface AttachmentRepository {
    suspend fun uploadImage(byteArray: ByteArray, fileName: String): String
    suspend fun sendAttachment(messageId: String?, fileUrl: String, fileType: String, fileName: String, fileSize: Int)
    suspend fun getAttachments(messageId: String): List<Attachment>
    suspend fun uploadDocument(byteArray: ByteArray, fileName: String): String
}