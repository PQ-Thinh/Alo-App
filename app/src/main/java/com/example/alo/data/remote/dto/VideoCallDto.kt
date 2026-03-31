package com.example.alo.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VideoCallDto(
    @SerialName("id") val id: String,
    @SerialName("message_id") val messageId: String,
    @SerialName("duration_sec") val durationSec: Int? = 0,
    @SerialName("direction") val direction: String? = "outgoing",
    @SerialName("is_video") val isVideo: Boolean? = true,
    @SerialName("end_reason") val endReason: String? = "ended",
    @SerialName("created_at") val createdAt: String? = null
)
