package com.example.alo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alo.domain.repository.AuthRepository
import com.example.alo.domain.repository.FriendRepository
import com.example.alo.presentation.helper.ContactState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactViewModel @Inject constructor(
    private val friendRepository: FriendRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ContactState())
    val state: StateFlow<ContactState> = _state.asStateFlow()

    init {
        fetchPendingRequests()
    }

    fun fetchPendingRequests() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val currentUserId = authRepository.getCurrentAuthUser()?.id
                if (currentUserId == null) {
                    _state.update { it.copy(isLoading = false, error = "Lỗi xác thực!") }
                    return@launch
                }
                val requests = friendRepository.getPendingFriendRequests(currentUserId)
                _state.update { it.copy(isLoading = false, pendingRequests = requests) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Không thể tải lời mời: ${e.message}") }
            }
        }
    }

    fun acceptRequest(senderId: String) {
        viewModelScope.launch {
            try {
                val currentUserId = authRepository.getCurrentAuthUser()?.id ?: return@launch

                val success = friendRepository.acceptFriendRequest(senderId, currentUserId)

                if (success) {
                    val updatedRequests = _state.value.pendingRequests.filter { it.id != senderId }

                    _state.update {
                        it.copy(
                            pendingRequests = updatedRequests,
                            successMessage = "Đã chấp nhận kết bạn!"
                        )
                    }
                } else {
                    _state.update { it.copy(error = "Có lỗi xảy ra, không thể chấp nhận.") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Lỗi: ${e.message}") }
            }
        }
    }

    fun declineRequest(senderId: String) {
        viewModelScope.launch {
            try {
                val currentUserId = authRepository.getCurrentAuthUser()?.id ?: return@launch

                val success = friendRepository.declineFriendRequest(senderId, currentUserId)

                if (success) {

                    val updatedRequests = _state.value.pendingRequests.filter { it.id != senderId }

                    _state.update {
                        it.copy(pendingRequests = updatedRequests)
                    }
                } else {
                    _state.update { it.copy(error = "Có lỗi xảy ra, không thể từ chối.") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Lỗi: ${e.message}") }
            }
        }
    }

    fun clearMessages() {
        _state.update { it.copy(error = null, successMessage = null) }
    }
}