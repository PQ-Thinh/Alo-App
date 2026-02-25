package com.example.alo.data.remote.dto

import com.example.alo.domain.model.Friend
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FriendDto(
    @SerialName("id") val id: String,
    @SerialName("user_id_1") val userId1: String,
    @SerialName("user_id_2") val userId2: String,
    @SerialName("created_at") val createdAt: String
) {
    fun toDomain(): Friend = Friend(id, userId1, userId2, createdAt)
}