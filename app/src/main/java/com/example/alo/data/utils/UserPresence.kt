package com.example.alo.data.utils

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserPresence(
    @SerialName("user_id") val userId: String,
    val status: String
)