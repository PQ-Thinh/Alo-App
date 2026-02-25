package com.example.alo.data.remote.dto

import com.example.alo.domain.model.UserDevice
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDeviceDto(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("fcm_token") val fcmToken: String,
    @SerialName("device_name") val deviceName: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
) {
    fun toDomain(): UserDevice = UserDevice(
        id = id,
        userId = userId,
        fcmToken = fcmToken,
        deviceName = deviceName,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}