package com.example.alo.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alo.data.utils.CryptoHelper
import com.example.alo.domain.model.User
import com.example.alo.domain.repository.AuthRepository
import com.example.alo.domain.repository.UserRepository
import com.example.alo.presentation.helper.ProfileSetupEvent
import com.example.alo.presentation.helper.ProfileSetupState
import com.example.alo.presentation.helper.UserProfileState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Locale
import javax.inject.Inject



@HiltViewModel
class UserViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
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
                val authUser = authRepository.getCurrentAuthUser()
                if (authUser == null) {
                    _state.update { it.copy(isLoading = false, error = "Lỗi bảo mật: Bạn chưa đăng nhập!") }
                    return@launch
                }
                val currentState = _state.value

                val existingProfile = userRepository.getCurrentUser(authUser.id)

                var finalAvatarUrl: String? = existingProfile?.avatarUrl ?: authUser.avatarUrl
                var finalAvatarId: String? = existingProfile?.avatarId
                    ?: if (authUser.avatarUrl != null) "google_oauth_avatar" else "default_id"
                val (encryptKeyStr, signKeyStr) = CryptoHelper.generateAndGetPublicKeys(context)
                if (currentState.avatarBytes != null) {
                    val uploadedUrl = userRepository.uploadAvatar(currentState.avatarBytes, "jpg")
                    if (uploadedUrl.isNotBlank()) {
                        finalAvatarUrl = uploadedUrl
                        finalAvatarId = uploadedUrl.substringAfterLast("/")
                    }
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
                if (currentState.username.isBlank() || currentState.displayName.isBlank()) {
                    _state.update { it.copy(error = "Vui lòng nhập đầy đủ tên và username") }
                    return@launch
                }
                val user = User(
                    id = authUser.id,
                    email = authUser.email,
                    username = currentState.username,
                    displayName = currentState.displayName,
                    phone = currentState.phone.ifBlank { null },
                    birthday = formattedBirthday,
                    gender = currentState.gender,
                    bio = currentState.bio.ifBlank{ null},
                    avatarId = finalAvatarId.toString(),
                    avatarUrl = finalAvatarUrl,
                    publicEncryptKey = encryptKeyStr,
                    publicSignKey = signKeyStr,
                    createdAt = existingProfile?.createdAt ?: Instant.now().toString(),
                    updatedAt = Instant.now().toString()
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
                val authUser = authRepository.getCurrentAuthUser()

                if (authUser == null) {
                    _profileState.value = UserProfileState.Error("Bạn chưa đăng nhập!")
                    return@launch
                }

                val user = userRepository.getCurrentUser(authUser.id)

                if (user != null) {
                    val isGoogleAccount = user.avatarId == "google_oauth_avatar"
                    val latestGoogleAvatar = authUser.avatarUrl

                    if (isGoogleAccount && latestGoogleAvatar != null && latestGoogleAvatar != user.avatarUrl) {
                        val updateData = mapOf("avatar_url" to latestGoogleAvatar)
                        val success = userRepository.updateProfile(user.id, updateData)

                        if (success) {
                            _profileState.value = UserProfileState.Success(user.copy(avatarUrl = latestGoogleAvatar))
                            return@launch
                        }
                    }

                    _profileState.value = UserProfileState.Success(user)
                } else {
                    _profileState.value = UserProfileState.Error("Không tìm thấy thông tin người dùng")
                }
            } catch (e: Exception) {
                _profileState.value = UserProfileState.Error(e.message ?: "Đã xảy ra lỗi không xác định")
            }
        }
    }
    fun updateUserProfile(
        displayName: String,
        bio: String,
        phone: String,
        birthday: String,
        gender: Boolean?,
        newUsername: String,
        newAvatarBytes: ByteArray?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val currentState = _profileState.value
            if (currentState !is UserProfileState.Success) return@launch

            val currentUser = currentState.user
            _profileState.value = UserProfileState.Loading

            try {
                val formattedBirthday = if (birthday.isNotBlank()) {
                    try {
                        val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val date = inputFormat.parse(birthday)
                        outputFormat.format(date!!)
                    } catch (e: Exception) {
                        birthday 
                    }
                } else ""

                val updateData = mutableMapOf<String, String>(
                    "display_name" to displayName,
                    "bio" to bio,
                    "phone" to phone,
                    "birthday" to formattedBirthday
                )

                // Lưu trạng thái giới tính vào Map
                if (gender != null) {
                    updateData["gender"] = gender.toString()
                }

                val isGoogleAccount = currentUser.avatarId == "google_oauth_avatar"

                if (!isGoogleAccount) {
                    updateData["username"] = newUsername

                    if (newAvatarBytes != null) {
                        val uploadedUrl = userRepository.uploadAvatar(newAvatarBytes, "jpg")
                        if (uploadedUrl.isNotBlank()) {
                            updateData["avatar_url"] = uploadedUrl
                            updateData["avatarid"] = uploadedUrl.substringAfterLast("/")
                        }
                    }
                }

                val success = userRepository.updateProfile(currentUser.id, updateData)

                if (success) {
                    fetchCurrentUserProfile()
                    withContext(Dispatchers.Main) { onSuccess() }
                } else {
                    fetchCurrentUserProfile()
                    withContext(Dispatchers.Main) { onError("Cập nhật thất bại") }
                }
            } catch (e: Exception) {
                fetchCurrentUserProfile()
                withContext(Dispatchers.Main) { onError(e.message ?: "Lỗi không xác định") }
            }
        }
    }
}