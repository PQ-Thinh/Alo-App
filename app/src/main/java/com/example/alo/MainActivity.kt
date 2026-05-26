package com.example.alo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.alo.domain.repository.AuthRepository
import com.example.alo.domain.repository.UserRepository
import com.example.alo.presentation.theme.AloTheme
import com.example.alo.presentation.navigation.AppNavigation
import com.example.alo.presentation.call.CallViewModel
import com.example.alo.presentation.home.SplashViewModel
import com.example.alo.core.utils.Constant
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val splashViewModel: SplashViewModel by viewModels()
    private val callViewModel: CallViewModel by viewModels()

    @Inject
    lateinit var userRepository: UserRepository

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Log.e("FCM", "Người dùng từ chối quyền thông báo")
        } else {
            splashViewModel.saveFCMToken()
        }
    }

    private var pushConversationId = mutableStateOf<String?>(null)
    private var pushCallId = mutableStateOf<String?>(null)
    private var pushCallerName = mutableStateOf<String?>(null)
    private var pushCallAction = mutableStateOf<String?>(null) // ACCEPT / DECLINE / null

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        pushConversationId.value = intent?.getStringExtra("conversationId")
        pushCallId.value = intent?.getStringExtra("callId")
        pushCallerName.value = intent?.getStringExtra("callerName")
        pushCallAction.value = intent?.action?.takeIf { it == Constant.ACTION_INCOMING_CALL_ACCEPT || it == Constant.ACTION_INCOMING_CALL_DECLINE }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        // Đăng ký Heartbeat
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                super.onStart(owner)
                userRepository.startHeartbeat()
            }

            override fun onStop(owner: LifecycleOwner) {
                userRepository.stopHeartbeat()
                super.onStop(owner)
            }
        })

        splashScreen.setKeepOnScreenCondition {
            splashViewModel.isLoading.value
        }

        setContent {

            AloTheme {
                val startDestination by splashViewModel.startDestination.collectAsState()
                val conversationIdToNavigate by pushConversationId
                val callIdToNavigate by pushCallId
                val callerNameToNavigate by pushCallerName

                val context = androidx.compose.ui.platform.LocalContext.current
                val networkObserver = androidx.compose.runtime.remember { com.example.alo.core.utils.NetworkConnectivityObserver(context) }
                val networkStatus by networkObserver.status.collectAsState(initial = com.example.alo.core.utils.NetworkStatus.Available)
                val isOffline = networkStatus == com.example.alo.core.utils.NetworkStatus.Unavailable || networkStatus == com.example.alo.core.utils.NetworkStatus.Lost

                var showNetworkPrompt by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
                var showOfflineWarning by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
                var hasPromptedForOffline by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }

                androidx.compose.runtime.LaunchedEffect(isOffline) {
                    if (isOffline) {
                        if (!hasPromptedForOffline) {
                            showNetworkPrompt = true
                            hasPromptedForOffline = true
                        }
                    } else {
                        showNetworkPrompt = false
                        showOfflineWarning = false
                        hasPromptedForOffline = false // Sẵn sàng prompt lại cho lần rớt mạng tiếp theo
                    }
                }

                if (showNetworkPrompt) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { 
                            showNetworkPrompt = false 
                            showOfflineWarning = true
                        },
                        title = { androidx.compose.material3.Text("Không có kết nối mạng") },
                        text = { androidx.compose.material3.Text("Ứng dụng cần kết nối Internet để hoạt động đầy đủ tính năng. Bạn có muốn bật kết nối mạng không?") },
                        confirmButton = {
                            androidx.compose.material3.TextButton(
                                onClick = {
                                    showNetworkPrompt = false
                                    context.startActivity(Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS))
                                }
                            ) {
                                androidx.compose.material3.Text("Cài đặt")
                            }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(
                                onClick = {
                                    showNetworkPrompt = false
                                    showOfflineWarning = true
                                }
                            ) {
                                androidx.compose.material3.Text("Bỏ qua")
                            }
                        }
                    )
                }

                if (showOfflineWarning) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showOfflineWarning = false },
                        title = { androidx.compose.material3.Text("Cảnh báo ngoại tuyến") },
                        text = { androidx.compose.material3.Text("Bạn đang mở ứng dụng ở chế độ ngoại tuyến. Dữ liệu sẽ không được đồng bộ cho đến khi kết nối lại mạng.") },
                        confirmButton = {
                            androidx.compose.material3.TextButton(onClick = { showOfflineWarning = false }) {
                                androidx.compose.material3.Text("Đã hiểu")
                            }
                        }
                    )
                }

                androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                    androidx.compose.foundation.layout.Column {
                        com.example.alo.presentation.components.NetworkStatusBanner(isOffline = isOffline)
                        
                        androidx.compose.foundation.layout.Box(modifier = Modifier.weight(1f)) {
                            if (startDestination != null) {
                                AppNavigation(
                                    startDestination = startDestination!!,
                                    pushConversationId = conversationIdToNavigate,
                                    pushCallId = callIdToNavigate,
                                    pushCallerName = callerNameToNavigate,
                                    pushCallAction = pushCallAction.value,
                                    onClearPushDetails = {
                                        pushConversationId.value = null
                                        pushCallId.value = null
                                        pushCallerName.value = null
                                        pushCallAction.value = null
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        askNotificationPermission()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pushConversationId.value = intent.getStringExtra("conversationId")
        pushCallId.value = intent.getStringExtra("callId")
        pushCallerName.value = intent.getStringExtra("callerName")
        pushCallAction.value = intent.action?.takeIf { it == Constant.ACTION_INCOMING_CALL_ACCEPT || it == Constant.ACTION_INCOMING_CALL_DECLINE }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                splashViewModel.saveFCMToken()
            }
        } else {
            splashViewModel.saveFCMToken()
        }
    }



    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val state = callViewModel.uiState.value
            // KHÔNG vào PIP nếu đang kết thúc cuộc gọi (user nhấn nút hủy)
            if (state is com.example.alo.presentation.helper.CallUiState.InCall && !callViewModel.isEndingCall) {
                try {
                    enterPictureInPictureMode(
                        android.app.PictureInPictureParams.Builder()
                            .setAspectRatio(android.util.Rational(9, 16))
                            .build()
                    )
                } catch (_: Exception) {
                }
            }
        }
    }
}