package com.example.alo.domain.repository

import io.getstream.video.android.core.Call

interface VideoCallRepository {
    suspend fun getStreamUserToken(userId: String): String
    suspend fun initStreamClient(userId: String, displayName: String, avatarUrl: String?)
    suspend fun createAndJoinCall(callId: String, memberIds: List<String>): Call
    suspend fun pushIncomingCall(callId: String, senderId: String, receiverIds: List<String>)
    suspend fun joinCall(callId: String): Call
    fun logoutStreamClient()
}
