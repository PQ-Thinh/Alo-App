package com.example.alo.domain.model

data class User(
    val id: String,
    val username: String,
    val displayName: String,
    val email: String,
    val bio: String?,
    val phone: String?,
    val avatarId: String,
    val avatarUrl: String?,
    val publicKey: String,
    val createdAt: String,
    val updatedAt: String
)