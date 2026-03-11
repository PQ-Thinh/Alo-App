package com.example.alo.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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

        // 1. Tạo Kênh thông báo (Bắt buộc cho Bong bóng chat phải bật allowBubbles)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Tin nhắn Alo App", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Kênh thông báo khi có tin nhắn mới"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setAllowBubbles(true) // Cho phép hiển thị bong bóng
                }
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 2. Tạo Intent để mở MainActivity và truyền conversationId
        val intent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("conversationId", conversationId)
        }

        // Bắt buộc dùng FLAG_MUTABLE cho Bubble API từ Android 12 trở lên
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(this, conversationId?.hashCode() ?: 0, intent, flag)

        // 3. Khởi tạo đối tượng Person (Người gửi và Người nhận)
        val senderPerson = Person.Builder()
            .setName(title)
            // .setIcon(IconCompat.createWithResource(this, R.drawable.avatar_default)) // Nếu có icon avatar thì thêm vào đây
            .build()

        // 4. Tạo MessagingStyle (Giao diện chuẩn của ứng dụng Chat)
        val messagingStyle = NotificationCompat.MessagingStyle(Person.Builder().setName("Tôi").build())
            .addMessage(message, System.currentTimeMillis(), senderPerson)

        // 5. Tạo Lối tắt (Shortcut) - Yêu cầu bắt buộc của Android 11+ để hiện Bubble
        val shortcutId = "chat_$conversationId"
        val shortcut = ShortcutInfoCompat.Builder(this, shortcutId)
            .setShortLabel(title)
            .setLongLabel("Tin nhắn từ $title")
            .setIcon(IconCompat.createWithResource(this, R.mipmap.maloi))
            .setIntent(intent)
            .setLongLived(true)
            .setCategories(setOf("com.example.alo.category.TEXT_SHARE_TARGET"))
            .setPerson(senderPerson)
            .build()
        ShortcutManagerCompat.pushDynamicShortcut(this, shortcut)

        // 6. Tạo Bubble Metadata (Cấu hình bong bóng)
        val bubbleMetadata = NotificationCompat.BubbleMetadata.Builder(
            pendingIntent,
            IconCompat.createWithResource(this, R.mipmap.maloi)
        )
            .setDesiredHeight(600) // Chiều cao cửa sổ chat khi bấm vào bong bóng
            .setAutoExpandBubble(true) // Tự động mở bong bóng nếu app đang ở background
            .setSuppressNotification(true)
            .build()

        // 7. Lắp ráp Notification
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE) // BẮT BUỘC LÀ CATEGORY_MESSAGE
            .setStyle(messagingStyle) // Áp dụng giao diện chat
            .setShortcutId(shortcutId) // Liên kết với Shortcut
            .setBubbleMetadata(bubbleMetadata) // Thêm Bong bóng
            .setContentIntent(pendingIntent)

        // 8. Hiển thị thông báo (Dùng conversationId làm ID để các tin nhắn cùng người sẽ gộp chung 1 bong bóng)
        val notificationId = conversationId?.hashCode() ?: Random.nextInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}