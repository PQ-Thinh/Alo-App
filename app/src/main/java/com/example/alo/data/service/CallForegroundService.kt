package com.example.alo.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.example.alo.MainActivity
import com.example.alo.R
import com.example.alo.data.receiver.CallActionReceiver
import com.example.alo.data.utils.CallAudioManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ForegroundService đảm bảo cuộc gọi không bị hệ thống kill và phát chuông/rung.
 * Chỉ giữ vai trò audio + timeout; logic join/leave nằm ở ViewModel.
 */
@AndroidEntryPoint
class CallForegroundService : LifecycleService() {

    @Inject
    lateinit var callAudioManager: CallAudioManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var timeoutJob: Job? = null
    private var currentCallId: String? = null
    private var currentPeerName: String? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START_OUTGOING -> {
                val callId = intent.getStringExtra(EXTRA_CALL_ID) ?: return START_NOT_STICKY
                val peer = intent.getStringExtra(EXTRA_PEER_NAME) ?: "Đang gọi..."
                currentCallId = callId
                currentPeerName = peer
                startForeground(
                    callId.hashCode(),
                    buildNotification(
                        title = "Đang gọi video",
                        text = peer
                    )
                )
                callAudioManager.playRingbackTone()
                scheduleTimeout(callId)
            }

            ACTION_START_INCOMING -> {
                val callId = intent.getStringExtra(EXTRA_CALL_ID) ?: return START_NOT_STICKY
                val peer = intent.getStringExtra(EXTRA_PEER_NAME) ?: "Cuộc gọi đến"
                currentCallId = callId
                currentPeerName = peer
                startForeground(
                    callId.hashCode(),
                    buildNotification(
                        title = "Cuộc gọi đến",
                        text = "$peer đang gọi bạn..."
                    )
                )
                callAudioManager.playIncomingRingtone()
                scheduleTimeout(callId)
            }

            ACTION_CALL_CONNECTED -> {
                timeoutJob?.cancel()
                callAudioManager.stopAll()
                // vẫn giữ foreground để hạn chế bị kill
                currentCallId?.let { callId ->
                    startForeground(
                        callId.hashCode(),
                        buildNotification(
                            title = "Đang trong cuộc gọi",
                            text = currentPeerName ?: "Video call"
                        )
                    )
                }
            }

            ACTION_STOP -> {
                stopSelfSafe()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        timeoutJob?.cancel()
        callAudioManager.stopAll()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun scheduleTimeout(callId: String) {
        timeoutJob?.cancel()
        timeoutJob = serviceScope.launch {
            delay(CALL_TIMEOUT_MS)
            // chỉ tắt âm và dừng service; CallViewModel sẽ tự endCall
            callAudioManager.stopAll()
            stopSelfSafe()
        }
    }

    private fun stopSelfSafe() {
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (_: Exception) { }
        stopSelf()
    }

    private fun buildNotification(title: String, text: String): Notification {
        val callId = currentCallId ?: "call"
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val fullIntent = Intent(this, MainActivity::class.java).apply {
            action = MainActivity.ACTION_INCOMING_CALL_ACCEPT
            putExtra("callId", callId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            callId.hashCode(),
            fullIntent,
            flag or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val acceptIntent = Intent(this, CallActionReceiver::class.java).apply {
            action = MainActivity.ACTION_INCOMING_CALL_ACCEPT
            putExtra("callId", callId)
            putExtra("callerName", currentPeerName)
        }
        val declineIntent = Intent(this, CallActionReceiver::class.java).apply {
            action = MainActivity.ACTION_INCOMING_CALL_DECLINE
            putExtra("callId", callId)
            putExtra("callerName", currentPeerName)
        }

        val acceptPendingIntent = PendingIntent.getBroadcast(
            this,
            (callId + "_accept").hashCode(),
            acceptIntent,
            flag or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val declinePendingIntent = PendingIntent.getBroadcast(
            this,
            (callId + "_decline").hashCode(),
            declineIntent,
            flag or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.maloi_icon)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.ic_menu_call, "Nhấc máy", acceptPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Từ chối", declinePendingIntent)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = ContextCompat.getSystemService(this, NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alo Video Call",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo cuộc gọi video"
                setSound(null, null)
                enableVibration(true)
            }
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "alo_call_foreground"
        private const val CALL_TIMEOUT_MS = 45_000L
        private const val EXTRA_CALL_ID = "extra_call_id"
        private const val EXTRA_PEER_NAME = "extra_peer_name"

        const val ACTION_START_OUTGOING = "com.example.alo.call.START_OUTGOING"
        const val ACTION_START_INCOMING = "com.example.alo.call.START_INCOMING"
        const val ACTION_CALL_CONNECTED = "com.example.alo.call.CALL_CONNECTED"
        const val ACTION_STOP = "com.example.alo.call.STOP"

        fun startOutgoing(context: Context, callId: String, peerName: String?) {
            val intent = Intent(context, CallForegroundService::class.java).apply {
                action = ACTION_START_OUTGOING
                putExtra(EXTRA_CALL_ID, callId)
                putExtra(EXTRA_PEER_NAME, peerName)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun startIncoming(context: Context, callId: String, callerName: String?) {
            val intent = Intent(context, CallForegroundService::class.java).apply {
                action = ACTION_START_INCOMING
                putExtra(EXTRA_CALL_ID, callId)
                putExtra(EXTRA_PEER_NAME, callerName)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun notifyConnected(context: Context, callId: String) {
            val intent = Intent(context, CallForegroundService::class.java).apply {
                action = ACTION_CALL_CONNECTED
                putExtra(EXTRA_CALL_ID, callId)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, CallForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}