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
            supabaseClient.postgrest["shared_tasks"]
                .select { filter { eq("conversation_id", conversationId) } }
                .decodeList<SharedTask>()
        } catch (e: Exception) {
            Log.e("SharedTaskRepo", "Lỗi lấy danh sách task: ${e.message}")
            emptyList()
        }
    }

    override suspend fun createTask(task: SharedTask): SharedTask? {
        return try {
            supabaseClient.postgrest["shared_tasks"]
                .insert(task)
                .decodeSingle<SharedTask>()
        } catch (e: Exception) {
            Log.e("SharedTaskRepo", "Lỗi tạo task: ${e.message}")
            null
        }
    }

    override suspend fun updateTask(task: SharedTask): SharedTask? {
        return try {
            supabaseClient.postgrest["shared_tasks"]
                .update(task) { filter { eq("id", task.id) } }
                .decodeSingle<SharedTask>()
        } catch (e: Exception) {
            Log.e("SharedTaskRepo", "Lỗi cập nhật task: ${e.message}")
            null
        }
    }

    override suspend fun deleteTask(taskId: String) {
        try {
            supabaseClient.postgrest["shared_tasks"]
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
