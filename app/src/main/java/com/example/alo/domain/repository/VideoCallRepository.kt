package com.example.alo.domain.repository

import io.getstream.video.android.core.Call

interface VideoCallRepository {
    suspend fun getStreamUserToken(userId: String): String
    suspend fun initStreamClient(userId: String, displayName: String, avatarUrl: String?)
    suspend fun createAndJoinCall(callId: String, memberIds: List<String>): Call
    suspend fun joinCall(callId: String): Call
    suspend fun rejectCall(callId: String)
    fun logoutStreamClient()
    
    // Lưu Metadata chuyên sâu vào bảng video_calls
    suspend fun saveCallMetadata(
        messageId: String,
        durationSec: Int,
        direction: String,
        reason: String,
        isVideo: Boolean
    )
}
