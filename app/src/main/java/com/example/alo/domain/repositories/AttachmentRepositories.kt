package com.example.alo.domain.repositories

import com.example.alo.domain.model.Attachment

interface AttachmentRepositories {
    suspend fun getAttachment(attachmentId: String): Attachment?
}