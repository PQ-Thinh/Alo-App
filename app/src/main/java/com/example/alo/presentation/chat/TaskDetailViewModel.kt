package com.example.alo.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alo.domain.model.Participant
import com.example.alo.domain.model.SharedTask
import com.example.alo.domain.repository.AuthRepository
import com.example.alo.domain.repository.ParticipantRepository
import com.example.alo.domain.repository.SharedTaskRepository
import com.example.alo.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sharedTaskRepository: SharedTaskRepository,
    private val participantRepository: ParticipantRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _task = MutableStateFlow<SharedTask?>(null)
    val task: StateFlow<SharedTask?> = _task.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    private val _assigneeName = MutableStateFlow<String?>(null)
    val assigneeName: StateFlow<String?> = _assigneeName.asStateFlow()

    private val _creatorName = MutableStateFlow<String?>(null)
    val creatorName: StateFlow<String?> = _creatorName.asStateFlow()

    // Danh sách thành viên nhóm (để admin chọn người nhận task)
    private val _members = MutableStateFlow<List<Participant>>(emptyList())
    val members: StateFlow<List<Participant>> = _members.asStateFlow()

    // Map userId -> displayName
    private val _memberNames = MutableStateFlow<Map<String, String>>(emptyMap())
    val memberNames: StateFlow<Map<String, String>> = _memberNames.asStateFlow()

    fun loadTaskDetails(taskId: String, conversationId: String) {
        viewModelScope.launch {
            val user = authRepository.getCurrentAuthUser()
            _currentUserId.value = user?.id

            if (user != null) {
                val participant = participantRepository.getParticipant(conversationId, user.id)
                _isAdmin.value = participant?.role == "admin"
            }

            // Fetch task
            val loadedTask = sharedTaskRepository.getTaskById(taskId)
            _task.value = loadedTask

            // Fetch assignee name
            loadedTask?.assigneeId?.let { id ->
                val assigneeUser = userRepository.getCurrentUser(id)
                _assigneeName.value = assigneeUser?.displayName ?: "Không xác định"
            }

            // Fetch creator name
            loadedTask?.creatorId?.let { id ->
                val creatorUser = userRepository.getCurrentUser(id)
                _creatorName.value = creatorUser?.displayName ?: "Không xác định"
            }

            // Fetch members for reassignment
            val participants = participantRepository.getParticipants(conversationId)
            _members.value = participants
            val namesMap = mutableMapOf<String, String>()
            participants.forEach { p ->
                val u = userRepository.getCurrentUser(p.userId)
                namesMap[p.userId] = u?.displayName ?: p.userId
            }
            _memberNames.value = namesMap
        }
    }

    fun updateTaskStatus(isCompleted: Boolean) {
        viewModelScope.launch {
            _task.value?.let { currentTask ->
                val updatedTask = currentTask.copy(isCompleted = isCompleted)
                val result = sharedTaskRepository.updateTask(updatedTask)
                if (result != null) {
                    _task.value = result
                }
            }
        }
    }

    fun saveTaskDetails(title: String, description: String, dueDate: String, priority: String, assigneeId: String?) {
        viewModelScope.launch {
            _task.value?.let { currentTask ->
                val updatedTask = currentTask.copy(
                    title = title,
                    description = description,
                    dueDate = com.example.alo.presentation.utils.parseTaskDueDateToIso(dueDate),
                    priority = priority,
                    assigneeId = assigneeId
                )
                val result = sharedTaskRepository.updateTask(updatedTask)
                if (result != null) {
                    _task.value = result
                    // Cập nhật lại tên assignee
                    assigneeId?.let { id ->
                        _assigneeName.value = _memberNames.value[id] ?: "Không xác định"
                    }
                }
            }
        }
    }
}
