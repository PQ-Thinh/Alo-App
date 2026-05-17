package com.example.alo.presentation.chat

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alo.domain.model.SharedTask
import com.example.alo.domain.model.User
import com.example.alo.domain.repository.AuthRepository
import com.example.alo.domain.repository.ParticipantRepository
import com.example.alo.domain.repository.SharedTaskRepository
import com.example.alo.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class CreateTaskState(
    val conversationId: String = "",
    val title: String = "",
    val description: String = "",
    val assignee: User? = null,
    val dueDate: Calendar? = null,
    val priority: String = "medium",
    val members: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

@HiltViewModel
class CreateTaskViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sharedTaskRepository: SharedTaskRepository,
    private val participantRepository: ParticipantRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val conversationId: String = checkNotNull(savedStateHandle["conversationId"])
    private val _state = MutableStateFlow(CreateTaskState(conversationId = conversationId))
    val state: StateFlow<CreateTaskState> = _state.asStateFlow()

    init {
        loadMembers()
    }

    private fun loadMembers() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val participants = participantRepository.getParticipants(conversationId)
                val userIds = participants.map { it.userId }
                val profiles = userRepository.getUsersByIds(userIds)
                _state.update { it.copy(members = profiles, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun onTitleChanged(title: String) {
        _state.update { it.copy(title = title) }
    }

    fun onDescriptionChanged(desc: String) {
        _state.update { it.copy(description = desc) }
    }

    fun onAssigneeSelected(user: User?) {
        _state.update { it.copy(assignee = user) }
    }

    fun onDueDateSelected(date: Calendar?) {
        _state.update { it.copy(dueDate = date) }
    }

    fun onPriorityChanged(priority: String) {
        _state.update { it.copy(priority = priority) }
    }

    fun saveTask() {
        if (_state.value.title.isBlank()) {
            _state.update { it.copy(error = "Vui lòng nhập tiêu đề") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            try {
                val currentUser = authRepository.getCurrentAuthUser()
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                
                val newTask = SharedTask(
                    conversationId = conversationId,
                    title = _state.value.title,
                    description = _state.value.description.takeIf { it.isNotBlank() },
                    assigneeId = _state.value.assignee?.id,
                    dueDate = _state.value.dueDate?.let { sdf.format(it.time) },
                    creatorId = currentUser?.id,
                    priority = _state.value.priority
                )
                
                sharedTaskRepository.createTask(newTask)
                _state.update { it.copy(isSaving = false, success = true) }
            } catch (e: Exception) {
                Log.e("CreateTaskVM", "Lỗi tạo task: ${e.message}")
                _state.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }
}
