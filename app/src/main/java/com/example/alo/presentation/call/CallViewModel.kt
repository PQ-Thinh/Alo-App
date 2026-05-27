package com.example.alo.presentation.call

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
import com.example.alo.core.service.CallForegroundService
import com.example.alo.core.utils.Constant
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
import kotlinx.coroutines.Dispatchers
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
    private var currentConversationId: String? = null
    private var currentMemberIds: List<String> = emptyList()
    private var isCaller: Boolean = false
    private var callTimeoutJob: Job? = null
    private var cancelPushSent: Boolean = false
    private var isRejoining: Boolean = false
    private var callStartTimestamp: Long? = null
    private var isCallLogged: Boolean = false
    private var initJob: Job? = null
    private var _isEndingCall: Boolean = false
    val isEndingCall: Boolean get() = _isEndingCall
    private var callEventSubscription: kotlinx.coroutines.DisposableHandle? = null

    // StateFlow để UI observe callId mới → navigate đến OutgoingCallScreen
    private val _currentCallIdFlow = MutableStateFlow<String?>(null)
    val currentCallIdFlow: StateFlow<String?> = _currentCallIdFlow.asStateFlow()

    // Tạo callId unique cho mỗi cuộc gọi — format dễ debug
    private fun generateCallId(conversationId: String): String {
        return "${conversationId}_${System.currentTimeMillis()}"
    }

    // Trích xuất conversationId từ unique callId
    // Format: "{conversationId}_{timestamp}" → lấy phần trước "_" cuối cùng
    private fun extractConversationId(callId: String): String {
        val lastUnderscore = callId.lastIndexOf('_')
        return if (lastUnderscore > 0) callId.substring(0, lastUnderscore) else callId
    }

    fun initStreamClient() {
        if (initJob?.isActive == true) return
        initJob = viewModelScope.launch {
            try {
                if (StreamVideo.isInstalled) {
                    _uiState.value = CallUiState.Idle
                    return@launch
                }
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

    private suspend fun ensureStreamInitialized() {
        if (!StreamVideo.isInstalled) {
            initStreamClient()
            initJob?.join()
        }
    }

    fun startCall(conversationId: String, memberIds: List<String>) {
        viewModelScope.launch {
            // Guard: không gọi trùng khi đang có cuộc gọi
            if (_uiState.value is CallUiState.Calling || _uiState.value is CallUiState.InCall) {
                return@launch
            }

            val callId = generateCallId(conversationId)
            currentCallId = callId
            currentConversationId = conversationId
            currentMemberIds = memberIds
            isCaller = true
            cancelPushSent = false
            callStartTimestamp = null
            isCallLogged = false
            _currentCallIdFlow.value = callId  // UI observe để navigate

            try {
                _uiState.value = CallUiState.Initializing
                ensureStreamInitialized()
                
                if (!StreamVideo.isInstalled) {
                    throw Exception("Không thể khởi tạo StreamVideo. Vui lòng kiểm tra lại cấu hình API Key hoặc mạng.")
                }

                CallForegroundService.startOutgoing(appContext, callId, null)
                val call = videoCallRepository.createAndJoinCall(callId, memberIds)
                activeCall = call
                startNetworkMonitor()
                startCallTimeoutWatcher()

                // Cleanup subscription cũ trước khi subscribe mới
                callEventSubscription?.dispose()
                callEventSubscription = call.subscribe { event ->
                    try {
                        when (event) {
                            is CallRejectedEvent, is CallEndedEvent -> {
                                if (isCallLogged) return@subscribe
                                val reason = if (event is CallRejectedEvent) Constant.EVENT_CALL_REJECTED else Constant.EVENT_CALL_ENDED
                                logCallMessage(reason)
                                _uiState.value = CallUiState.Ended
                                cleanupCallResources()
                            }
                            else -> Unit
                        }
                    } catch (_: Exception) {}
                }

                _uiState.value = CallUiState.Calling(call)
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi tạo cuộc gọi nghiêm trọng: ${e.message}", e)
                _uiState.value = CallUiState.Error("Lỗi hệ thống: ${e.message}")
                _currentCallIdFlow.value = null
            }
        }
    }

    fun acceptCall(callId: String) {
        viewModelScope.launch {
            if (_uiState.value is CallUiState.InCall || _uiState.value is CallUiState.Calling) return@launch
            isCaller = false
            currentCallId = callId
            currentConversationId = extractConversationId(callId)  // Trích xuất conversationId cho logCallMessage
            callStartTimestamp = null
            isCallLogged = false
            try {
                ensureStreamInitialized()
                _uiState.value = CallUiState.Initializing
                val call = videoCallRepository.joinCall(callId)
                activeCall = call
                startNetworkMonitor()
                call.accept()
                callStartTimestamp = System.currentTimeMillis()
                CallForegroundService.notifyConnected(appContext, callId)

                // Cleanup subscription cũ trước khi subscribe mới
                callEventSubscription?.dispose()
                callEventSubscription = call.subscribe { event ->
                    try {
                        when (event) {
                            is CallEndedEvent, is CallRejectedEvent -> {
                                if (isCallLogged) return@subscribe
                                logCallMessage(if (event is CallRejectedEvent) Constant.EVENT_CALL_REJECTED else Constant.EVENT_CALL_ENDED)
                                _uiState.value = CallUiState.Ended
                                cleanupCallResources()
                            }
                            else -> Unit
                        }
                    } catch (_: Exception) {}
                }
                _uiState.value = CallUiState.InCall(call)
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi join call: ${e.message}")
                _uiState.value = CallUiState.Error("Không thể tham gia cuộc gọi.")
            }
        }
    }

    fun rejectCall(callId: String) {
        viewModelScope.launch {
            try {
                // Chỉ cố gắng khởi tạo nếu chưa có, không block quá lâu khi reject
                if (!StreamVideo.isInstalled) {
                    initStreamClient()
                    delay(1000)
                }
                if (StreamVideo.isInstalled) {
                    videoCallRepository.rejectCall(callId)
                }
                logCallMessage(Constant.EVENT_CALL_REJECTED)
            } catch (_: Exception) {
            } finally {
                _uiState.value = CallUiState.Ended
                CallForegroundService.stop(appContext)
            }
        }
    }

    fun onCallAccepted(call: Call) {
        _uiState.value = CallUiState.InCall(call)
        callTimeoutJob?.cancel()
        callStartTimestamp = System.currentTimeMillis()
        currentCallId?.let { CallForegroundService.notifyConnected(appContext, it) }
    }

    fun endCall() {
        _isEndingCall = true
        viewModelScope.launch {
            val current = _uiState.value
            try {
                when (current) {
                    is CallUiState.Calling -> {
                        // Đang ring → reject + leave (gửi tín hiệu hủy cho đối phương)
                        if (isCaller) {
                            val reason = if (!cancelPushSent) Constant.EVENT_CALL_CANCELLED else Constant.EVENT_CALL_ENDED
                            logCallMessage(reason)
                        } else {
                            logCallMessage(Constant.EVENT_CALL_ENDED)
                        }
                        try { current.call.reject() } catch (_: Exception) {}
                        try { current.call.leave() } catch (_: Exception) {}
                    }
                    is CallUiState.InCall -> {
                        // Đang active → end() kết thúc cho CẢ HAI (an toàn với unique callId)
                        logCallMessage(Constant.EVENT_CALL_ENDED)
                        try {
                            current.call.end()
                        } catch (_: Exception) {
                            // Fallback nếu không phải creator
                            try { current.call.leave() } catch (_: Exception) {}
                        }
                    }
                    else -> Unit
                }
            } catch (_: Exception) {
            } finally {
                cleanupCallResources()
                _uiState.value = CallUiState.Ended
            }
        }
    }

    fun resetToIdle() {
        _uiState.value = CallUiState.Idle
        cancelPushSent = false
        currentCallId = null
        currentConversationId = null
        _currentCallIdFlow.value = null
        currentMemberIds = emptyList()
        isCaller = false
        callStartTimestamp = null
        isCallLogged = false
        _isEndingCall = false
    }

    override fun onCleared() {
        super.onCleared()
        cleanupCallResources()
    }

    private fun cleanupCallResources() {
        callEventSubscription?.dispose()
        callEventSubscription = null
        stopNetworkMonitor()
        callTimeoutJob?.cancel()
        activeCall = null
        CallForegroundService.stop(appContext)
    }

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
                        } catch (_: Exception) {
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
            } catch (_: Exception) {}
        }
        networkCallback = null
        _networkStatus.value = NetworkStatus.Connected
    }

    private fun startCallTimeoutWatcher() {
        callTimeoutJob?.cancel()
        callTimeoutJob = viewModelScope.launch {
            delay(CALL_TIMEOUT_MS)
            if (_uiState.value is CallUiState.Calling) {
                logCallMessage(Constant.EVENT_CALL_MISSED)
                endCall()
            }
        }
    }


    private fun logCallMessage(reason: String, durationMs: Long? = null) {
        if (isCallLogged) return
        val conversationId = currentConversationId ?: return
        isCallLogged = true
        viewModelScope.launch {
            try {
                val authUser = authRepository.getCurrentAuthUser() ?: return@launch
                val measuredMs = durationMs ?: callStartTimestamp?.let { System.currentTimeMillis() - it }
                val formattedDuration = measuredMs?.let { ms ->
                    val totalSec = (ms / 1000).coerceAtLeast(0)
                    String.format("%d:%02d", totalSec / 60, totalSec % 60)
                }
                val preview = when (reason) {
                    "CALL_MISSED" -> "\uD83D\uDCDE Cuộc gọi nhỡ"
                    "CALL_CANCELLED" -> "\uD83D\uDCDE Cuộc gọi đã hủy"
                    else -> "\uD83D\uDCF9 Cuộc gọi video - ${formattedDuration ?: "--:--"}"
                }
                val messageId = messageRepository.sendMessage(
                    conversationId = conversationId,
                    senderId = authUser.id,
                    messageType = reason,
                    content = preview
                )
                videoCallRepository.sendCallLog(
                    messageId = messageId,
                    durationSec = measuredMs?.let { (it / 1000).toInt() } ?: 0,
                    direction = if (isCaller) "outgoing" else "incoming",
                    reason = reason,
                    isVideo = true
                )
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi ghi log: ${e.message}")
            }
        }
    }
}
