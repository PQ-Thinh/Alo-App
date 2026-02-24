package com.example.alo.data.repository

import com.example.alo.data.utils.SharedPreferenceHelper
import com.example.alo.domain.responsitories.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val sharedPref: SharedPreferenceHelper
) : AuthRepository {

    override suspend fun signUp(email: String, password: String) {
        supabaseClient.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }

        val accessToken = supabaseClient.auth.currentAccessTokenOrNull() ?: ""
        sharedPref.saveStringData("accessToken", accessToken)
        //sharedPref.getStringData("accessToken")

    }

    override suspend fun login(email: String, password: String) {
        supabaseClient.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        sharedPref.getStringData("accessToken")
    }

    override suspend fun logout() {


    }

}