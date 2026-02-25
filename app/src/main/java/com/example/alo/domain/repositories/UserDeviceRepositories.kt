package com.example.alo.domain.repositories

import com.example.alo.domain.model.UserDevice

interface UserDeviceRepositories {
    suspend fun getUserDevices(userId: String): List<UserDevice>
}