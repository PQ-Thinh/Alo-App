package com.example.alo.presentation.helper

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.example.alo.BuildConfig

class GoogleAuthUiClient(
    private val context: Context
) {
    private val credentialManager = CredentialManager.Companion.create(context)

    suspend fun signIn(): String? {
        try {
            val webClientId = BuildConfig.webClientId

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context, request)
            val credential = result.credential

            return when (credential) {
                is CustomCredential -> {
                    if (credential.type == GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        val googleIdTokenCredential = GoogleIdTokenCredential.Companion.createFrom(credential.data)
                        googleIdTokenCredential.idToken
                    } else null
                }
                is GoogleIdTokenCredential -> credential.idToken
                else -> null
            }
        } catch (e: GetCredentialCancellationException) {
            throw kotlinx.coroutines.CancellationException("Đã hủy đăng nhập Google")
        } catch (e: Exception) {
            throw Exception(e.message)
        }
    }
}