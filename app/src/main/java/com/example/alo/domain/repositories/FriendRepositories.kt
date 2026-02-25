package com.example.alo.domain.repositories

import com.example.alo.domain.model.Friend

interface FriendRepositories {
    suspend fun getFriends(userId: String): List<Friend>
}