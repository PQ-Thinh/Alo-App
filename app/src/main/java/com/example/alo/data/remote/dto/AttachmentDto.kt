package com.example.alo.data.remote.dto

import com.example.alo.domain.model.Attachment
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AttachmentDto(
    @SerialName("id") val id: String,
    @SerialName("message_id") val messageId: String,
    @SerialName("file_url") val fileUrl: String,
    @SerialName("file_type") val fileType: String? = null,
    @SerialName("file_name") val fileName: String? = null,
    @SerialName("file_size") val fileSize: Int? = null,
    @SerialName("created_at") val createdAt: String
) {
    fun toDomain(): Attachment = Attachment(
        id, messageId, fileUrl, fileType, fileName, fileSize, createdAt
    )
}