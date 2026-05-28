package com.example.alo.core.service

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
import com.example.alo.core.receiver.CallActionReceiver
import com.example.alo.core.audio.CallAudioManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.alo.core.utils.Constant

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
    private var isIncomingCall: Boolean = false

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            Constant.ACTION_START_OUTGOING -> {
                val callId = intent.getStringExtra(Constant.EXTRA_CALL_ID) ?: return START_NOT_STICKY
                val peer = intent.getStringExtra(Constant.EXTRA_PEER_NAME) ?: "Đang gọi..."
                currentCallId = callId
                currentPeerName = peer
                isIncomingCall = false
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

            Constant.ACTION_START_INCOMING -> {
                val callId = intent.getStringExtra(Constant.EXTRA_CALL_ID) ?: return START_NOT_STICKY
                val peer = intent.getStringExtra(Constant.EXTRA_PEER_NAME) ?: "Cuộc gọi đến"
                currentCallId = callId
                currentPeerName = peer
                isIncomingCall = true
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

            Constant.ACTION_CALL_CONNECTED -> {
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

            Constant.ACTION_STOP -> {
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
            delay(Constant.CALL_TIMEOUT_MS)
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
            action = Constant.ACTION_INCOMING_CALL_ACCEPT
            putExtra("callId", callId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            callId.hashCode(),
            fullIntent,
            flag or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val acceptIntent = Intent(this, CallActionReceiver::class.java).apply {
            action = Constant.ACTION_INCOMING_CALL_ACCEPT
            putExtra("callId", callId)
            putExtra("callerName", currentPeerName)
        }
        val declineIntent = Intent(this, CallActionReceiver::class.java).apply {
            action = Constant.ACTION_INCOMING_CALL_DECLINE
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

        val builder = NotificationCompat.Builder(this, Constant.CALL_CHANNEL_ID)
            .setSmallIcon(R.mipmap.maloi_icon)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(contentPendingIntent)

        if (isIncomingCall) {
            builder.addAction(android.R.drawable.ic_menu_call, "Nhấc máy", acceptPendingIntent)
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Từ chối", declinePendingIntent)
        } else {
            val cancelIntent = Intent(this, CallActionReceiver::class.java).apply {
                action = Constant.ACTION_INCOMING_CALL_DECLINE
                putExtra("callId", callId)
                putExtra("callerName", currentPeerName)
            }
            val cancelPendingIntent = PendingIntent.getBroadcast(
                this,
                (callId + "_cancel").hashCode(),
                cancelIntent,
                flag or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Hủy cuộc gọi", cancelPendingIntent)
        }

        return builder.build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = ContextCompat.getSystemService(this, NotificationManager::class.java)
            val channel = NotificationChannel(
                Constant.CALL_CHANNEL_ID,
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

        fun startOutgoing(context: Context, callId: String, peerName: String?) {
            val intent = Intent(context, CallForegroundService::class.java).apply {
                action = Constant.ACTION_START_OUTGOING
                putExtra(Constant.EXTRA_CALL_ID, callId)
                putExtra(Constant.EXTRA_PEER_NAME, peerName)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun startIncoming(context: Context, callId: String, callerName: String?) {
            val intent = Intent(context, CallForegroundService::class.java).apply {
                action = Constant.ACTION_START_INCOMING
                putExtra(Constant.EXTRA_CALL_ID, callId)
                putExtra(Constant.EXTRA_PEER_NAME, callerName)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun notifyConnected(context: Context, callId: String) {
            val intent = Intent(context, CallForegroundService::class.java).apply {
                action = Constant.ACTION_CALL_CONNECTED
                putExtra(Constant.EXTRA_CALL_ID, callId)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, CallForegroundService::class.java)
            context.stopService(intent)
        }
    }
}
