package com.example.alo.core.crypto

import android.content.Context
import android.util.Log
import com.example.alo.domain.repository.ParticipantRepository
import com.example.alo.domain.repository.UserRepository

/**
 * Chịu trách nhiệm toàn bộ logic Auto-Healing Group Key Re-wrap.
 *
 * Khi user đổi thiết bị/xóa app → Private Key cũ mất → tạo key mới
 * → encrypted_group_key trên server vẫn wrap bằng Public Key CŨ → FAIL giải mã.
 *
 * Helper này:
 * 1. Cho phép User A (người mất key) đánh dấu yêu cầu re-wrap trên DB
 * 2. Cho phép User B (người có Group Key hợp lệ) re-wrap cho User A
 * 3. Background scan khi mở app để xử lý các request re-wrap đang chờ
 */
object GroupKeyRewrapHelper {

    private const val TAG = "GroupKeyRewrap"

    /**
     * GỌI BỞI NGƯỜI CẦN KHÔI PHỤC (User A).
     * Đánh dấu needs_key_rewrap = true trên DB cho participant của mình.
     */
    suspend fun requestKeyRewrap(
        participantRepository: ParticipantRepository,
        conversationId: String,
        userId: String
    ) {
        try {
            Log.w(TAG, "🔑 Đánh dấu needs_key_rewrap=true cho user=$userId, group=$conversationId")
            participantRepository.setNeedsKeyRewrap(conversationId, userId, true)
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi requestKeyRewrap: ${e.message}", e)
        }
    }

    /**
     * GỌI BỞI NGƯỜI GIÚP ĐỠ (User B - có Group Key hợp lệ).
     *
     * 1. Giải mã Group Key của mình (User B)
     * 2. Lấy Public Key MỚI của User A từ Supabase
     * 3. Wrap lại Group Key bằng Public Key MỚI của User A
     * 4. Update encrypted_group_key cho User A trên DB
     * 5. Set needs_key_rewrap = false
     *
     * @return true nếu re-wrap thành công
     */
    suspend fun processRewrapForUser(
        context: Context,
        helperUserId: String,       // userId của người giúp (B) — dùng để giải mã Group Key
        targetUserId: String,       // userId cần re-wrap (A)
        conversationId: String,
        participantRepository: ParticipantRepository,
        userRepository: UserRepository
    ): String? {
        return try {
            Log.d(TAG, "🔄 Bắt đầu re-wrap key cho user=$targetUserId trong group=$conversationId")

            // 1. Lấy encrypted_group_key của helper (B)
            val helperParticipant = participantRepository.getParticipant(conversationId, helperUserId)
            val helperWrappedKey = helperParticipant?.encryptedGroupKey
            if (helperWrappedKey.isNullOrEmpty()) {
                Log.e(TAG, "Helper không có encrypted_group_key!")
                return null
            }

            // 2. Giải mã Group Key bằng Private Key của helper
            val groupKeysetBase64 = CryptoHelper.unwrapGroupKey(context, helperUserId, helperWrappedKey)
            if (groupKeysetBase64.isEmpty()) {
                Log.e(TAG, "Helper không thể giải mã Group Key!")
                return null
            }

            // 3. Lấy Public Key MỚI của target user (A) từ Supabase
            val targetProfile = userRepository.getCurrentUser(targetUserId)
            val targetPublicEncryptKey = targetProfile?.publicEncryptKey
            if (targetPublicEncryptKey.isNullOrEmpty()) {
                Log.e(TAG, "Không tìm thấy Public Key của user=$targetUserId")
                return null
            }

            // 4. Wrap lại Group Key bằng Public Key MỚI của A
            val newWrappedKey = CryptoHelper.wrapGroupKey(groupKeysetBase64, targetPublicEncryptKey)
            if (newWrappedKey.isEmpty()) {
                Log.e(TAG, "Wrap Group Key thất bại!")
                return null
            }

            Log.i(TAG, "✅ Re-wrap key THÀNH CÔNG cho user=$targetUserId trong group=$conversationId. Bỏ qua cập nhật DB để tránh lỗi RLS, gửi trực tiếp qua Broadcast.")
            return newWrappedKey
        } catch (e: Exception) {
            Log.e(TAG, "❌ Lỗi processRewrapForUser: ${e.message}", e)
            return null
        }
    }

    /**
     * BACKGROUND SCAN: Quét tất cả nhóm mà mình tham gia để tìm thành viên cần re-wrap.
     * Gọi khi mở app (từ SplashViewModel) hoặc khi mở ChatRoom mà mình có Group Key hợp lệ.
     *
     * Logic:
     * 1. Query DB: tìm tất cả participant có needs_key_rewrap = true trong các nhóm mình tham gia
     * 2. Với mỗi participant tìm thấy: gọi processRewrapForUser()
     */
    suspend fun scanAndProcessPendingRewraps(
        context: Context,
        userId: String,
        participantRepository: ParticipantRepository,
        userRepository: UserRepository
    ) {
        try {
            val pendingRewraps = participantRepository.getParticipantsNeedingRewrap(userId)

            if (pendingRewraps.isEmpty()) {
                Log.d(TAG, "Không có ai cần re-wrap key.")
                return
            }

            Log.i(TAG, "🔍 Tìm thấy ${pendingRewraps.size} participant cần re-wrap key")

            for (participant in pendingRewraps) {
                // Bỏ qua nếu chính mình cần re-wrap (mình không thể tự giúp mình)
                if (participant.userId == userId) continue

                val success = processRewrapForUser(
                    context = context,
                    helperUserId = userId,
                    targetUserId = participant.userId,
                    conversationId = participant.conversationId,
                    participantRepository = participantRepository,
                    userRepository = userRepository
                )

                if (success) {
                    Log.i(TAG, "✅ Đã re-wrap xong cho ${participant.userId} trong group ${participant.conversationId}")
                } else {
                    Log.w(TAG, "⚠️ Không thể re-wrap cho ${participant.userId} trong group ${participant.conversationId}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi scanAndProcessPendingRewraps: ${e.message}", e)
        }
    }
}
