package com.example.alo.domain.repository

interface PushNotiRepository {
    suspend fun getDeviceToken(): String?
    suspend fun deleteDeviceToken(): Boolean
}