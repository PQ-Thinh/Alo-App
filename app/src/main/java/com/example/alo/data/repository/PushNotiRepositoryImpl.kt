package com.example.alo.data.repository

import android.util.Log
import com.example.alo.domain.repository.PushNotiRepository
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject


class PushNotiRepositoryImpl @Inject constructor() : PushNotiRepository {

    override suspend fun getDeviceToken(): String? {
        return withContext(Dispatchers.IO) { // Ép chạy trên luồng phụ, tránh nghẽn App
            var token: String? = null
            var retryCount = 0
            val maxRetries = 3 // Thử tối đa 3 lần

            while (retryCount < maxRetries) {
                try {
                    // Dùng .await() cực kỳ an toàn
                    token = FirebaseMessaging.getInstance().token.await()
                    Log.d("FCM_DEBUG", "1. Lấy token Firebase THÀNH CÔNG: $token")
                    break // Lấy thành công thì thoát vòng lặp ngay
                } catch (e: Exception) {
                    retryCount++
                    Log.e("FCM_DEBUG", "1. LỖI lấy token lần $retryCount: ${e.message}")

                    if (retryCount >= maxRetries) {
                        Log.e("FCM_DEBUG", "1. Đã thử $maxRetries lần nhưng bó tay.", e)
                        break
                    }
                    // Nghỉ 2 giây cho Google Play "thở" rồi gọi lại
                    delay(2000L)
                }
            }
            token
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