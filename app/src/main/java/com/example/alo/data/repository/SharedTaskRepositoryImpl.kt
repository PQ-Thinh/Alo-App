package com.example.alo.data.repository

import android.util.Log
import com.example.alo.domain.model.SharedTask
import com.example.alo.domain.repository.SharedTaskRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class SharedTaskRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : SharedTaskRepository {

    override suspend fun getTasks(conversationId: String): List<SharedTask> {
        return try {
            supabaseClient.postgrest[com.example.alo.core.utils.Constant.TABLE_SHARED_TASKS]
                .select { filter { eq("conversation_id", conversationId) } }
                .decodeList<SharedTask>()
        } catch (e: Exception) {
            Log.e("SharedTaskRepo", "Lỗi lấy danh sách task: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getTaskById(taskId: String): SharedTask? {
        return try {
            supabaseClient.postgrest[com.example.alo.core.utils.Constant.TABLE_SHARED_TASKS]
                .select { filter { eq("id", taskId) } }
                .decodeSingleOrNull<SharedTask>()
        } catch (e: Exception) {
            Log.e("SharedTaskRepo", "Lỗi lấy task theo ID: ${e.message}")
            null
        }
    }

    override suspend fun getAssignedTasks(userId: String): List<SharedTask> {
        return try {
            supabaseClient.postgrest[com.example.alo.core.utils.Constant.TABLE_SHARED_TASKS]
                .select(columns = io.github.jan.supabase.postgrest.query.Columns.raw("*, conversations(name)")) {
                    filter { 
                        eq("assignee_id", userId)
                        eq("is_completed", false)
                    }
                }
                .decodeList<SharedTask>()
        } catch (e: Exception) {
            Log.e("SharedTaskRepo", "Lỗi lấy danh sách task được giao: ${e.message}")
            emptyList()
        }
    }

    override suspend fun createTask(task: SharedTask): SharedTask? {
        return try {
            supabaseClient.postgrest[com.example.alo.core.utils.Constant.TABLE_SHARED_TASKS]
                .insert(task)
                .decodeSingle<SharedTask>()
        } catch (e: Exception) {
            Log.e("SharedTaskRepo", "Lỗi tạo task: ${e.message}")
            null
        }
    }

    override suspend fun updateTask(task: SharedTask): SharedTask? {
        return try {
            supabaseClient.postgrest[com.example.alo.core.utils.Constant.TABLE_SHARED_TASKS]
                .update(task) { filter { eq("id", task.id) } }
                .decodeSingle<SharedTask>()
        } catch (e: Exception) {
            Log.e("SharedTaskRepo", "Lỗi cập nhật task: ${e.message}")
            null
        }
    }

    override suspend fun deleteTask(taskId: String) {
        try {
            supabaseClient.postgrest[com.example.alo.core.utils.Constant.TABLE_SHARED_TASKS]
                .delete { filter { eq("id", taskId) } }
        } catch (e: Exception) {
            Log.e("SharedTaskRepo", "Lỗi xóa task: ${e.message}")
        }
    }

    override fun subscribeToTaskUpdates(conversationId: String): Flow<Unit> = callbackFlow {
        val channel = supabaseClient.channel("shared_tasks_update_$conversationId")

        val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "shared_tasks"
        }

        val job = launch {
            changeFlow.collect {
                trySend(Unit)
            }
        }

        channel.subscribe()

        awaitClose {
            job.cancel()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    supabaseClient.realtime.removeChannel(channel)
                } catch (e: Exception) { }
            }
        }
    }
}
