package com.example.alo.data.remote.dto

import com.example.alo.domain.helperEnum.FriendRequestStatus
import com.example.alo.domain.model.FriendRequest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FriendRequestDto(
    @SerialName("id") val id: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("receiver_id") val receiverId: String,
    @SerialName("status") val status: String,
    @SerialName("created_at") val createdAt: String
) {
    fun toDomain(): FriendRequest {
        // Ánh xạ chuỗi từ DB sang Enum của Kotlin
        val domainStatus = FriendRequestStatus.entries.find { it.value == status }
            ?: FriendRequestStatus.PENDING

        return FriendRequest(id, senderId, receiverId, domainStatus, createdAt)
    }
}