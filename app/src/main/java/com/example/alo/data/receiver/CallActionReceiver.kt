package com.example.alo.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationManager
import androidx.core.content.ContextCompat
import com.example.alo.MainActivity

/**
 * Nhận action Accept/Decline từ notification nền và điều hướng vào MainActivity với action tương ứng.
 */
class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val callId = intent.getStringExtra("callId")
        val callerName = intent.getStringExtra("callerName")
        val action = intent.action

        // Dừng chuông và Foreground service ngay lập tức
        com.example.alo.data.service.CallForegroundService.stop(context)

        // Đóng notification ngay lập tức
        val nm = ContextCompat.getSystemService(context, NotificationManager::class.java)
        if (callId != null) nm?.cancel(callId.hashCode())

        val startIntent = Intent(context, MainActivity::class.java).apply {
            this.action = action
            putExtra("callId", callId)
            putExtra("callerName", callerName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(startIntent)
    }
}
