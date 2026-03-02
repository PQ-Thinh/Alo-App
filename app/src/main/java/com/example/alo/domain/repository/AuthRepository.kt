package com.example.alo.domain.repository

import com.example.alo.domain.model.AuthUser

interface AuthRepository {
    suspend fun signUp(email: String, password: String)
    suspend fun login(email: String, password: String)
    suspend fun logout()
    suspend fun loginWithGoogle(idToken: String)
    suspend fun verifyOtp(email: String, otp: String)
    suspend fun resendOtp(email: String)
    suspend fun sendPasswordResetEmail(email: String)
    suspend fun verifyPasswordResetOtp(email: String, otp: String)
    suspend fun updateNewPassword(newPassword: String)
    suspend fun getCurrentAuthUser(): AuthUser?
}