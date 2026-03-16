package com.example.alo.data.utils

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AttachmentInsertDto(
    @SerialName("message_id") val messageId: String?,
    @SerialName("file_url") val fileUrl: String,
    @SerialName("file_type") val fileType: String,
    @SerialName("file_name") val fileName: String,
    @SerialName("file_size") val fileSize: Int
)