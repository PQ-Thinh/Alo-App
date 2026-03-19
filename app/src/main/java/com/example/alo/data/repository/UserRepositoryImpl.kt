package com.example.alo.data.repository

import android.util.Log
import com.example.alo.data.remote.dto.UserDto
import com.example.alo.data.remote.dto.toDto
import com.example.alo.domain.model.User
import com.example.alo.domain.repository.UserRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import io.github.jan.supabase.postgrest.query.filter.TextSearchType
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.onCompletion


class UserRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : UserRepository {

    private var heartbeatJob: Job? = null

    override suspend fun getCurrentUser(userId: String): User? {
        return try {
            val userDto = supabaseClient.postgrest["users"]
                .select {
                    filter {
                        eq("id", userId)
                    }
                }.decodeSingleOrNull<UserDto>()

            userDto?.toDomain()

        } catch (e: Exception) {
            Log.e("UserRepository", "Lỗi lấy thông tin User: ${e.message}")
            null
        }
    }

    override suspend fun saveUserProfile(user: User): Boolean {
        return try {
            val currentUtcTime = Instant.now().toString()
            val userDto = user.copy(updatedAt = currentUtcTime).toDto()

            Log.d("UserRepository", "Đang gửi dữ liệu: $userDto")

            supabaseClient.postgrest["users"].upsert(userDto)
            true
        } catch (e: Exception) {
            Log.e("UserRepository", "Lỗi Supabase chi tiết: ${e.message}")
            false
        }
    }

    override suspend fun uploadAvatar(imageBytes: ByteArray, extension: String): String {
        return withContext(Dispatchers.IO) {
            val fileName = "${UUID.randomUUID()}.$extension"
            val bucket = supabaseClient.storage.from("avatars")

            bucket.upload(fileName, imageBytes)

            bucket.publicUrl(fileName)
        }
    }

    override suspend fun searchUsers(query: String): List<User> {
        return try {

            val formattedQuery = query.trim()
                .split("\\s+".toRegex())
                .joinToString(" & ") { "$it:*" }

            val dtos = supabaseClient.postgrest["users"]
                .select {
                    filter {
                        textSearch("fts_search", formattedQuery, TextSearchType.NONE)
                    }
                }
                .decodeList<UserDto>()
            dtos.map { it.toDomain() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun updateProfile(
        userId: String,
        updateData: Map<String, String>
    ): Boolean {
        return try {
            supabaseClient.postgrest["users"].update(updateData) {
                filter { eq("id", userId) }
            }
            true
        } catch (e: Exception) {
            Log.e("UserRepo", "Lỗi cập nhật Profile: ${e.message}")
            false
        }
    }

    override suspend fun updateLastSeen() {
        try {
            supabaseClient.postgrest.rpc("update_last_seen")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    override fun startHeartbeat() {
        stopHeartbeat()
        heartbeatJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                updateLastSeen()
                delay(60_000L)
            }
        }
    }

    override fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    override fun observeUserStatus(targetUserId: String): Flow<String?> {
        val statusChannel = supabaseClient.realtime.channel("user_status_$targetUserId")

        return statusChannel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
            table = "users"
            filter("id", FilterOperator.EQ, targetUserId)
        }.map { action ->
            val userDto = action.decodeRecord<UserDto>()
            userDto.lastSeen
        }.onStart {
            supabaseClient.realtime.connect()
            statusChannel.subscribe()
            Log.d("UserRepository", "Bắt đầu theo dõi online của user: $targetUserId")
        }.onCompletion {
            statusChannel.unsubscribe()
            Log.d("UserRepository", "Đã ngừng theo dõi user: $targetUserId")
        }
    }

    override fun observeListUsersStatus(userIds: List<String>): Flow<Pair<String, String?>> {

        if (userIds.isEmpty()) return kotlinx.coroutines.flow.emptyFlow()
        val idsString = userIds.joinToString(",")
        val statusChannel = supabaseClient.realtime.channel("users_status_list_${UUID.randomUUID()}")
        return statusChannel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
            table = "users"
            filter("id", FilterOperator.IN, idsString)
        }.map { action ->
            val userDto = action.decodeRecord<UserDto>()
            Pair(userDto.id, userDto.lastSeen)
        }.onStart {
            supabaseClient.realtime.connect()
            statusChannel.subscribe()
        }.onCompletion {
            statusChannel.unsubscribe()
        }
    }
}