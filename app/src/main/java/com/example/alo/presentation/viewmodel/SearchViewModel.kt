package com.example.alo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alo.domain.repository.AuthRepository
import com.example.alo.domain.repository.ConversationRepository
import com.example.alo.domain.repository.FriendRepository
import com.example.alo.domain.repository.UserRepository
import com.example.alo.presentation.helper.SearchState
import com.example.alo.presentation.helper.UserSearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val friendRepository: FriendRepository,
    private val authRepository: AuthRepository,
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()


    // Hàm thực hiện tìm kiếm
    fun searchUsers(query: String) {
        if (query.isBlank()) {
            _state.update { it.copy(searchResults = emptyList(), error = null) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, successMessage = null) }

            try {
                val currentUserId = authRepository.getCurrentAuthUser()?.id
                if (currentUserId == null) {
                    _state.update { it.copy(isLoading = false, error = "Lỗi xác thực!") }
                    return@launch
                }

                val rawUsers = userRepository.searchUsers(query)

                val filteredUsers = rawUsers.filter { it.id != currentUserId }

                val resultsWithStatus = filteredUsers.map { user ->
                    val status = friendRepository.checkFriendStatus(currentUserId, user.id)
                    UserSearchResult(user, status)
                }

                _state.update {
                    it.copy(isLoading = false, searchResults = resultsWithStatus)
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = "Lỗi tìm kiếm: ${e.message}")
                }
            }
        }
    }

    // Hàm gửi lời mời kết bạn
    fun sendFriendRequest(receiverId: String) {
        viewModelScope.launch {
            try {
                val currentUserId = authRepository.getCurrentAuthUser()?.id ?: return@launch

                val success = friendRepository.sendFriendRequest(currentUserId, receiverId)

                if (success) {
                    _state.update { currentState ->
                        val updatedList = currentState.searchResults.map {
                            if (it.user.id == receiverId) it.copy(relationStatus = "request_sent")
                            else it
                        }
                        currentState.copy(
                            searchResults = updatedList,
                            successMessage = "Đã gửi lời mời kết bạn!"
                        )
                    }
                } else {
                    _state.update { it.copy(error = "Không thể gửi lời mời, vui lòng thử lại!") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Lỗi: ${e.message}") }
            }
        }
    }
    fun acceptFriendRequest(senderId: String) {
        viewModelScope.launch {
            val currentUserId = authRepository.getCurrentAuthUser()?.id ?: return@launch
            _state.value = _state.value.copy(isLoading = true)
            val success = friendRepository.acceptFriendRequest(senderId,currentUserId)
            if (success) {
                _state.value = _state.value.copy(successMessage = "Đã trở thành bạn bè!")
                val updatedList = _state.value.searchResults.map {
                    if (it.user.id == senderId) it.copy(relationStatus = "friends") else it
                }
                _state.value = _state.value.copy(searchResults = updatedList, isLoading = false)
            } else {
                _state.value = _state.value.copy(error = "Lỗi chấp nhận kết bạn", isLoading = false)
            }
        }
    }
    fun getOrCreateChatAndNavigate(targetUserId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val currentId = authRepository.getCurrentAuthUser()?.id ?: return@launch

            val convId = conversationRepository.getOrCreateDirectConversation(targetUserId,currentId)

            if (convId != null) {
                _state.update { it.copy(isLoading = false, navigateToConversationId = convId) }
            } else {
                _state.update { it.copy(isLoading = false, error = "Không thể tạo phòng chat") }
            }
        }
    }
    fun resetNavigation() {
        _state.update { it.copy(navigateToConversationId = null) }
    }
    // Hàm xóa thông báo (để UI gọi sau khi hiển thị Toast)
    fun clearMessages() {
        _state.update { it.copy(error = null, successMessage = null) }
    }
}