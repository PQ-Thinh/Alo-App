package com.example.alo.data.utils

import kotlinx.serialization.Serializable

@Serializable
data class EncryptedPayload(
    val for_sender: String,   // Chứa Ciphertext (+ kèm Khóa tạm thời do Tink tự gộp) của người gửi
    val for_receiver: String, // Chứa Ciphertext (+ kèm Khóa tạm thời do Tink tự gộp) của người nhận
    val signature: String     // Chữ ký điện tử của người gửi
)