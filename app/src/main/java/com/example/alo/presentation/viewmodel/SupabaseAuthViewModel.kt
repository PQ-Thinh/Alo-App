package com.example.alo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alo.data.utils.CryptoHelper
import com.example.alo.domain.model.User
import com.example.alo.presentation.helper.UserState
import com.example.alo.domain.repository.AuthRepository
import com.example.alo.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject


@HiltViewModel
class SupabaseAuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    private val _userState = MutableStateFlow<UserState>(UserState.Idle)
    val userState: StateFlow<UserState> = _userState

    fun signUp(userEmail: String, userPassword: String) {
        viewModelScope.launch {
            _userState.value = UserState.Loading
            try {
                authRepository.signUp(userEmail, userPassword)
                _userState.value = UserState.Success("Đăng ký thành công")
            } catch (e: Exception) {
                _userState.value = UserState.Error(e.message ?: "Lỗi không xác định")
            }
        }
    }

    fun login(userEmail: String, userPassword: String) {
        viewModelScope.launch {
            _userState.value = UserState.Loading
            try {
                authRepository.login(userEmail, userPassword)
                _userState.value = UserState.Success("Đăng nhập thành công")
            } catch (e: Exception) {
                _userState.value = UserState.Error(e.message ?: "Lỗi không xác định")
            }
        }
    }

    fun loginWithGoogle(credential: Any) {
        viewModelScope.launch {
            _userState.value = UserState.Loading
            try {
                authRepository.loginWithGoogle(credential as String)
                val sessionUser = supabaseClient.auth.currentSessionOrNull()?.user

                if (sessionUser != null) {
                    val existingProfile = userRepository.getCurrentUser(sessionUser.id)

                    if (existingProfile != null) {
                        _userState.value = UserState.Success("Đăng nhập Google thành công")
                    } else {
                        val email = sessionUser.email ?: ""
                        val metadata = sessionUser.userMetadata

                        val fullName = metadata?.get("full_name")?.toString()?.trim('"') ?: email.substringBefore("@")
                        val avatarUrl = metadata?.get("avatar_url")?.toString()?.trim('"')



                        val autoProfile = User(
                            id = sessionUser.id,
                            username = email.substringBefore("@"),
                            displayName = fullName,
                            email = email,
                            bio = null,
                            phone = null,
                            birthday = null,
                            gender = null,
                            avatarId = "google_oauth_avatar",
                            avatarUrl = avatarUrl,
                            publicKey = CryptoHelper.getOrGeneratePublicKey(),
                            createdAt = Instant.now().toString(),
                            updatedAt = Instant.now().toString()
                        )

                        val isSaved = userRepository.saveUserProfile(autoProfile)

                        if (isSaved) {
                            _userState.value = UserState.Success("Khởi tạo hồ sơ Google thành công")
                        } else {
                            _userState.value = UserState.Error("Lỗi: Không thể khởi tạo hồ sơ người dùng.")
                        }
                    }
                } else {
                    _userState.value = UserState.Error("Không lấy được phiên đăng nhập.")
                }
            } catch (e: Exception) {
                _userState.value = UserState.Error(e.message ?: "Đăng nhập Google thất bại")
            }
        }
    }
    fun logout() {
        viewModelScope.launch {
            _userState.value = UserState.Loading
            try {
                authRepository.logout()
                _userState.value = UserState.Success("Đăng xuất thành công")
            } catch (e: Exception) {
                _userState.value = UserState.Error(e.message ?: "Lỗi không xác định")
            }
        }
    }

}