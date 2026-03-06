package com.example.alo

import android.os.Bundle
import android.util.Log // <-- Import Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.alo.domain.repository.AuthRepository
import com.example.alo.domain.repository.PresenceRepository
import com.example.alo.domain.repository.UserRepository
import com.example.alo.presentation.theme.AloTheme
import com.example.alo.presentation.view.navigation.AppNavigation
import com.example.alo.presentation.viewmodel.SplashViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val splashViewModel: SplashViewModel by viewModels()

    @Inject
    lateinit var presenceRepository: PresenceRepository

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {

            override fun onStart(owner: LifecycleOwner) {
                super.onStart(owner)
                Log.d("", "--> App vừa ON_START (vào Foreground)")

                lifecycleScope.launch {
                    authRepository.awaitInitialization()

                    val user = authRepository.getCurrentAuthUser()
                    val userId = user?.id
                    Log.d("MainActivity", "--> Current User ID lấy được: $userId")

                    if (userId != null) {
                        // Nếu userId khác null thì mới gọi hàm Presence
                        presenceRepository.subscribeAndTrack(userId)
                    } else {
                        Log.e("MainActivity", "!!! KHÔNG GỌI PRESENCE VÌ USER ID NULL !!!")
                    }
                }
            }

            override fun onStop(owner: LifecycleOwner) {
                Log.d("MainActivity", "--> App vừa ON_STOP (vào Background)")
                lifecycleScope.launch {
                    presenceRepository.unsubscribe()
                    userRepository.updateLastSeen()
                }
                super.onStop(owner)
            }
        })

        splashScreen.setKeepOnScreenCondition {
            splashViewModel.isLoading.value
        }

        setContent {
            AloTheme {
                val startDestination by splashViewModel.startDestination.collectAsState()

                if (startDestination != null) {
                    AppNavigation(startDestination = startDestination!!)
                }
            }
        }
    }
}