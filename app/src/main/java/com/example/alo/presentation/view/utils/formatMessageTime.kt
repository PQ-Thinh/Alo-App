package com.example.alo.presentation.view.utils
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.LocalDateTime

fun shouldShowTimeHeader(currentMessageTime: String?, previousMessageTime: String?): Boolean {
    if (currentMessageTime.isNullOrEmpty()) return false
    if (previousMessageTime.isNullOrEmpty()) return true

    return try {
        val current = Instant.parse(currentMessageTime)
        val previous = Instant.parse(previousMessageTime)

        // Nếu cách nhau hơn 30 phút thì trả về true (cần hiện header mới)
        val minutesDiff = ChronoUnit.MINUTES.between(previous, current)
        Math.abs(minutesDiff) > 30
    } catch (e: Exception) {
        true
    }
}

fun formatTimeHeader(utcTimeString: String?): String {
    if (utcTimeString.isNullOrEmpty()) return ""
    return try {
        val instant = Instant.parse(utcTimeString)
        val zoneId = ZoneId.systemDefault()
        val localTime = LocalDateTime.ofInstant(instant, zoneId)
        val now = LocalDateTime.now(zoneId)

        val daysDiff = ChronoUnit.DAYS.between(localTime.toLocalDate(), now.toLocalDate())

        when {
            daysDiff == 0L -> {
                val formatter = DateTimeFormatter.ofPattern("HH:mm")
                "Hôm nay ${localTime.format(formatter)}"
            }
            daysDiff == 1L -> {
                val formatter = DateTimeFormatter.ofPattern("HH:mm")
                "Hôm qua ${localTime.format(formatter)}"
            }
            localTime.year == now.year -> {
                val formatter = DateTimeFormatter.ofPattern("dd/MM HH:mm")
                localTime.format(formatter)
            }
            else -> {
                val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                localTime.format(formatter)
            }
        }
    } catch (e: Exception) {
        ""
    }
}

fun formatMessageTime(utcTimeString: String?): String {
    if (utcTimeString.isNullOrEmpty()) return ""
    return try {
        val instant = Instant.parse(utcTimeString)
        val formatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (e: Exception) {
        ""
    }
}