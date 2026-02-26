package com.example.alo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alo.domain.repositories.UserRepository
import com.example.alo.presentation.view.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination = _startDestination.asStateFlow()

    init {
        checkAuthStatus()
    }

    private fun checkAuthStatus() {
        viewModelScope.launch {
            val session = supabaseClient.auth.currentSessionOrNull()

            if (session != null) {
                val userId = session.user?.id ?: ""
                val userProfile = userRepository.getCurrentUser(userId)

                if (userProfile != null) {
                    _startDestination.value = Screen.Dashboard.route
                } else {
                    val email = session.user?.email ?: ""
                    _startDestination.value = Screen.ProfileSetup.createRoute(userId, email)
                }
            } else {
                _startDestination.value = Screen.Login.route
            }
        }
    }
}