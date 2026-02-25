package com.example.alo.presentation.helper

import com.example.alo.domain.model.User

sealed class UserProfileState {
    object Idle : UserProfileState()
    object Loading : UserProfileState()
    data class Success(val user: User) : UserProfileState()
    data class Error(val message: String) : UserProfileState()
}