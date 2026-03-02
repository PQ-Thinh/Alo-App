package com.example.alo.domain.repository

import com.example.alo.domain.model.Friend
import com.example.alo.domain.model.FriendRequest

interface FriendRepository {
    suspend fun getFriendRequests(userId: String): List<FriendRequest>
    suspend fun sendFriendRequest(senderId: String, receiverId: String): Boolean
    suspend fun getFriends(userId: String): List<Friend>
    suspend fun checkFriendStatus(currentUserId: String, targetUserId: String): String
}