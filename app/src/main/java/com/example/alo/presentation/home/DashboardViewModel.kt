package com.example.alo.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alo.domain.model.SharedTask
import com.example.alo.domain.repository.AuthRepository
import com.example.alo.domain.repository.SharedTaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sharedTaskRepository: SharedTaskRepository
) : ViewModel() {

    private val _assignedTasks = MutableStateFlow<List<SharedTask>>(emptyList())
    val assignedTasks: StateFlow<List<SharedTask>> = _assignedTasks.asStateFlow()

    private val _hasAssignedTasks = MutableStateFlow(false)
    val hasAssignedTasks: StateFlow<Boolean> = _hasAssignedTasks.asStateFlow()

    init {
        checkAssignedTasks()
    }

    fun checkAssignedTasks() {
        viewModelScope.launch {
            val user = authRepository.getCurrentAuthUser()
            if (user != null) {
                val tasks = sharedTaskRepository.getAssignedTasks(user.id)
                _assignedTasks.value = tasks
                _hasAssignedTasks.value = tasks.isNotEmpty()
            }
        }
    }
}
