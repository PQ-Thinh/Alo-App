package com.example.alo.domain.repository

import com.example.alo.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun getCurrentUser(userId: String): User?
    suspend fun saveUserProfile(user: User): Boolean
    suspend fun uploadAvatar(imageBytes: ByteArray, extension: String): String
    suspend fun searchUsers(query: String): List<User>
    suspend fun updateProfile(userId: String, updateData: Map<String, String>): Boolean
    suspend fun updateLastSeen()
    fun startHeartbeat()
    fun stopHeartbeat()
    fun observeUserStatus(targetUserId: String): Flow<String?>
    fun observeListUsersStatus(userIds: List<String>): Flow<Pair<String, String?>>

}