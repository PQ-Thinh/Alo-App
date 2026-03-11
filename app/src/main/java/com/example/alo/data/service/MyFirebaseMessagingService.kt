package com.example.alo.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.alo.MainActivity
import com.example.alo.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_DEBUG", "FCM Token bị làm mới: $token")
        // Token mới này sẽ được SplashViewModel tự động cập nhật ở lần mở app tiếp theo
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Bắt gói hàng (Data Payload) từ Supabase Edge Function gửi về
        if (remoteMessage.data.isNotEmpty()) {
            val type = remoteMessage.data["type"]
            val senderName = remoteMessage.data["senderName"] ?: "Người dùng"
            val conversationId = remoteMessage.data["conversationId"]

            Log.d("FCM_DEBUG", "Nhận tin nhắn từ: $senderName, Type: $type")

            if (type == "NEW_MESSAGE") {
                showNotification(
                    title = senderName,
                    message = "Bạn có một tin nhắn mới",
                    conversationId = conversationId
                )
            }
        }
    }

    private fun showNotification(title: String, message: String, conversationId: String?) {
        val channelId = "alo_chat_channel"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // 1. Tạo Kênh thông báo (Bắt buộc từ Android 8.0 trở lên)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Tin nhắn Alo App",
                NotificationManager.IMPORTANCE_HIGH // Đặt HIGH để nó Pop-up (heads-up) trên màn hình
            ).apply {
                description = "Kênh thông báo khi có tin nhắn mới"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 2. Tạo Hành động khi người dùng bấm vào thông báo -> Mở ứng dụng
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            // Bạn có thể nhét conversationId vào intent để xử lý điều hướng thẳng vào phòng chat sau
            putExtra("conversationId", conversationId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Vẽ Giao diện Thông báo
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.maloi_icon)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true) // Bấm vào là tự biến mất
            .setSound(defaultSoundUri) // Kêu "Ting" một cái
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Ưu tiên hiển thị Pop-up
            .setContentIntent(pendingIntent)

        // 4. Hiển thị lên màn hình (Dùng số ngẫu nhiên để các thông báo không đè lên nhau)
        val notificationId = Random.nextInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}