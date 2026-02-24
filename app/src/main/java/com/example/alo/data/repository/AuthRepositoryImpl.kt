package com.example.alo.data.repository

import com.example.alo.data.utils.DataStoreHelper
import com.example.alo.domain.repositories.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val dataStoreHelper: DataStoreHelper
) : AuthRepository {

    override suspend fun signUp(email: String, password: String) {
        supabaseClient.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        val accessToken = supabaseClient.auth.currentAccessTokenOrNull() ?: ""
        dataStoreHelper.saveToken(accessToken)
    }

    override suspend fun login(email: String, password: String) {
        supabaseClient.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        val accessToken = supabaseClient.auth.currentAccessTokenOrNull() ?: ""
        dataStoreHelper.saveToken(accessToken)
    }

    override suspend fun logout() {
        supabaseClient.auth.signOut()
        dataStoreHelper.clearAll()
    }
    override suspend fun loginWithGoogle(idToken: String) {
        supabaseClient.auth.signInWith(IDToken) {
            this.idToken = idToken
            this.provider = Google
        }

        val accessToken = supabaseClient.auth.currentAccessTokenOrNull() ?: ""
        dataStoreHelper.saveToken(accessToken)
    }
}