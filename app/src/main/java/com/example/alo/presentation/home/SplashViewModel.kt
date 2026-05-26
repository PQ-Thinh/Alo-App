package com.example.alo.presentation.home

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alo.core.crypto.GroupKeyRewrapHelper
import com.example.alo.domain.repository.AuthRepository
import com.example.alo.domain.repository.ParticipantRepository
import com.example.alo.domain.repository.PushNotiRepository
import com.example.alo.domain.repository.UserDeviceRepository
import com.example.alo.domain.repository.UserRepository
import com.example.alo.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val pushNotiRepository: PushNotiRepository,
    private val userDeviceRepository: UserDeviceRepository,
    private val participantRepository: ParticipantRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination

    init {
        checkAuthStatus()
    }

    private fun checkAuthStatus() {
        viewModelScope.launch {
            try {
                authRepository.awaitInitialization()

                delay(1000)

                val session = authRepository.getCurrentAuthUser()

                if (session != null) {
                _startDestination.value = Screen.Dashboard.route
                saveFCMToken()
                // Background scan: giúp re-wrap Group Key cho thành viên cần khôi phục
                viewModelScope.launch {
                    try {
                        GroupKeyRewrapHelper.scanAndProcessPendingRewraps(
                            context, session.id, participantRepository, userRepository
                        )
                    } catch (e: Exception) {
                        Log.e("SplashVM", "Lỗi background scan re-wrap: ${e.message}")
                    }
                }
                } else {
                    _startDestination.value = Screen.Intro.route
                }
            } catch (e: Exception) {
                _startDestination.value = Screen.Intro.route
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveFCMToken() {
        viewModelScope.launch {
            try {
                val session = authRepository.getCurrentAuthUser()
                if (session == null) {
                    return@launch
                }
                val token = pushNotiRepository.getDeviceToken()
                if (token != null) {
                    val deviceName = Build.MODEL
                    userDeviceRepository.saveFcmToken(token, deviceName)
                } else {
                    Log.e("FCM_DEBUG", " Firebase trả về Token bị rỗng (null)")
                }
            } catch (e: Exception) {
            }
        }
    }
}
