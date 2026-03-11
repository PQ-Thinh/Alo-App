package com.example.alo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.alo.domain.repository.AuthRepository
import com.example.alo.domain.repository.UserRepository
import com.example.alo.presentation.theme.AloTheme
import com.example.alo.presentation.view.navigation.AppNavigation
import com.example.alo.presentation.viewmodel.SplashViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val splashViewModel: SplashViewModel by viewModels()

    @Inject
    lateinit var authRepository: AuthRepository

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

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        pushConversationId.value = intent?.getStringExtra("conversationId")

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

                // Lắng nghe state của conversationId
                val conversationIdToNavigate by pushConversationId

                if (startDestination != null) {
                    AppNavigation(
                        startDestination = startDestination!!,
                        pushConversationId = conversationIdToNavigate
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
}