package com.example.alo.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alo.domain.repository.AuthRepository
import com.example.alo.domain.repository.UserRepository
import com.example.alo.domain.repository.VideoCallRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.getstream.video.android.core.Call
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class CallUiState {
    object Idle : CallUiState()
    object Initializing : CallUiState()
    data class Calling(val call: Call) : CallUiState()   // outgoing – đang ring
    data class InCall(val call: Call) : CallUiState()    // đã kết nối (active)
    object Ended : CallUiState()
    data class Error(val message: String) : CallUiState()
}

@HiltViewModel
class CallViewModel @Inject constructor(
    private val videoCallRepository: VideoCallRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    companion object {
        private const val TAG = "CallViewModel"
    }

    private val _uiState = MutableStateFlow<CallUiState>(CallUiState.Idle)
    val uiState: StateFlow<CallUiState> = _uiState.asStateFlow()

    // ────────────────────────────────────────────────────
    // Khởi tạo Stream client (gọi ngay sau login success)
    // ────────────────────────────────────────────────────
    fun initStreamClient() {
        viewModelScope.launch {
            try {
                _uiState.value = CallUiState.Initializing
                val authUser = authRepository.getCurrentAuthUser() ?: run {
                    _uiState.value = CallUiState.Error("Chưa đăng nhập.")
                    return@launch
                }
                val userProfile = userRepository.getCurrentUser(authUser.id)
                videoCallRepository.initStreamClient(
                    userId = authUser.id,
                    displayName = userProfile?.displayName ?: authUser.email,
                    avatarUrl = userProfile?.avatarUrl
                )
                _uiState.value = CallUiState.Idle
                Log.d(TAG, "Stream client sẵn sàng.")
            } catch (e: Exception) {
                Log.e(TAG, "Khởi tạo Stream thất bại: ${e.message}", e)
                _uiState.value = CallUiState.Error("Không thể kết nối video call: ${e.message}")
            }
        }
    }

    // ────────────────────────────────────────────────────
    // Tạo cuộc gọi đi (Outgoing call)
    // ────────────────────────────────────────────────────
    fun startCall(callId: String, memberIds: List<String>) {
        viewModelScope.launch {
            try {
                _uiState.value = CallUiState.Initializing
                val call = videoCallRepository.createAndJoinCall(
                    callId = callId,
                    memberIds = memberIds
                )
                _uiState.value = CallUiState.Calling(call)
                Log.d(TAG, "Outgoing call bắt đầu: $callId")
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi tạo cuộc gọi: ${e.message}", e)
                _uiState.value = CallUiState.Error("Không thể bắt đầu cuộc gọi: ${e.message}")
            }
        }
    }

    // ────────────────────────────────────────────────────
    // Chấp nhận cuộc gọi đến (Incoming call – Accept)
    // ────────────────────────────────────────────────────
    fun acceptCall(callId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = CallUiState.Initializing
                val call = videoCallRepository.joinCall(callId = callId)
                _uiState.value = CallUiState.InCall(call)
                Log.d(TAG, "Đã chấp nhận cuộc gọi: $callId")
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi join call: ${e.message}", e)
                _uiState.value = CallUiState.Error("Không thể tham gia cuộc gọi: ${e.message}")
            }
        }
    }

    // ────────────────────────────────────────────────────
    // Khi người kia picked up (Calling → InCall)
    // ────────────────────────────────────────────────────
    fun onCallAccepted(call: Call) {
        _uiState.value = CallUiState.InCall(call)
    }

    // ────────────────────────────────────────────────────
    // Kết thúc / Từ chối cuộc gọi
    // ────────────────────────────────────────────────────
    fun endCall() {
        viewModelScope.launch {
            val current = _uiState.value
            try {
                when (current) {
                    is CallUiState.Calling -> current.call.leave()
                    is CallUiState.InCall -> current.call.leave()
                    else -> Unit
                }
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi kết thúc cuộc gọi: ${e.message}", e)
            } finally {
                _uiState.value = CallUiState.Ended
            }
        }
    }

    fun resetToIdle() {
        _uiState.value = CallUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        // Không cleanup StreamVideo ở đây vì nó là Singleton (logout sẽ cleanup)
    }
}