package com.example.alo.core.crypto

import android.content.Context
import android.util.Log
import com.example.alo.domain.repository.UserRepository

/**
 * Chịu trách nhiệm đồng bộ crypto keys giữa thiết bị (SharedPref) và Supabase.
 * 
 * Gọi [ensureKeysReady] NGAY SAU khi đăng nhập thành công (mọi flow: email, Google, OTP).
 * 
 * Logic:
 * 1. Tạo hoặc load key từ SharedPref (theo userId)
 * 2. So sánh public key local với public key trên Supabase
 * 3. Nếu khác → upload key mới lên Supabase
 * 4. Nếu giống → không làm gì (key đã đồng bộ)
 */
object KeySyncHelper {

    private const val TAG = "KeySyncHelper"

    /**
     * Đảm bảo local keys sẵn sàng và đồng bộ với Supabase.
     *
     * @return true nếu keys đã sẵn sàng (sync thành công hoặc đã đồng bộ), false nếu lỗi
     */
    suspend fun ensureKeysReady(
        context: Context,
        userId: String,
        userRepository: UserRepository
    ): Boolean {
        return try {
            // 1. Clear cache cũ để đảm bảo load key đúng user
            CryptoHelper.clearCachedKeys()

            // 2. Dọn dẹp legacy keys (nếu còn sót từ phiên bản cũ)
            CryptoHelper.cleanupLegacyKeys(context)

            // 3. Tạo hoặc load key từ SharedPref (per-user)
            val (localEncryptKey, localSignKey) = CryptoHelper.generateAndGetPublicKeys(context, userId)

            if (localEncryptKey.isEmpty() || localSignKey.isEmpty()) {
                Log.e(TAG, "Không thể tạo/load local keys cho user: $userId")
                return false
            }

            // 4. Lấy public keys hiện tại trên Supabase
            val currentProfile = userRepository.getCurrentUser(userId)
            val remoteEncryptKey = currentProfile?.publicEncryptKey ?: ""
            val remoteSignKey = currentProfile?.publicSignKey ?: ""

            // 5. So sánh: nếu khác → upload key mới
            if (localEncryptKey != remoteEncryptKey || localSignKey != remoteSignKey) {
                Log.w(TAG, "Key mismatch detected! Local ≠ Remote. Uploading new keys cho user: $userId")

                val updateData = mapOf(
                    "public_encrypt_key" to localEncryptKey,
                    "public_sign_key" to localSignKey
                )

                val success = userRepository.updateProfile(userId, updateData)
                if (success) {
                    Log.i(TAG, "✅ Đã đồng bộ public keys lên Supabase thành công cho user: $userId")
                } else {
                    Log.e(TAG, "❌ Không thể upload public keys lên Supabase cho user: $userId")
                    return false
                }
            } else {
                Log.i(TAG, "✅ Keys đã đồng bộ, không cần update cho user: $userId")
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi ensureKeysReady cho user $userId: ${e.message}", e)
            false
        }
    }
}
