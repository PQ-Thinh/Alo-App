package com.example.alo.data.remote.dto

import com.example.alo.domain.model.User
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    @SerialName("id") val id: String,
    @SerialName("username") val username: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("email") val email: String,
    @SerialName("bio") val bio: String? = null,
    @SerialName("phone") val phone: String? = null,
    @SerialName("avatarid") val avatarId: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("public_key") val publicKey: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
) {
    // Hàm chuyển đổi (Mapper) từ DTO của tầng Data sang Model của tầng Domain
    fun toDomain(): User {
        return User(
            id = id,
            username = username,
            displayName = displayName,
            email = email,
            bio = bio,
            phone = phone,
            avatarId = avatarId,
            avatarUrl = avatarUrl,
            publicKey = publicKey,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
    fun User.toDto(): UserDto {
        return UserDto(
            id = this.id,
            username = this.username,
            displayName = this.displayName,
            email = this.email,
            bio = this.bio,
            phone = this.phone,
            avatarId = this.avatarId,
            avatarUrl = this.avatarUrl,
            publicKey = this.publicKey,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
}