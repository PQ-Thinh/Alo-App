package com.example.alo.data.repository

import android.content.Context
import android.util.Log
import com.example.alo.domain.repository.VideoCallRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.model.User
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.functions.Functions
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
                val response = supabaseClient.functions.invoke(
                    function = "stream-token",
                    body = buildJsonObject { put("userId", userId }
                )
                // Parse token từ response body
                val bodyText = response.body.toString()
                // Đơn giản parse: tìm "token":"<value>"
                val regex = Regex("\"token\"\\s*:\\s*\"([^\"]+)\"")
                regex.find(bodyText)?.groupValues?.get(1)
                    ?: throw Exception("Token không tìm thấy trong response: $bodyText")
            } catch (e: Exception) {
                Log.e(TAG, "Lấy Stream token thất bại: ${e.message}", e)
                throw e
            }
        }
    }

    /**
     * Khởi tạo StreamVideo singleton client.
     * Phải gọi hàm này sau khi user đăng nhập thành công.
     */
    override suspend fun initStreamClient(
        userId: String,
        displayName: String,
        avatarUrl: String?
    ) {
        withContext(Dispatchers.IO) {
            // Nếu đã có instance rồi thì bỏ qua
            if (StreamVideo.isInstalled) {
                Log.d(TAG, "StreamVideo đã được khởi tạo.")
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
                apiKey = com.example.alo.BuildConfig.streamApiKey,
                token = io.getstream.video.android.core.GEO.GlobalEdgeNetwork,
                user = user,
                tokenProvider = { token }
            ).build()

            Log.d(TAG, "StreamVideo đã khởi tạo cho user: $userId")
        }
    }

    /**
     * Tạo cuộc gọi mới, thêm members và ring họ.
     */
    override suspend fun createAndJoinCall(
        callId: String,
        memberIds: List<String>
    ): Call {
        return withContext(Dispatchers.IO) {
            val call = StreamVideo.instance().call(type = CALL_TYPE, id = callId)
            val members = memberIds.map {
                io.getstream.video.android.model.MemberRequest(userId = it)
            }
            call.create(memberIds = memberIds, ring = true)
            call
        }
    }

    /**
     * Tham gia vào cuộc gọi đang có (khi nhận incoming call từ FCM).
     */
    override suspend fun joinCall(callId: String): Call {
        return withContext(Dispatchers.IO) {
            val call = StreamVideo.instance().call(type = CALL_TYPE, id = callId)
            call.join()
            call
        }
    }

    /**
     * Đăng xuất khỏi StreamVideo client (khi user logout khỏi app).
     */
    override fun logoutStreamClient() {
        if (StreamVideo.isInstalled) {
            StreamVideo.instance().logOut()
            Log.d(TAG, "StreamVideo đã logout.")
        }
    }
}
