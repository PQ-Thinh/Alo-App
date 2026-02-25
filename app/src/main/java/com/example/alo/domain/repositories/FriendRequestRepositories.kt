package com.example.alo.domain.repositories

import com.example.alo.domain.model.FriendRequest

interface FriendRequestRepositories {
    suspend fun getFriendRequests(userId: String): List<FriendRequest>
}