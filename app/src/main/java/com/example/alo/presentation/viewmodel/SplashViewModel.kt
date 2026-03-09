package com.example.alo.presentation.viewmodel

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alo.domain.repository.AuthRepository
import com.example.alo.domain.repository.PushNotiRepository
import com.example.alo.domain.repository.UserDeviceRepository
import com.example.alo.presentation.view.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val pushNotiRepository: PushNotiRepository,
    private val userDeviceRepository: UserDeviceRepository
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
                val token = pushNotiRepository.getDeviceToken()
                if (token != null) {
                    val deviceName = Build.MODEL
                    userDeviceRepository.saveFcmToken(token, deviceName)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}