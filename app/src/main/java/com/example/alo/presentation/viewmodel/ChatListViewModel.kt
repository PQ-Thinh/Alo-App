package com.example.alo.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alo.data.utils.CryptoHelper
import com.example.alo.domain.repository.AuthRepository
import com.example.alo.domain.repository.ConversationRepository
import com.example.alo.domain.repository.UserRepository
import com.example.alo.presentation.helper.ChatListState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val conversationRepository: ConversationRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
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
                    fetchChatList(isSilentRefresh = true)
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

                //Tải danh sách thô (chứa JSON) từ DB về
                val rawChats = conversationRepository.getChatList(currentUser.id)

                // ====================================================================
                // GIẢI MÃ PREVIEW CHO TỪNG PHÒNG CHAT (CHẠY SONG SONG)
                // ====================================================================
                val decryptedChats = rawChats.map { chat ->
                    async {
                        val previewJson = chat.lastMessagePreview

                        if (previewJson.isNullOrBlank() || !previewJson.contains("for_sender")) {
                            return@async chat
                        }

                        // Kiểm tra xem ai là người gửi tin nhắn cuối cùng này
                        val isMine = (chat.lastMessageSenderId == currentUser.id)
                        var senderSignKey: String? = null

                        // Nếu không phải mình gửi, phải tìm Public Sign Key của người kia để soi mộc
                        if (!isMine && chat.lastMessageSenderId != null) {
                            try {
                                val senderProfile = userRepository.getCurrentUser(chat.lastMessageSenderId!!)
                                senderSignKey = senderProfile?.publicSignKey
                            } catch (e: Exception) {
                                // Lỗi lấy profile thì Sign Key = null,
                            }
                        }

                        // Gọi cỗ máy Tink giải mã
                        val clearText = CryptoHelper.decryptMessage(
                            context = context,
                            encryptedJson = previewJson,
                            senderPublicSignKeyBase64 = senderSignKey,
                            isMyMessage = isMine
                        )

                        chat.copy(lastMessagePreview = clearText)
                    }
                }.awaitAll() // Đợi tất cả các tiến trình giải mã hoàn tất

                //  Đẩy danh sách đã giải mã đẹp đẽ lên UI
                _state.update {
                    it.copy(
                        isLoading = false,
                        chatList = decryptedChats,
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