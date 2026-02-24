package com.example.alo.domain.repositories

interface AuthRepository {
    suspend fun signUp(email: String, password: String)
    suspend fun login(email: String, password: String)
    suspend fun logout()
    suspend fun loginWithGoogle(idToken: String)
}