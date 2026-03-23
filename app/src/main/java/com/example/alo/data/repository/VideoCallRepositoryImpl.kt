package com.example.alo.data.repository

import android.content.Context
import android.util.Log
import com.example.alo.BuildConfig
import com.example.alo.domain.repository.VideoCallRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.model.User
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoCallRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val supabaseClient: SupabaseClient
) : VideoCallRepository {

    companion object {
        private const val TAG = "VideoCallRepo"
        private const val CALL_TYPE = "default"
    }

    /**
     * Gọi Supabase Edge Function `stream-token` để lấy JWT token cho Stream SDK.
     * Edge Function phải trả về JSON: { "token": "..." }
     */
    override suspend fun getStreamUserToken(userId: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val responseText = supabaseClient.funtion.invoke(
                    function = "stream-token",
                    body = buildJsonObject { put("userId", userId) }
                )
                // Parse token từ response
                val regex = Regex(""""token"\s*:\s*"([^"]+)"""")
                regex.find(responseText.toString())?.groupValues?.get(1)
                    ?: throw Exception("Không tìm thấy token trong response")
            } catch (e: Exception) {
                Log.e(TAG, "Lấy Stream token thất bại: ${e.message}", e)
                throw e
            }
        }
    }

    /**
     * Khởi tạo StreamVideo singleton client.
     * Phải gọi sau khi user đăng nhập thành công.
     * Nếu đã khởi tạo rồi thì bỏ qua.
     */
    override suspend fun initStreamClient(
        userId: String,
        displayName: String,
        avatarUrl: String?
    ) {
        withContext(Dispatchers.IO) {
            if (StreamVideo.isInstalled) {
                Log.d(TAG, "StreamVideo đã được khởi tạo sẵn.")
                return@withContext
            }

            val token = getStreamUserToken(userId)

            val user = User(
                id = userId,
                name = displayName,
                image = avatarUrl ?: ""
            )

            StreamVideoBuilder(
                context = context,
                apiKey = BuildConfig.getStreamKey,
                user = user,
                token = token
            ).build()

            Log.d(TAG, "StreamVideo khởi tạo thành công cho user: $userId")
        }
    }

    /**
     * Tạo cuộc gọi mới, ring tới các members.
     * [callId] nên là conversationId hoặc UUID duy nhất của cuộc gọi.
     * [memberIds] là danh sách userId (Supabase auth UID) của người nhận.
     */
    override suspend fun createAndJoinCall(
        callId: String,
        memberIds: List<String>
    ): Call {
        return withContext(Dispatchers.IO) {
            val call = StreamVideo.instance().call(type = CALL_TYPE, id = callId)
            // Tạo call và ring members
            call.create(memberIds = memberIds, ring = true)
            call
        }
    }

    /**
     * Tham gia vào cuộc gọi đang có (khi nhận FCM incoming call và Accept).
     */
    override suspend fun joinCall(callId: String): Call {
        return withContext(Dispatchers.IO) {
            val call = StreamVideo.instance().call(type = CALL_TYPE, id = callId)
            call.join()
            call
        }
    }

    /**
     * Đăng xuất khỏi StreamVideo khi user logout.
     */
    override fun logoutStreamClient() {
        if (StreamVideo.isInstalled) {
            StreamVideo.instance().logOut()
            Log.d(TAG, "StreamVideo logout thành công.")
        }
    }
}