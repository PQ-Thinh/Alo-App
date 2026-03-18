package com.example.alo.data.repository


import android.util.Log
import com.example.alo.domain.repository.UserDeviceRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import javax.inject.Inject

class UserDeviceRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : UserDeviceRepository {
    override suspend fun saveFcmToken(token: String, deviceName: String): Boolean {
        return try {
            supabaseClient.postgrest.rpc(
                function = "upsert_fcm_token",
                parameters = mapOf(
                    "p_token" to token,
                    "p_device_name" to deviceName
                )
            )
            true
        } catch (e: Exception) {
            Log.e("FCM_DEBUG", "4. LỖI GỌI RPC Supabase: ${e.message}", e)
            false
        }
    }

    override suspend fun deleteFcmToken(token: String): Boolean {
        return try {
            supabaseClient.postgrest["user_devices"].delete {
                filter {
                    eq("fcm_token", token)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}