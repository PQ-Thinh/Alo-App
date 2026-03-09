package com.example.alo.presentation.view.utils

import com.example.alo.presentation.helper.UserStatus
import java.time.Instant

fun getUserStatus(lastSeenIsoString: String?): UserStatus {
    if (lastSeenIsoString == null) {
        return UserStatus(isOnline = false, statusText = "Ngoại tuyến")
    }

    return try {
        val lastSeenTime = Instant.parse(lastSeenIsoString).toEpochMilli()
        val currentTime = System.currentTimeMillis()
        val diffInMinutes = (currentTime - lastSeenTime) / (1000 * 60)

        if (diffInMinutes <= 2) {
            UserStatus(isOnline = true, statusText = "Đang hoạt động")
        } else {
            val text = when {
                diffInMinutes < 60 -> "Hoạt động $diffInMinutes phút trước"
                diffInMinutes < 1440 -> "Hoạt động ${diffInMinutes / 60} giờ trước"
                else -> "Hoạt động ${diffInMinutes / 1440} ngày trước"
            }
            UserStatus(isOnline = false, statusText = text)
        }
    } catch (e: Exception) {
        UserStatus(isOnline = false, statusText = "Ngoại tuyến")
    }
}