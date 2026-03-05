package com.example.alo.domain.repository

import com.example.alo.domain.model.FriendRequest
import com.example.alo.domain.model.User
import kotlinx.coroutines.flow.Flow

interface FriendRepository {
    suspend fun getFriendRequests(userId: String): List<FriendRequest>
    suspend fun sendFriendRequest(senderId: String, receiverId: String): Boolean
    suspend fun checkFriendStatus(currentUserId: String, targetUserId: String): String
    suspend fun getPendingFriendRequests(currentUserId: String): List<User>
    suspend fun acceptFriendRequest(senderId: String, receiverId: String): Boolean
    suspend fun declineFriendRequest(senderId: String, receiverId: String): Boolean
    suspend fun getFriendsList(currentUserId: String): List<User>
    fun subscribeToFriendReQuestListUpdates(receiverId: String): Flow<Unit>

}