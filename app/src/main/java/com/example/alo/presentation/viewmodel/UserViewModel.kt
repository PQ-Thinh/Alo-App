package com.example.alo.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alo.data.remote.dto.UserDto
import com.example.alo.data.utils.CryptoHelper
import com.example.alo.domain.model.User
import com.example.alo.domain.repositories.UserRepository
import com.example.alo.presentation.helper.ProfileSetupEvent
import com.example.alo.presentation.helper.ProfileSetupState
import com.example.alo.presentation.helper.UserProfileState
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Locale
import javax.inject.Inject



@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileSetupState())
    val state: StateFlow<ProfileSetupState> = _state.asStateFlow()

    private val _profileState = MutableStateFlow<UserProfileState>(UserProfileState.Idle)
    val profileState: StateFlow<UserProfileState> = _profileState.asStateFlow()

    fun onEvent(event: ProfileSetupEvent) {
        when (event) {
            is ProfileSetupEvent.EnteredUsername -> _state.update { it.copy(username = event.value) }
            is ProfileSetupEvent.EnteredDisplayName -> _state.update { it.copy(displayName = event.value) }
            is ProfileSetupEvent.EnteredPhone -> _state.update { it.copy(phone = event.value) }
            is ProfileSetupEvent.EnteredBirthday -> _state.update { it.copy(birthday = event.value) }
            is ProfileSetupEvent.SelectedGender -> _state.update { it.copy(gender = event.value) }
            is ProfileSetupEvent.EnteredBio -> _state.update { it.copy(bio = event.value) }
            is ProfileSetupEvent.SelectedAvatar -> _state.update { it.copy(avatarBytes = event.bytes) }
            is ProfileSetupEvent.Submit -> submitProfile()
        }
    }

    private fun submitProfile() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val sessionUser = supabaseClient.auth.currentSessionOrNull()?.user
                if (sessionUser == null) {
                    _state.update { it.copy(isLoading = false, error = "Lỗi bảo mật: Bạn chưa đăng nhập!") }
                    return@launch
                }
                val currentState = _state.value

                var uploadedUrl = ""
                var avatarId = "default_id"

                if (currentState.avatarBytes != null) {
                    uploadedUrl = userRepository.uploadAvatar(currentState.avatarBytes, "jpg")
                    avatarId = uploadedUrl.substringAfterLast("/")
                }
                val formattedBirthday = if (currentState.birthday.isNotBlank()) {
                    try {
                        val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val date = inputFormat.parse(currentState.birthday)
                        outputFormat.format(date!!)
                    } catch (e: Exception) {
                        currentState.birthday
                    }
                } else {
                    null
                }

                val user = User(
                    id = sessionUser.id,
                    email = sessionUser.email ?: "",
                    username = currentState.username,
                    displayName = currentState.displayName,
                    phone = currentState.phone.ifBlank { null },
                    birthday = formattedBirthday,
                    gender = currentState.gender,
                    bio = currentState.bio.ifBlank{ null},
                    avatarId = avatarId,
                    avatarUrl = uploadedUrl.ifEmpty { null },
                    publicKey = CryptoHelper.getOrGeneratePublicKey(),
                    createdAt = Instant.now().toString(),
                    updatedAt = ""
                )
                val result = userRepository.saveUserProfile(user)
                if (result) {
                    _state.update { it.copy(isLoading = false, isSuccess = true) }
                } else {
                    _state.update { it.copy(isLoading = false, error = "Lỗi lưu thông tin người dùng") }

                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
    fun fetchCurrentUserProfile() {
        viewModelScope.launch {
            _profileState.value = UserProfileState.Loading
            try {
                val currentUserId = supabaseClient.auth.currentSessionOrNull()?.user?.id
                if (currentUserId == null) {
                    _profileState.value = UserProfileState.Error("Bạn chưa đăng nhập!")
                    return@launch
                }

                val user = userRepository.getCurrentUser(currentUserId)
                if (user != null) {
                    _profileState.value = UserProfileState.Success(user)
                } else {
                    _profileState.value = UserProfileState.Error("Không tìm thấy thông tin người dùng")
                }
            } catch (e: Exception) {
                _profileState.value = UserProfileState.Error(e.message ?: "Đã xảy ra lỗi không xác định")
            }
        }
    }
}