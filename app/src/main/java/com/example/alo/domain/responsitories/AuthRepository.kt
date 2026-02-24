package com.example.alo.domain.responsitories

interface AuthRepository {
    suspend fun signUp(email: String, password: String)
    suspend fun login(email: String, password: String)
    suspend fun logout()

}