package com.example.alo.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alo.domain.model.Message
import com.example.alo.domain.repository.AuthRepository
import com.example.alo.domain.repository.ConversationRepository
import com.example.alo.domain.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

    private val _partnerName = MutableStateFlow("Đang tải...")
    val partnerName: StateFlow<String> = _partnerName.asStateFlow()

    private val _partnerAvatar = MutableStateFlow("")
    val partnerAvatar: StateFlow<String> = _partnerAvatar.asStateFlow()

    private val _isPartnerTyping = MutableStateFlow(false)
    val isPartnerTyping: StateFlow<Boolean> = _isPartnerTyping.asStateFlow()

    private var typingJob: Job? = null
    private var lastTypingTime = 0L

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

                    val chatList = conversationRepository.getChatList(user.id)
                    val currentChatInfo = chatList.find { it.conversationId == conversationId }

                    if (currentChatInfo != null) {
                        _partnerName.value = currentChatInfo.chatName ?: "Người dùng ẩn danh"
                        _partnerAvatar.value = currentChatInfo.chatAvatar ?: ""
                    }
                } catch (e: Exception) {}
            }

            val historyMessages = messageRepository.getMessages(conversationId)
            _messages.value = historyMessages

            historyMessages.forEach { msg ->
                if (msg.senderId != user?.id && !msg.seenBy.contains(user?.id)) {
                    viewModelScope.launch {
                        messageRepository.markMessageAsSeen(msg.id, user!!.id)
                    }
                }
            }
            messageRepository.subscribeToNewMessages(conversationId
            , onTyping = { typingUserId ->
                    if (typingUserId != _currentUserId.value) {
                        typingJob?.cancel()
                        typingJob = viewModelScope.launch {
                            _isPartnerTyping.value = true
                            delay(3000) //
                            _isPartnerTyping.value = false
                        }
                    }
                }
                ).collect { newMessage ->
                if (newMessage.senderId != user?.id && !newMessage.seenBy.contains(user?.id)) {
                    viewModelScope.launch {
                        messageRepository.markMessageAsSeen(newMessage.id, user!!.id)
                    }
                }

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
        val currentTime = System.currentTimeMillis()
        if (text.isNotEmpty() && (currentTime - lastTypingTime > 2000)) {
            lastTypingTime = currentTime
            viewModelScope.launch {
                messageRepository.sendTypingEvent(conversationId, _currentUserId.value)
            }
        }
    }

    fun sendMessage() {
        val content = _messageText.value.trim()
        val senderId = _currentUserId.value

        if (content.isEmpty() || senderId.isEmpty()) return

        _messageText.value = ""

        viewModelScope.launch {
            messageRepository.sendMessage(conversationId, senderId, content)
        }
    }
    fun addReaction(messageId: String, reactionIcon: String) {
        viewModelScope.launch {
            val userId = _currentUserId.value
            if (userId.isNotEmpty()) {
                messageRepository.addReaction(messageId, userId, reactionIcon)
            }
        }
    }
}