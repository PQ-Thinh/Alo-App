package com.example.alo.presentation.view.utils

import android.util.Log
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

fun formatRelativeTime(utcTimeString: String): String {
    return try {
        val messageInstant = Instant.parse(utcTimeString)

        val nowInstant = Instant.now()

        val zoneId = ZoneId.systemDefault()
        val messageLocalTime = LocalDateTime.ofInstant(messageInstant, zoneId)
        val nowLocalTime = LocalDateTime.ofInstant(nowInstant, zoneId)

        val secondsDiff = ChronoUnit.SECONDS.between(messageInstant, nowInstant)
        val minutesDiff = ChronoUnit.MINUTES.between(messageInstant, nowInstant)
        val hoursDiff = ChronoUnit.HOURS.between(messageInstant, nowInstant)

        val daysDiff = ChronoUnit.DAYS.between(messageLocalTime.toLocalDate(), nowLocalTime.toLocalDate())

        when {
            secondsDiff < 60 -> "Vừa xong"

            minutesDiff < 60 -> "${minutesDiff} phút trước"

            daysDiff == 0L -> {
                val formatter = DateTimeFormatter.ofPattern("HH:mm")
                messageLocalTime.format(formatter)
            }

            daysDiff == 1L -> "Hôm qua"

            messageLocalTime.year == nowLocalTime.year -> {
                val formatter = DateTimeFormatter.ofPattern("dd/MM")
                messageLocalTime.format(formatter)
            }

            else -> {
                val formatter = DateTimeFormatter.ofPattern("dd/MM/yy")
                messageLocalTime.format(formatter)
            }
        }
    } catch (e: Exception) {
        Log.e("TimeUtils", "Lỗi định dạng thời gian: ${e.message}")
        ""
    }
}