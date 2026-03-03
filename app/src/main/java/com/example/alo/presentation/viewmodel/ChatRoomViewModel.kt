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
import kotlinx.coroutines.flow.update // <--- Import quan trọng để ép StateFlow cập nhật
import kotlinx.coroutines.launch
import java.time.Instant // Dùng để lấy thời gian hiện tại
import java.util.UUID // Dùng để tạo ID tạm
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
                _messages.update { currentList ->
                    if (currentList.none { it.id == newMessage.id }) {
                        listOf(newMessage) + currentList
                    } else {
                        currentList.map { if (it.id == newMessage.id) newMessage else it }
                    }
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

        val tempMessageId = UUID.randomUUID().toString()
        val tempMessage = Message(
            id = tempMessageId,
            conversationId = conversationId,
            senderId = senderId,
            replyToId = null,
            encryptedContent = content,
            messageType = "text",
            isEdited = false,
            seenBy = emptyList(),
            createdAt = Instant.now().toString(),
            deletedAt = null
        )

        _messages.update { currentList ->
            listOf(tempMessage) + currentList
        }

        _messageText.value = ""

        viewModelScope.launch {
            messageRepository.sendMessage(conversationId, senderId, content)
        }
    }
}