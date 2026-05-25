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
