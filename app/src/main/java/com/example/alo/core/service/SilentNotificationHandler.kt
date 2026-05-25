package com.example.alo.core.service

import android.app.Application
import android.app.Notification
import android.util.Log
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.notifications.DefaultNotificationHandler
import io.getstream.video.android.model.StreamCallId

/**
 * Custom NotificationHandler để TẮT toàn bộ default notification UI của GetStream SDK.
 * 
 * App Alo sử dụng custom UI riêng (IncomingCallScreen, OutgoingCallScreen, ActiveCallScreen)
 * thay vì UI mặc định của SDK. Cuộc gọi đến được xử lý thông qua:
 * - WebSocket: ringingCall state trong AppNavigation (khi app foreground)
 * - Push → WebSocket wake-up (khi app background, SDK chỉ cần wake app lên)
 */
class SilentNotificationHandler(
    application: Application
) : DefaultNotificationHandler(application) {

    override fun getRingingCallNotification(
        ringingState: RingingState,
        callId: StreamCallId,
        callDisplayName: String?,
        shouldHaveContentIntent: Boolean
    ): Notification? {
        Log.d("SilentNotifHandler", "Suppressed ringing notification for: ${callId.id}, state: $ringingState")
        return null
    }

    override fun getOngoingCallNotification(
        callId: StreamCallId,
        callDisplayName: String?,
        isOutgoingCall: Boolean,
        remoteParticipantCount: Int
    ): Notification? {
        // Tắt ongoing notification — ta dùng CallForegroundService riêng
        Log.d("SilentNotifHandler", "Suppressed ongoing notification for: ${callId.id}")
        return null
    }
}
