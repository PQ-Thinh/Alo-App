package com.example.alo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alo.domain.model.User
import com.example.alo.domain.repositories.UserRepository
import com.example.alo.presentation.helper.UserProfileState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject



@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _userState = MutableStateFlow<UserProfileState>(UserProfileState.Idle)
    val userState: StateFlow<UserProfileState> = _userState

    // Lấy thông tin user
    fun fetchUserProfile(userId: String) {
        viewModelScope.launch {
            _userState.value = UserProfileState.Loading
            val user = userRepository.getCurrentUser(userId)
            if (user != null) {
                _userState.value = UserProfileState.Success(user)
            } else {
                _userState.value = UserProfileState.Error("Không tìm thấy thông tin người dùng")
            }
        }
    }

    // Lưu thông tin user
    fun saveUserProfile(user: User, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _userState.value = UserProfileState.Loading
            val isSuccess = userRepository.saveUserProfile(user)
            if (isSuccess) {
                _userState.value = UserProfileState.Success(user)
                onSuccess()
            } else {
                _userState.value = UserProfileState.Error("Lỗi khi lưu thông tin")
            }
        }
    }
}