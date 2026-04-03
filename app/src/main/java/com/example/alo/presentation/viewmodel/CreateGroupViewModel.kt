package com.example.alo.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alo.data.utils.CryptoHelper
import com.example.alo.domain.model.User
import com.example.alo.domain.repository.AuthRepository
import com.example.alo.domain.repository.ConversationRepository
import com.example.alo.domain.repository.FriendRepository
import com.example.alo.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateGroupState(
    val friends: List<User> = emptyList(),
    val selectedUserIds: Set<String> = emptySet(),
    val groupName: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val navigateToConversationId: String? = null,
    val selectedAvatarBytes: ByteArray? = null
)

@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    private val friendRepository: FriendRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CreateGroupState())
    val state: StateFlow<CreateGroupState> = _state.asStateFlow()

    init {
        loadFriends()
    }

    private fun loadFriends() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val currentUser = authRepository.getCurrentAuthUser()
            if (currentUser != null) {
                val friends = friendRepository.getFriendsList(currentUser.id)
                _state.update { it.copy(friends = friends, isLoading = false) }
            } else {
                _state.update { it.copy(isLoading = false, error = "Chưa đăng nhập") }
            }
        }
    }

    fun onGroupNameChanged(name: String) {
        _state.update { it.copy(groupName = name) }
    }

    fun toggleUserSelection(userId: String) {
        _state.update { currentState ->
            val newSelection = if (currentState.selectedUserIds.contains(userId)) {
                currentState.selectedUserIds - userId
            } else {
                currentState.selectedUserIds + userId
            }
            currentState.copy(selectedUserIds = newSelection)
        }
    }

    fun onAvatarSelected(bytes: ByteArray?) {
        _state.update { it.copy(selectedAvatarBytes = bytes) }
    }

    fun createGroup() {
        val groupName = _state.value.groupName.trim()
        val selectedIds = _state.value.selectedUserIds.toList()

        if (groupName.isEmpty()) {
            _state.update { it.copy(error = "Vui lòng nhập tên nhóm") }
            return
        }
        if (selectedIds.isEmpty()) {
            _state.update { it.copy(error = "Vui lòng chọn ít nhất 1 thành viên") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val currentUser = authRepository.getCurrentAuthUser() ?: return@launch
                val myProfile = userRepository.getCurrentUser(currentUser.id) ?: return@launch
                
                // 0. Upload Avatar if selected
                var avatarUrl: String? = null
                _state.value.selectedAvatarBytes?.let { bytes ->
                    val uploadedUrl = userRepository.uploadAvatar(bytes, "jpg")
                    if (uploadedUrl.isNotEmpty()) {
                        avatarUrl = uploadedUrl
                    }
                }
                
                // 1. Tạo Group KeysetHandle (AES-256)
                val groupKeysetHandle = CryptoHelper.generateGroupKeysetHandle()
                val groupKeysetBase64 = CryptoHelper.exportKeysetToBase64(groupKeysetHandle)
                
                // 2. Chuẩn bị bản đồ khóa đã mã hóa cho từng thành viên
                val encryptedKeysMap = mutableMapOf<String, String>()
                
                // Mã hóa cho chính mình (admin)
                if (myProfile.publicEncryptKey.isNotEmpty()) {
                    encryptedKeysMap[currentUser.id] = CryptoHelper.wrapGroupKey(
                        groupKeysetBase64, 
                        myProfile.publicEncryptKey
                    )
                    Log.d("CreateGroupVM", "Đã mã hóa key cho ADMIN: ${currentUser.id}")
                }

                // Mã hóa cho từng thành viên được chọn (Sử dụng dữ liệu cached trong state.friends)
                for (userId in selectedIds) {
                    val friend = _state.value.friends.find { it.id == userId }
                    if (friend != null && friend.publicEncryptKey.isNotEmpty()) {
                        encryptedKeysMap[userId] = CryptoHelper.wrapGroupKey(
                            groupKeysetBase64,
                            friend.publicEncryptKey
                        )
                        Log.d("CreateGroupVM", "Đã mã hóa key cho MEMBER: $userId (${friend.displayName})")
                    } else {
                        Log.e("CreateGroupVM", "KHÔNG THỂ mã hóa cho user $userId: Thiếu publicEncryptKey")
                    }
                }
                
                // 3. Gọi RPC tạo nhóm
                val chatList = conversationRepository.createGroupConversation(
                    name = groupName,
                    avatarUrl = avatarUrl, 
                    userIds = selectedIds,
                    encryptedKeys = encryptedKeysMap
                )
                
                if (chatList != null) {
                    _state.update { it.copy(isLoading = false, navigateToConversationId = chatList.conversationId) }
                } else {
                    _state.update { it.copy(isLoading = false, error = "Không thể tạo nhóm") }
                }
                
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
    
    fun resetNavigation() {
        _state.update { it.copy(navigateToConversationId = null) }
    }
}