package com.example.alo.presentation.helper

import com.example.alo.domain.model.Message

data class MessageUiModel(
    val message: Message,
    val isUploading: Boolean = false
)