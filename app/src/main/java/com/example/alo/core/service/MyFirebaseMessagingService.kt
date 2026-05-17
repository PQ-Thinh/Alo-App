package com.example.alo.core.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.alo.MainActivity
import com.example.alo.R
import com.example.alo.domain.repository.AuthRepository
import com.example.alo.domain.repository.UserDeviceRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import kotlin.random.Random

@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var userDeviceRepository: UserDeviceRepository

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val deviceName = "${Build.MODEL}"
                userDeviceRepository.saveFcmToken(token, deviceName)
                Log.d("FCM_DEBUG", "Cập nhật Token lên Supabase thành công")
            } catch (e: Exception) {
                Log.e("FCM_ERROR", "Lỗi cập nhật Token lên Supabase", e)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        if (remoteMessage.data.isNotEmpty()) {
            val type = remoteMessage.data["type"]
            val senderName = remoteMessage.data["senderName"] ?: "Người dùng"
            val conversationId = remoteMessage.data["conversationId"]
            val senderAvatar = remoteMessage.data["senderAvatar"]
            val callId = remoteMessage.data["callId"]
            val senderId = remoteMessage.data["senderId"]

            Log.d("FCM_DEBUG", "Nhận tin nhắn từ: $senderName, Type: $type")

            when (type) {
                "NEW_MESSAGE" -> {
                    CoroutineScope(Dispatchers.IO).launch {
                        val authUser = authRepository.getCurrentAuthUser()
                        if (authUser?.id != senderId) {
                            showNotification(
                                title = senderName,
                                message = "Bạn có một tin nhắn mới",
                                conversationId = conversationId,
                                avatarUrl = senderAvatar
                            )
                        }
                    }
                }
                "INCOMING_CALL" -> {
                    if (callId != null) {
                        CoroutineScope(Dispatchers.IO).launch {
                            val authUser = authRepository.getCurrentAuthUser()
                            if (authUser?.id != senderId) {
                                showIncomingCallNotification(callerName = senderName, callId = callId)
                            }
                        }
                    }
                }
                "CALL_CANCELLED", "MISSED_CALL", "CALL_REJECTED", "CALL_STARTED" -> {
                    if (callId != null) {
                        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.cancel(callId.hashCode())
                        Log.d("FCM_DEBUG", "Đã dọn dẹp chuông/noti cuộc gọi: $callId, Type: $type")
                        CallForegroundService.stop(this)
                    }
                }
            }
        }
    }

    private fun showNotification(title: String, message: String, conversationId: String?, avatarUrl: String?) {
        val channelId = "alo_chat_channel"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val avatarBitmap = getBitmapFromUrl(avatarUrl)

        val iconCompat = if (avatarBitmap != null) {
            IconCompat.createWithBitmap(avatarBitmap)
        } else {
            IconCompat.createWithResource(this, R.mipmap.maloi_icon)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Tin nhắn Alo App", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Kênh thông báo khi có tin nhắn mới"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("conversationId", conversationId)
        }

        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(this, conversationId?.hashCode() ?: 0, intent, flag)

        val senderPerson = Person.Builder()
            .setName(title)
            .setIcon(iconCompat)
            .build()

        val messagingStyle = NotificationCompat.MessagingStyle(Person.Builder().setName("Tôi").build())
            .addMessage(message, System.currentTimeMillis(), senderPerson)

        val shortcutId = "chat_$conversationId"
        val shortcut = ShortcutInfoCompat.Builder(this, shortcutId)
            .setShortLabel(title)
            .setIcon(iconCompat)
            .setIntent(intent)
            .setLongLived(true)
            .setPerson(senderPerson)
            .build()
        ShortcutManagerCompat.pushDynamicShortcut(this, shortcut)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.maloi_icon)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setStyle(messagingStyle)
            .setShortcutId(shortcutId)
            .setContentIntent(pendingIntent)

        // Nếu app không ở Foreground -> tạo Bong Bóng Chat
        if (!com.example.alo.AloApplication.isAppInForeground) {
            val bubbleMetadata = NotificationCompat.BubbleMetadata.Builder(pendingIntent, iconCompat)
                .setDesiredHeight(600)
                .setAutoExpandBubble(true)
                .setSuppressNotification(false)
                .build()
            notificationBuilder.setBubbleMetadata(bubbleMetadata)
        }

        notificationManager.notify(conversationId?.hashCode() ?: Random.nextInt(), notificationBuilder.build())
    }

    private fun showIncomingCallNotification(callerName: String, callId: String) {
        val channelId = "alo_call_channel"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Cuộc gọi đến",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo khi có người gọi video"
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE), null)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        CallForegroundService.startIncoming(this, callId, callerName)

        val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
            action = "ACTION_INCOMING_CALL"
            putExtra("callId", callId)
            putExtra("callerName", callerName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val fullScreenPendingIntent = PendingIntent.getActivity(this, callId.hashCode(), fullScreenIntent, flag or PendingIntent.FLAG_UPDATE_CURRENT)

        val acceptIntent = Intent(this, com.example.alo.core.receiver.CallActionReceiver::class.java).apply {
            action = "com.example.alo.ACTION_INCOMING_CALL_ACCEPT"
            putExtra("callId", callId)
            putExtra("callerName", callerName)
        }
        val acceptPendingIntent = PendingIntent.getBroadcast(this, (callId + "_accept").hashCode(), acceptIntent, flag or PendingIntent.FLAG_UPDATE_CURRENT)

        val declineIntent = Intent(this, com.example.alo.core.receiver.CallActionReceiver::class.java).apply {
            action = "com.example.alo.ACTION_INCOMING_CALL_DECLINE"
            putExtra("callId", callId)
            putExtra("callerName", callerName)
        }
        val declinePendingIntent = PendingIntent.getBroadcast(this, (callId + "_decline").hashCode(), declineIntent, flag or PendingIntent.FLAG_UPDATE_CURRENT)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.maloi_icon)
            .setContentTitle("Cuộc gọi đến")
            .setContentText("$callerName đang gọi video cho bạn...")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setOngoing(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(android.R.drawable.ic_menu_call, "Nhấc máy", acceptPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Từ chối", declinePendingIntent)
            .setColor(0xFF4CAF50.toInt())
            .setColorized(true)

        notificationManager.notify(callId.hashCode(), notificationBuilder.build())
    }

    private fun getBitmapFromUrl(imageUrl: String?): android.graphics.Bitmap? {
        if (imageUrl.isNullOrEmpty()) return null
        return try {
            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.doInput = true
            connection.connect()
            val input = connection.inputStream
            BitmapFactory.decodeStream(input)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
