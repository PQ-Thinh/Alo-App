package com.example.alo.domain.repositories

import com.example.alo.domain.model.User

interface UserRepository {
    suspend fun getCurrentUser(userId: String): User?
    suspend fun saveUserProfile(user: User): Boolean
}