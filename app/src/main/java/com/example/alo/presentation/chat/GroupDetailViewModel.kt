package com.example.alo.presentation.chat

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alo.domain.model.SharedTask
import com.example.alo.domain.model.User
import com.example.alo.domain.repository.AuthRepository
import com.example.alo.domain.repository.ConversationRepository
import com.example.alo.domain.repository.ParticipantRepository
import com.example.alo.domain.repository.SharedTaskRepository
import com.example.alo.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroupDetailState(
    val conversationId: String = "",
    val groupName: String = "",
    val groupAvatar: String? = null,
    val members: List<UserWithRole> = emptyList(),
    val isAdmin: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val isLeaving: Boolean = false,
    val currentUserId: String = "",
    val tasks: List<SharedTask> = emptyList(),
    val groupStatus: String? = null
)

data class UserWithRole(
    val user: User,
    val role: String
)

@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val conversationRepository: ConversationRepository,
    private val participantRepository: ParticipantRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val sharedTaskRepository: SharedTaskRepository
) : ViewModel() {

    private val conversationId: String = checkNotNull(savedStateHandle["conversationId"])

    private val _state = MutableStateFlow(GroupDetailState(conversationId = conversationId))
    val state: StateFlow<GroupDetailState> = _state.asStateFlow()

    init {
        loadGroupDetails()
    }

    private fun loadGroupDetails() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val currentUser = authRepository.getCurrentAuthUser() ?: return@launch
                _state.update { it.copy(currentUserId = currentUser.id) }

                // 1. Get Conversation Info (using getChatList for simplicity or direct query)
                val chatList = conversationRepository.getChatList(currentUser.id)
                val groupInfo = chatList.find { it.conversationId == conversationId }

                if (groupInfo != null) {
                    _state.update { it.copy(
                        groupName = groupInfo.chatName ?: "Nhóm",
                        groupAvatar = groupInfo.chatAvatar,
                        groupStatus = groupInfo.status ?: ""
                    ) }
                }

                // 2. Get Participants
                val participants = participantRepository.getParticipants(conversationId)
                Log.d("GroupDetailVM", "Fetched ${participants.size} participants for group $conversationId")
                
                val userIds = participants.map { it.userId }
                val userProfiles = userRepository.getUsersByIds(userIds)
                Log.d("GroupDetailVM", "Fetched ${userProfiles.size} user profiles from DB")
                
                val membersWithRoles = participants.mapNotNull { p ->
                    val profile = userProfiles.find { it.id == p.userId }
                    if (profile != null) {
                        UserWithRole(profile, p.role)
                    } else null
                }
                
                Log.d("GroupDetailVM", "Total members joined with roles: ${membersWithRoles.size}")

                val myParticipant = participants.find { it.userId == currentUser.id }
                _state.update { it.copy(
                    members = membersWithRoles,
                    isAdmin = myParticipant?.role == "admin",
                    isLoading = false
                ) }

                loadTasks()
                observeTaskUpdates()

            } catch (e: Exception) {
                Log.e("GroupDetailVM", "Lỗi tải thông tin nhóm: ${e.message}")
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun loadTasks() {
        viewModelScope.launch {
            val tasks = sharedTaskRepository.getTasks(conversationId)
            _state.update { it.copy(tasks = tasks) }
        }
    }

    private fun observeTaskUpdates() {
        viewModelScope.launch {
            sharedTaskRepository.subscribeToTaskUpdates(conversationId).collect {
                loadTasks()
            }
        }
    }

    fun createTask(title: String, description: String? = null) {
        viewModelScope.launch {
            val newTask = SharedTask(
                conversationId = conversationId,
                title = title,
                description = description,
                creatorId = _state.value.currentUserId
            )
            sharedTaskRepository.createTask(newTask)
        }
    }

    fun toggleTaskCompletion(task: SharedTask) {
        viewModelScope.launch {
            sharedTaskRepository.updateTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            sharedTaskRepository.deleteTask(taskId)
        }
    }

    fun updateGroupStatus(status: String) {
        viewModelScope.launch {
            try {
                conversationRepository.updateGroupMetadata(conversationId, null, null, status) 
                _state.update { it.copy(groupStatus = status, successMessage = "Cập nhật trạng thái nhóm thành công") }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateGroupName(newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            try {
                conversationRepository.updateGroupMetadata(conversationId, newName, null, null)
                _state.update { it.copy(groupName = newName, successMessage = "Cập nhật tên nhóm thành công") }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Lỗi cập nhật tên nhóm: ${e.message}") }
            }
        }
    }

    fun updateGroupAvatar(bytes: ByteArray) {
        viewModelScope.launch {
            try {
                val avatarUrl = userRepository.uploadAvatar(bytes, "jpg")
                if (avatarUrl.isNotEmpty()) {
                    conversationRepository.updateGroupMetadata(conversationId, null, avatarUrl, null)
                    _state.update { it.copy(groupAvatar = avatarUrl, successMessage = "Cập nhật ảnh đại diện thành công") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Lỗi cập nhật ảnh đại diện: ${e.message}") }
            }
        }
    }

    fun removeMember(userId: String) {
        viewModelScope.launch {
            try {
                participantRepository.removeParticipant(conversationId, userId)
                _state.update { currentState ->
                    currentState.copy(
                        members = currentState.members.filter { it.user.id != userId },
                        successMessage = "Đã xóa thành viên"
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Lỗi xóa thành viên: ${e.message}") }
            }
        }
    }

    fun updateMemberRole(userId: String, newRole: String) {
        viewModelScope.launch {
            try {
                participantRepository.updateParticipantRole(conversationId, userId, newRole)
                _state.update { currentState ->
                    val updatedMembers = currentState.members.map {
                        if (it.user.id == userId) it.copy(role = newRole)
                        else it
                    }
                    currentState.copy(members = updatedMembers, successMessage = "Cập nhật quyền thành công")
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Lỗi cập nhật quyền: ${e.message}") }
            }
        }
    }

    fun leaveGroup() {
        viewModelScope.launch {
            _state.update { it.copy(isLeaving = true) }
            try {
                val currentId = _state.value.currentUserId
                participantRepository.removeParticipant(conversationId, currentId)
                // Navigation will be handled in UI via state observation if needed, 
                // or just pop backstack.
                _state.update { it.copy(isLeaving = false, successMessage = "Đã rời nhóm") }
            } catch (e: Exception) {
                _state.update { it.copy(isLeaving = false, error = e.message) }
            }
        }
    }

    fun clearMessages() {
        _state.update { it.copy(error = null, successMessage = null) }
    }
}
