package com.example.alo.data.repository


import android.util.Log
import com.example.alo.domain.repositories.UserDeviceRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject

class UserDeviceRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : UserDeviceRepository {

    override suspend fun registerDevice(userId: String, fcmToken: String, deviceName: String?) {
        try {
            // Upsert: Nếu fcmToken đã tồn tại thì cập nhật, chưa có thì thêm mới
            val deviceData = mapOf(
                "user_id" to userId,
                "fcm_token" to fcmToken,
                "device_name" to deviceName
            )
            supabaseClient.postgrest["user_devices"].upsert(deviceData)
        } catch (e: Exception) {
            Log.e("UserDeviceRepo", "Lỗi đăng ký thiết bị: ${e.message}")
        }
    }

    override suspend fun removeDevice(fcmToken: String) {
        try {
            // Xóa token khi user đăng xuất
            supabaseClient.postgrest["user_devices"].delete {
                filter { eq("fcm_token", fcmToken) }
            }
        } catch (e: Exception) {
            Log.e("UserDeviceRepo", "Lỗi xóa thiết bị: ${e.message}")
        }
    }
}