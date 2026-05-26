package com.example.alo.domain.repository

import com.example.alo.domain.model.SharedTask
import kotlinx.coroutines.flow.Flow

interface SharedTaskRepository {
    suspend fun getTasks(conversationId: String): List<SharedTask>
    suspend fun getTaskById(taskId: String): SharedTask?
    suspend fun getAssignedTasks(userId: String): List<SharedTask>
    suspend fun createTask(task: SharedTask): SharedTask?
    suspend fun updateTask(task: SharedTask): SharedTask?
    suspend fun deleteTask(taskId: String)
    fun subscribeToTaskUpdates(conversationId: String): Flow<Unit>
}
