package com.example.alo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alo.presentation.view.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth // Import module auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination = _startDestination.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                supabaseClient.auth.awaitInitialization()

                val session = supabaseClient.auth.currentSessionOrNull()
                if (session != null) {
                    _startDestination.value = Screen.Dashboard.route
                } else {
                    _startDestination.value = Screen.Intro.route
                }
            } catch (e: Exception) {
                _startDestination.value = Screen.Login.route
            } finally {
                _isLoading.value = false
            }
        }
    }
}