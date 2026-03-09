package com.example.alo.data.repository

import android.util.Log
import com.example.alo.domain.repository.PushNotiRepository
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject


class PushNotiRepositoryImpl @Inject constructor() : PushNotiRepository {

    override suspend fun getDeviceToken(): String? {
        return withContext(Dispatchers.IO) {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                Log.d("FCM_DEBUG", "1. Lấy token Firebase THÀNH CÔNG: $token")
                token
            } catch (e: Exception) {
                Log.e("FCM_DEBUG", "1. LỖI lấy token Firebase: ${e.message}", e)
                null
            }
        }
    }

    override suspend fun deleteDeviceToken(): Boolean {
        return try {
            FirebaseMessaging.getInstance().deleteToken().await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}