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
    @SerialName("birthday") val birthday: String? = null,
    @SerialName("gender") val gender: Boolean? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("avatarid") val avatarId: String,
    @SerialName("public_encrypt_key") val publicEncryptKey: String = "",
    @SerialName("public_sign_key") val publicSignKey: String? = null,
    @SerialName("last_seen") val lastSeen: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null

) {
    // Chuyển từ DTO (Data) sang Model (Domain)
    fun toDomain(): User = User(
        id = id,
        username = username,
        displayName = displayName,
        email = email,
        bio = bio,
        phone = phone,
        avatarId = avatarId,
        birthday = birthday,
        gender = gender,
        avatarUrl = avatarUrl,
        publicEncryptKey = publicEncryptKey,
        publicSignKey = publicSignKey,
        createdAt = createdAt ?: "",
        updatedAt = updatedAt ?: "",
        lastSeen = lastSeen ?: ""
    )

    // Chuyển từ Model (Domain) sang DTO (Data)

}
fun User.toDto(): UserDto = UserDto(
    id = this.id,
    username = this.username,
    displayName = this.displayName,
    email = this.email,
    bio = this.bio,
    phone = this.phone,
    avatarId = this.avatarId,
    avatarUrl = this.avatarUrl,
    birthday = this.birthday,
    gender = this.gender,
    publicEncryptKey = publicEncryptKey,
    publicSignKey = publicSignKey,
    createdAt = this.createdAt.ifEmpty { null },
    updatedAt = this.updatedAt.ifEmpty { null },
    lastSeen = this.lastSeen?.ifEmpty { null }

)