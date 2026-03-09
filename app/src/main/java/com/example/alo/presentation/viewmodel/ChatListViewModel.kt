package com.example.alo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alo.domain.repository.AuthRepository
import com.example.alo.domain.repository.ConversationRepository
import com.example.alo.presentation.helper.ChatListState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatListState())
    val state: StateFlow<ChatListState> = _state.asStateFlow()

    init {
        fetchChatList()
        observeGlobalUpdates()
    }

    private fun observeGlobalUpdates() {
        viewModelScope.launch {
            val currentUser = authRepository.getCurrentAuthUser()
            if (currentUser != null) {

                conversationRepository.subscribeToChatListUpdates(currentUser.id).collect {
                    fetchChatList()
                }
            }
        }
    }

    fun fetchChatList(isSilentRefresh: Boolean = false) {
        viewModelScope.launch {
            val currentList = _state.value.chatList

            if (currentList.isEmpty() && !isSilentRefresh) {
                _state.update { it.copy(isLoading = true, error = null) }
            }

            try {
                if (isSilentRefresh) {
                    delay(500L)
                }

                val currentUser = authRepository.getCurrentAuthUser()
                if (currentUser == null) {
                    if (currentList.isEmpty()) {
                        _state.update {
                            it.copy(isLoading = false, error = "Lỗi bảo mật: Bạn chưa đăng nhập!")
                        }
                    }
                    return@launch
                }

                val chats = conversationRepository.getChatList(currentUser.id)

                _state.update {
                    it.copy(
                        isLoading = false,
                        chatList = chats,
                        error = null
                    )
                }
            } catch (e: Exception) {

                if (currentList.isNotEmpty()) {
                    _state.update { it.copy(isLoading = false) }
                } else {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Không thể tải danh sách tin nhắn: ${e.message}"
                        )
                    }
                }
            }
        }
    }
}