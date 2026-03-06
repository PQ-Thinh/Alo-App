package com.example.alo

import android.os.Bundle
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
                lifecycleScope.launch {
                    val userId = authRepository.getCurrentAuthUser()
                    if (userId != null) {
                        presenceRepository.subscribeAndTrack(userId.id)
                    }
                }
            }

            override fun onStop(owner: LifecycleOwner) {
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