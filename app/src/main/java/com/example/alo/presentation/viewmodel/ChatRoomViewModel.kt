package com.example.alo.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alo.domain.model.Message
import com.example.alo.domain.repository.AuthRepository
import com.example.alo.domain.repository.ConversationRepository
import com.example.alo.domain.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatRoomViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val messageRepository: MessageRepository,
    private val authRepository: AuthRepository,
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    val conversationId: String = checkNotNull(savedStateHandle["conversationId"])

    private val _currentUserId = MutableStateFlow("")
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _messageText = MutableStateFlow("")
    val messageText: StateFlow<String> = _messageText.asStateFlow()

    init {
        initializeChatRoom()
    }
    private fun initializeChatRoom() {
        viewModelScope.launch {
            val user = authRepository.getCurrentAuthUser()
            if (user != null) {
                _currentUserId.value = user.id

                try {
                   conversationRepository.resetUnreadCount(conversationId, user.id)
                } catch (e: Exception) {}
            }
            val historyMessages = messageRepository.getMessages(conversationId)
            _messages.value = historyMessages

            messageRepository.subscribeToNewMessages(conversationId).collect { newMessage ->
                val currentList = _messages.value

                if (currentList.none { it.id == newMessage.id }) {

                    _messages.value = listOf(newMessage) + currentList
                }
            }
        }
    }

    fun onMessageTextChanged(text: String) {
        _messageText.value = text
    }

    fun sendMessage() {
        val content = _messageText.value.trim()
        val senderId = _currentUserId.value

        if (content.isEmpty() || senderId.isEmpty()) return

        viewModelScope.launch {
            _messageText.value = ""

            messageRepository.sendMessage(conversationId, senderId, content)
        }
    }
}