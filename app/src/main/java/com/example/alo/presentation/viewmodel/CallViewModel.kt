package com.example.alo.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alo.domain.repository.AuthRepository
import com.example.alo.domain.repository.UserRepository
import com.example.alo.domain.repository.VideoCallRepository
import com.example.alo.presentation.helper.CallUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import io.getstream.android.video.generated.models.CallEndedEvent
import io.getstream.android.video.generated.models.CallRejectedEvent
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.StreamVideo

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject



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
                
                // Kích hoạt API Push Notification "cây nhà lá vườn" (đã code trên Supabase)
                val authUser = authRepository.getCurrentAuthUser()
                if (authUser != null) {
                    val receiverIds = memberIds.filter { it != authUser.id }
                    if (receiverIds.isNotEmpty()) {
                        videoCallRepository.pushIncomingCall(
                            callId = callId,
                            senderId = authUser.id,
                            receiverIds = receiverIds
                        )
                    }
                }
                // Lắng nghe sự kiện để tự động gác máy nếu bị từ chối hoặc hết giờ
                call.subscribe { event ->
                    try {
                        when (event) {
                            is CallRejectedEvent,
                            is CallEndedEvent -> {
                                Log.d(TAG, "Cuộc gọi bị từ chối/kết thúc bởi hệ thống/đối tác")
                                _uiState.value = CallUiState.Ended
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Lỗi khi xử lý CallEvent", e)
                    }
                }

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
                Log.d(TAG, "Bắt đầu tiến trình Accept Call: $callId")

                if (!StreamVideo.isInstalled) {
                    Log.d(TAG, "Stream SDK chưa ready (App mở từ nền). Đang khởi tạo khẩn cấp...")
                    val authUser = authRepository.getCurrentAuthUser() ?: throw Exception("Chưa đăng nhập.")
                    val userProfile = userRepository.getCurrentUser(authUser.id)

                    videoCallRepository.initStreamClient(
                        userId = authUser.id,
                        displayName = userProfile?.displayName ?: authUser.email,
                        avatarUrl = userProfile?.avatarUrl
                    )
                }

                val call = videoCallRepository.joinCall(callId = callId)

                call.accept()

                call.subscribe { event ->
                    try {
                        when (event) {
                            is CallEndedEvent -> {
                                Log.d(TAG, "Cuộc gọi kết thúc do đối tác gác máy")
                                _uiState.value = CallUiState.Ended
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Lỗi khi xử lý CallEvent", e)
                    }
                }

                _uiState.value = CallUiState.InCall(call)
                Log.d(TAG, "Đã join phòng call thành công: $callId")
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi join call khốc liệt: ${e.message}", e)
                _uiState.value = CallUiState.Error("Không thể tham gia cuộc gọi: ${e.message}")
            }
        }
    }
    fun rejectCall(callId: String) {
        viewModelScope.launch {
            try {
                videoCallRepository.rejectCall(callId = callId)
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi từ chối cuộc gọi: ${e.message}", e)
            } finally {
                _uiState.value = CallUiState.Ended
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