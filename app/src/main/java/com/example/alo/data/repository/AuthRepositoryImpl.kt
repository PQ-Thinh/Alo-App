package com.example.alo.data.repository

import com.example.alo.data.utils.DataStoreHelper
import com.example.alo.domain.model.AuthUser
import com.example.alo.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.OtpType
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

    override suspend fun verifyOtp(email: String, otp: String) {
        supabaseClient.auth.verifyEmailOtp(
            type = OtpType.Email.SIGNUP,
            email = email,
            token = otp
        )
    }

    override suspend fun resendOtp(email: String) {
        supabaseClient.auth.resendEmail(
            type = OtpType.Email.SIGNUP,
            email = email,
            captchaToken = null
        )
    }

    override suspend fun sendPasswordResetEmail(email: String) {
        supabaseClient.auth.resetPasswordForEmail(email)
    }

    override suspend fun verifyPasswordResetOtp(email: String, otp: String) {
        supabaseClient.auth.verifyEmailOtp(
            type = OtpType.Email.RECOVERY,
            email = email,
            token = otp
        )
    }

    override suspend fun updateNewPassword(newPassword: String) {
        supabaseClient.auth.updateUser {
            password = newPassword
        }
    }

    override suspend fun getCurrentAuthUser(): AuthUser? {
        val sessionUser = supabaseClient.auth.currentSessionOrNull()?.user ?: return null

        val email = sessionUser.email ?: ""
        val metadata = sessionUser.userMetadata

        val fullName = metadata?.get("full_name")?.toString()?.trim('"')
        val avatarUrl = metadata?.get("avatar_url")?.toString()?.trim('"')

        return AuthUser(
            id = sessionUser.id,
            email = email,
            fullName = fullName,
            avatarUrl = avatarUrl
        )
    }

}