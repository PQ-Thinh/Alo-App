package com.example.alo.domain.model

data class UserDevice(
    val id: String,
    val userId: String,
    val fcmToken: String,
    val deviceName: String?,
    val createdAt: String,
    val updatedAt: String
)