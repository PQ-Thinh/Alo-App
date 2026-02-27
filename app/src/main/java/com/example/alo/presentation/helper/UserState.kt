package com.example.alo.presentation.helper

sealed class UserState {
    object Idle : UserState()
    object Loading : UserState()
    data class Success(val message: String) : UserState()
    data class NeedsOtpVerification(val email: String) : UserState()
    object VerificationSuccess : UserState()
    data class PasswordResetOtpSent(val email: String) : UserState()
    object PasswordResetOtpVerified : UserState()
    object PasswordChangedSuccess : UserState()
    data class Error(val message: String) : UserState()
}