package com.example.alo.domain.repository

import com.example.alo.domain.model.User

interface UserRepository {
    suspend fun getCurrentUser(userId: String): User?
    suspend fun saveUserProfile(user: User): Boolean
    suspend fun uploadAvatar(imageBytes: ByteArray, extension: String): String
}