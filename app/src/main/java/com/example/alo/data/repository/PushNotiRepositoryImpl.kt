package com.example.alo.data.repository

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
                        continuation.resume(task.result)
                    } else {
                        continuation.resume(null)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
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