package com.example.alo.presentation.helper
import com.example.alo.domain.model.ChatList

data class ChatListState(
    val isLoading: Boolean = true,
    val chatList: List<ChatList> = emptyList(),
    val error: String? = null
)