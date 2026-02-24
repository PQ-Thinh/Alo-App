package com.example.alo.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alo.data.network.SupabaseClient.client
import com.example.alo.domain.model.UserState
import com.example.alo.domain.usecase.SharedPreferenceHelper
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import io.github.jan.supabase.auth.providers.builtin.Email

class SupabaseAuthViewModel: ViewModel() {
    private val _userState = MutableStateFlow<UserState>(UserState.Loading)
    val userState: StateFlow<UserState> = _userState

    fun signUp(
        context: Context,
        userEmail: String,
        userPassword: String,
    ){
        viewModelScope.launch {
            try {
                client.auth.signUpWith(Email) {
                    email = userEmail
                    password = userPassword
                }
                saveToken(context)
            }catch (e: Exception){
                _userState.value = UserState.Error(e.message.toString())
            }
        }
    }
    private fun saveToken(context: Context){
        viewModelScope.launch {
            val accessToken = client.auth.currentAccessTokenOrNull() ?:""
            val sharedPref = SharedPreferenceHelper(context)
            sharedPref.saveStringData("accessToken",accessToken)

        }
    }
}