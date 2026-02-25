package com.example.alo.domain.repositories

interface UserDeviceRepository {
    suspend fun registerDevice(userId: String, fcmToken: String, deviceName: String?)
    suspend fun removeDevice(fcmToken: String)
}