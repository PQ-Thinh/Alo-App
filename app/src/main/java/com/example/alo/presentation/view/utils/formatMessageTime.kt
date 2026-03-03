package com.example.alo.presentation.view.utils
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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