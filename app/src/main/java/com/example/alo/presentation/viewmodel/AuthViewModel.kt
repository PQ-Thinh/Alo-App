package com.example.alo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alo.data.utils.CryptoHelper
import com.example.alo.domain.model.User
import com.example.alo.presentation.helper.UserState
import com.example.alo.domain.repository.AuthRepository
import com.example.alo.domain.repository.PushNotiRepository
import com.example.alo.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject


@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val notificationService: PushNotiRepository
) : ViewModel() {

    private val _userState = MutableStateFlow<UserState>(UserState.Idle)
    val userState: StateFlow<UserState> = _userState

    fun signUp(userEmail: String, userPassword: String) {
        viewModelScope.launch {
            _userState.value = UserState.Loading
            try {
                authRepository.signUp(userEmail, userPassword)
                _userState.value = UserState.NeedsOtpVerification(userEmail)
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
                val authUser = authRepository.getCurrentAuthUser()

                if (authUser != null) {
                    val existingProfile = userRepository.getCurrentUser(authUser.id)

                    if (existingProfile != null) {
                        _userState.value = UserState.Success("Đăng nhập Google thành công")
                    } else {
                        val defaultUsername = authUser.email.substringBefore("@")
                        val displayName = authUser.fullName ?: defaultUsername

                        val autoProfile = User(
                            id = authUser.id,
                            username = defaultUsername,
                            displayName = displayName,
                            email = authUser.email,
                            bio = null,
                            phone = null,
                            birthday = null,
                            gender = null,
                            avatarId = "google_oauth_avatar",
                            avatarUrl = authUser.avatarUrl,
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
    fun verifyOtp(email: String, otp: String) {
        viewModelScope.launch {
            _userState.value = UserState.Loading
            try {
                authRepository.verifyOtp(email, otp)
                _userState.value = UserState.VerificationSuccess
            } catch (e: Exception) {
                _userState.value = UserState.Error("Mã xác nhận không hợp lệ hoặc đã hết hạn")
            }
        }
    }
    fun resendOtp(email: String) {
        viewModelScope.launch {
            try {
              authRepository.resendOtp(email)
            } catch (e: Exception) {
                _userState.value = UserState.Error("Không thể gửi lại mã. Vui lòng thử lại sau!")
            }
        }
    }
    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            _userState.value = UserState.Loading
            try {
                authRepository.sendPasswordResetEmail(email)
                _userState.value = UserState.PasswordResetOtpSent(email)
            } catch (e: Exception) {
                _userState.value = UserState.Error("Không tìm thấy email hoặc có lỗi xảy ra.")
            }
        }
    }

    fun verifyPasswordResetOtp(email: String, otp: String) {
        viewModelScope.launch {
            _userState.value = UserState.Loading
            try {
                authRepository.verifyPasswordResetOtp(email, otp)
                _userState.value = UserState.PasswordResetOtpVerified
            } catch (e: Exception) {
                _userState.value = UserState.Error("Mã xác nhận không đúng hoặc đã hết hạn.")
            }
        }
    }

    fun updateNewPassword(newPassword: String) {
        viewModelScope.launch {
            _userState.value = UserState.Loading
            try {
                authRepository.updateNewPassword(newPassword)
                _userState.value = UserState.PasswordChangedSuccess
            } catch (e: Exception) {
                _userState.value = UserState.Error("Lỗi cập nhật mật khẩu: ${e.message}")
            }
        }
    }
    fun saveDeviceToken() {
        viewModelScope.launch {
            val token = notificationService.getDeviceToken()
            if (token != null) {
                val deviceName = android.os.Build.MODEL
                userDeviceRepository.saveFcmToken(token, deviceName)
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