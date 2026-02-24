package com.example.alo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alo.domain.model.UserState
import com.example.alo.domain.responsitories.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SupabaseAuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _userState = MutableStateFlow<UserState>(UserState.Loading)
    val userState: StateFlow<UserState> = _userState

    fun signUp(userEmail: String, userPassword: String) {
        viewModelScope.launch {
            try {
                authRepository.signUp(userEmail, userPassword)
                _userState.value = UserState.Success("Đăng ký thành công")

            } catch (e: Exception) {
                _userState.value = UserState.Error(e.message ?: "Lỗi không xác định")
            }
        }
    }
    fun login(userEmail: String, userPassword: String) {
        viewModelScope.launch {
            try {
                authRepository.login(userEmail, userPassword)
                _userState.value = UserState.Success("Đăng nhập thành công")
            } catch (e: Exception) {
                _userState.value = UserState.Error(e.message ?: "Lỗi không xác định")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                authRepository.logout()
                _userState.value = UserState.Success("Đăng xuất thành công")
            } catch (e: Exception) {
                _userState.value = UserState.Error(e.message ?: "Lỗi không xác định")
            }
        }
    }
}