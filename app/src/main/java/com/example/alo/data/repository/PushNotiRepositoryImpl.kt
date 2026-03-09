package com.example.alo.data.repository

import android.util.Log
import com.example.alo.domain.repository.PushNotiRepository
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PushNotiRepositoryImpl @Inject constructor() : PushNotiRepository {

    override suspend fun getDeviceToken(): String? {
        return try {
            suspendCoroutine { continuation ->
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val token = task.result
                        Log.d("FCM_DEBUG", "1. Lấy token Firebase THÀNH CÔNG: $token")
                        continuation.resume(token)
                    } else {
                        Log.e("FCM_DEBUG", "1. LỖI lấy token Firebase", task.exception)
                        continuation.resume(null)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FCM_DEBUG", "1. Exception: ${e.message}")
            null
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