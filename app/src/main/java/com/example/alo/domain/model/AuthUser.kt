package com.example.alo.domain.model

data class AuthUser(
    val id: String,
    val email: String,
    val fullName: String?,
    val avatarUrl: String?
)