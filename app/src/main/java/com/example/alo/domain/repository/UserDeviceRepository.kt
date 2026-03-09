package com.example.alo.domain.repository

interface UserDeviceRepository {
    suspend fun saveFcmToken(token: String, deviceName: String): Boolean
    suspend fun deleteFcmToken(token: String): Boolean
}