package com.example.alo.domain.model

data class Attachment(
    val id: String,
    val messageId: String,
    val fileUrl: String,
    val fileType: String?,
    val fileName: String?,
    val fileSize: Int?,
    val createdAt: String
)