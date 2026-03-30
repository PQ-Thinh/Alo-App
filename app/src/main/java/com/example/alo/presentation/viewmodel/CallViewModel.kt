package com.example.alo.presentation.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alo.domain.repository.AuthRepository
import com.example.alo.domain.repository.MessageRepository
import com.example.alo.domain.repository.UserRepository
import com.example.alo.domain.repository.VideoCallRepository
import com.example.alo.data.service.CallForegroundService
import com.example.alo.presentation.helper.CallUiState
import com.example.alo.presentation.helper.NetworkStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.getstream.android.video.generated.models.CallEndedEvent
import io.getstream.android.video.generated.models.CallRejectedEvent
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.StreamVideo

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import javax.inject.Inject



@HiltViewModel
class CallViewModel @Inject constructor(
    private val videoCallRepository: VideoCallRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val messageRepository: MessageRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    companion object {
        private const val TAG = "CallViewModel"
        private const val CALL_TIMEOUT_MS = 45_000L
    }

    private val connectivityManager: ConnectivityManager by lazy {
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private val _uiState = MutableStateFlow<CallUiState>(CallUiState.Idle)
    val uiState: StateFlow<CallUiState> = _uiState.asStateFlow()

    private val _networkStatus = MutableStateFlow(NetworkStatus.Connected)
    val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()

    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var activeCall: Call? = null
    private var currentCallId: String? = null
    private var currentMemberIds: List<String> = emptyList()
    private var isCaller: Boolean = false
    private var callTimeoutJob: Job? = null
    private var cancelPushSent: Boolean = false
    private var isRejoining: Boolean = false
    private var callStartTimestamp: Long? = null

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
            if (currentCallId == callId && (_uiState.value is CallUiState.Calling || _uiState.value is CallUiState.InCall)) {
                Log.d(TAG, "startCall bỏ qua: đang trong cuộc gọi $callId")
                return@launch
            }
            currentCallId = callId
            currentMemberIds = memberIds
            isCaller = true
            cancelPushSent = false
            callStartTimestamp = null
            try {
                _uiState.value = CallUiState.Initializing
                val startTime = System.currentTimeMillis()
                CallForegroundService.startOutgoing(
                    context = appContext,
                    callId = callId,
                    peerName = null
                )
                val call = videoCallRepository.createAndJoinCall(
                    callId = callId,
                    memberIds = memberIds
                )
                activeCall = call
                startNetworkMonitor()
                startCallTimeoutWatcher()

                val authUser = authRepository.getCurrentAuthUser()
                if (authUser != null) {
                    val receiverIds = memberIds.filter { it != authUser.id }
                    if (receiverIds.isNotEmpty()) {
                        videoCallRepository.pushIncomingCall(
                            callId = callId,
                            senderId = authUser.id,
                            receiverIds = receiverIds,
                            type = "INCOMING_CALL"
                        )
                    }
                }
                call.subscribe { event ->
                    try {
                        when (event) {
                            is CallRejectedEvent,
                            is CallEndedEvent -> {
                                Log.d(TAG, "Cuộc gọi bị từ chối/kết thúc bởi hệ thống/đối tác")
                                logCallMessage(reason = "CALL_MISSED")
                                _uiState.value = CallUiState.Ended
                                stopNetworkMonitor()
                                callTimeoutJob?.cancel()
                                activeCall = null
                                CallForegroundService.stop(appContext)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Lỗi khi xử lý CallEvent", e)
                    }
                }

                _uiState.value = CallUiState.Calling(call)
                Log.d(TAG, "Outgoing call bắt đầu: $callId, setupTime=${System.currentTimeMillis() - startTime}ms")
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
            if (currentCallId == callId && (_uiState.value is CallUiState.Calling || _uiState.value is CallUiState.InCall)) {
                Log.d(TAG, "acceptCall bỏ qua: đã join $callId")
                return@launch
            }
            isCaller = false
            currentCallId = callId
            callStartTimestamp = null
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

                val startTime = System.currentTimeMillis()
                val call = videoCallRepository.joinCall(callId = callId)
                activeCall = call
                startNetworkMonitor()

                call.accept()
                callStartTimestamp = System.currentTimeMillis()
                CallForegroundService.notifyConnected(appContext, callId)

                call.subscribe { event ->
                    try {
                        when (event) {
                            is CallEndedEvent -> {
                                Log.d(TAG, "Cuộc gọi kết thúc do đối tác gác máy")
                                logCallMessage(reason = "CALL_ENDED")
                                _uiState.value = CallUiState.Ended
                                stopNetworkMonitor()
                                callTimeoutJob?.cancel()
                                activeCall = null
                                CallForegroundService.stop(appContext)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Lỗi khi xử lý CallEvent", e)
                    }
                }

                _uiState.value = CallUiState.InCall(call)
                Log.d(TAG, "Đã join phòng call thành công: $callId, joinTime=${System.currentTimeMillis() - startTime}ms")
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
                CallForegroundService.stop(appContext)
            }
        }
    }
    // ────────────────────────────────────────────────────
    // Khi người kia picked up (Calling → InCall)
    // ────────────────────────────────────────────────────
    fun onCallAccepted(call: Call) {
        _uiState.value = CallUiState.InCall(call)
        callTimeoutJob?.cancel()
        callStartTimestamp = System.currentTimeMillis()
        currentCallId?.let { CallForegroundService.notifyConnected(appContext, it) }
    }

    // ────────────────────────────────────────────────────
    // Kết thúc / Từ chối cuộc gọi
    // ────────────────────────────────────────────────────
    fun endCall() {
        viewModelScope.launch {
            val current = _uiState.value
            try {
                when (current) {
                    is CallUiState.Calling -> {
                        current.call.leave()
                        if (isCaller && !cancelPushSent) {
                            sendCancelPush()
                        }
                        logCallMessage(reason = "CALL_CANCELLED")
                    }
                    is CallUiState.InCall -> {
                        current.call.leave()
                        logCallMessage(reason = "CALL_ENDED")
                    }
                    else -> Unit
                }
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi kết thúc cuộc gọi: ${e.message}", e)
            } finally {
                stopNetworkMonitor()
                callTimeoutJob?.cancel()
                activeCall = null
                _uiState.value = CallUiState.Ended
                CallForegroundService.stop(appContext)
            }
        }
    }

    fun resetToIdle() {
        _uiState.value = CallUiState.Idle
        cancelPushSent = false
        currentCallId = null
        currentMemberIds = emptyList()
        isCaller = false
        callStartTimestamp = null
    }

    override fun onCleared() {
        super.onCleared()
        stopNetworkMonitor()
        callTimeoutJob?.cancel()
        activeCall = null
        // Không cleanup StreamVideo ở đây vì nó là Singleton (logout sẽ cleanup)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // MẠNG & TIMEOUT
    // ─────────────────────────────────────────────────────────────────────────────
    private fun startNetworkMonitor() {
        if (networkCallback != null) return
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _networkStatus.value = NetworkStatus.Connected
                activeCall?.let { call ->
                    if (isRejoining) return
                    isRejoining = true
                    viewModelScope.launch {
                        try {
                            call.join(create = false)
                        } catch (e: Exception) {
                            Log.e(TAG, "Reconnect join failed: ${e.message}")
                        } finally {
                            isRejoining = false
                        }
                    }
                }
            }

            override fun onLost(network: Network) {
                _networkStatus.value = NetworkStatus.Reconnecting
            }
        }
        connectivityManager.registerNetworkCallback(request, networkCallback!!)
    }

    private fun stopNetworkMonitor() {
        networkCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
            } catch (_: Exception) {
            }
        }
        networkCallback = null
        _networkStatus.value = NetworkStatus.Connected
    }

    private fun startCallTimeoutWatcher() {
        callTimeoutJob?.cancel()
        callTimeoutJob = viewModelScope.launch {
            delay(CALL_TIMEOUT_MS)
            if (_uiState.value is CallUiState.Calling) {
                Log.d(TAG, "Call timeout, ending callId=$currentCallId")
                sendCancelPush("MISSED_CALL")
                logCallMessage(reason = "CALL_MISSED")
                endCall()
            }
        }
    }

    private suspend fun sendCancelPush(type: String = "CALL_CANCELLED") {
        try {
            val authUser = authRepository.getCurrentAuthUser() ?: return
            if (currentMemberIds.isNotEmpty() && currentCallId != null) {
                videoCallRepository.pushIncomingCall(
                    callId = currentCallId!!,
                    senderId = authUser.id,
                    receiverIds = currentMemberIds.filter { it != authUser.id },
                    type = type
                )
                cancelPushSent = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gửi CANCEL push thất bại: ${e.message}")
        }
    }

    private fun logCallMessage(reason: String, durationMs: Long? = null) {
        val conversationId = currentCallId ?: return
        viewModelScope.launch {
            try {
                val authUser = authRepository.getCurrentAuthUser() ?: return@launch
                val measuredMs = durationMs ?: callStartTimestamp?.let { start ->
                    System.currentTimeMillis() - start
                }
                val formattedDuration = measuredMs?.let { ms ->
                    val totalSec = (ms / 1000).coerceAtLeast(0)
                    val min = totalSec / 60
                    val sec = totalSec % 60
                    String.format("%d:%02d", min, sec)
                }
                val preview = when (reason) {
                    "CALL_MISSED" -> "\uD83D\uDCDE Cuộc gọi nhỡ"
                    "CALL_CANCELLED" -> "\uD83D\uDCDE Cuộc gọi đã hủy"
                    else -> "\uD83D\uDCF9 Cuộc gọi video - ${formattedDuration ?: "--:--"}"
                }
                messageRepository.sendMessage(
                    conversationId = conversationId,
                    messageType = reason,
                    senderId = authUser.id,
                    content = preview
                )
            } catch (_: Exception) {
            }
        }
    }
}
