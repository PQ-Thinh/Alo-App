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
import io.github.jan.supabase.functions.functions
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
                val response: HttpResponse = supabaseClient.functions.invoke(
                    function = "stream-token",
                    body = buildJsonObject { put("userId", userId) }
                )
                
                val responseText = response.bodyAsText()
                Log.d(TAG, "Raw Response từ stream-token: $responseText")

                val jsonObj = Json.parseToJsonElement(responseText).jsonObject
                return@withContext jsonObj["token"]?.jsonPrimitive?.content
                    ?: throw Exception("Không tìm thấy token. Server trả về: $responseText")
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
                if (StreamVideo.instance().user.id == userId) {
                    Log.d(TAG, "StreamVideo đã được khởi tạo đúng cho user: $userId")
                    return@withContext
                } else {
                    Log.w(TAG, "Phát hiện phiên bản cũ của user khác. Đang dọn dẹp...")
                    StreamVideo.instance().logOut()
                }
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
            if (!StreamVideo.isInstalled) {
                throw IllegalStateException("Video Call System chưa được khởi tạo. Vui lòng đăng xuất và đăng nhập lại.")
            }
            val call = StreamVideo.instance().call(type = CALL_TYPE, id = callId)
            // Tạo call và ring members
            call.create(memberIds = memberIds, ring = true)
            call
        }
    }

    /**
     * Bắn API gọi Push Notification "INCOMING_CALL" tới máy B.
     */
    override suspend fun pushIncomingCall(
        callId: String,
        senderId: String,
        receiverIds: List<String>
    ) {
        withContext(Dispatchers.IO) {
            try {
                val payload = PushCallPayload(
                    callId = callId,
                    senderId = senderId,
                    receiverIds = receiverIds
                )
                
                val payloadString = kotlinx.serialization.json.Json.encodeToString(PushCallPayload.serializer(), payload)
                
                val response: HttpResponse = supabaseClient.functions.invoke("push_call") {
                    setBody(
                        TextContent(
                            text = payloadString,
                            contentType = ContentType.Application.Json
                        )
                    )
                }
                Log.d(TAG, "Push call response: ${response.bodyAsText()}")
            } catch (e: Exception) {
                Log.e(TAG, "Gửi push call thất bại", e)
            }
        }
    }

    /**
     * Tham gia vào cuộc gọi đang có (khi nhận FCM incoming call và Accept).
     */
    override suspend fun joinCall(callId: String): Call {
        return withContext(Dispatchers.IO) {
            if (!StreamVideo.isInstalled) {
                throw IllegalStateException("Video Call System chưa được khởi tạo. Vui lòng đăng xuất và đăng nhập lại.")
            }
            val call = StreamVideo.instance().call(type = CALL_TYPE, id = callId)
            call.join()
            call
        }
    }
    override suspend fun rejectCall(callId: String) {
        withContext(Dispatchers.IO) {
            try {
                if (!StreamVideo.isInstalled) return@withContext
                val call = StreamVideo.instance().call(type = CALL_TYPE, id = callId)
                call.reject()
                Log.d(TAG, "Đã gửi lệnh Reject cuộc gọi: $callId lên Stream")
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi reject cuộc gọi", e)
            }
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

@Serializable
private data class PushCallPayload(
    val callId: String,
    val senderId: String,
    val receiverIds: List<String>
)