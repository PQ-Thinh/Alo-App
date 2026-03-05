package com.example.alo.data.repository

import android.util.Log
import com.example.alo.data.remote.dto.FriendDto
import com.example.alo.data.remote.dto.FriendRequestDto
import com.example.alo.data.remote.dto.UserDto
import com.example.alo.domain.model.FriendRequest
import com.example.alo.domain.model.User
import com.example.alo.domain.repository.FriendRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.postgrest.rpc
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

class FriendRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : FriendRepository {

    override suspend fun getFriendRequests(userId: String): List<FriendRequest> {
        return try {
            val dtos = supabaseClient.postgrest["friend_requests"]
                .select { filter { eq("receiver_id", userId) } }
                .decodeList<FriendRequestDto>()
            dtos.map { it.toDomain() }
        } catch (e: Exception) {
            Log.e("FriendRepo", "Lỗi lấy danh sách kết bạn: ${e.message}")
            emptyList()
        }
    }

    override suspend fun sendFriendRequest(senderId: String, receiverId: String): Boolean {
        return try {
            val params = mapOf(
                "p_sender_id" to senderId,
                "p_receiver_id" to receiverId
            )
            supabaseClient.postgrest.rpc("upsert_friend_request", params)
            true
        } catch (e: Exception) {
            Log.e("FriendRepo", "Lỗi gửi lời mời: ${e.message}")
            false
        }
    }

    override suspend fun checkFriendStatus(
        currentUserId: String,
        targetUserId: String
    ): String {
        return try {
            val friendsData = supabaseClient.postgrest["friends"]
                .select {
                    filter {
                        eq("status", "pending")
                        or {
                            and { eq("user_id_1", currentUserId); eq("user_id_2", targetUserId) }
                            and { eq("user_id_1", targetUserId); eq("user_id_2", currentUserId) }
                        }
                    }
                }.data

            if (friendsData != "[]") return "friends"

            val requestData = supabaseClient.postgrest["friend_requests"]
                .select {
                    filter {
                        or {
                            and { eq("sender_id", currentUserId); eq("receiver_id", targetUserId) }
                            and { eq("sender_id", targetUserId); eq("receiver_id", currentUserId) }
                        }
                    }
                }.data

            when {
                requestData.contains("\"sender_id\":\"$currentUserId\"") -> "request_sent"
                requestData.contains("\"receiver_id\":\"$currentUserId\"") -> "request_received"
                else -> "none"
            }
        } catch (e: Exception) {
            Log.e("FriendRepo", "Lỗi check status: ${e.message}")
            "none"
        }
    }

    override suspend fun getPendingFriendRequests(currentUserId: String): List<User> {
        return try {
            val requests = supabaseClient.postgrest["friend_requests"]
                .select {
                    filter {
                        eq("receiver_id", currentUserId)
                        eq("status", "pending")
                    }
                }.decodeList<FriendRequestDto>()

            if (requests.isEmpty()) return emptyList()

            val senderIds = requests.map { it.senderId }
            val senders = supabaseClient.postgrest["users"]
                .select {
                    filter {
                        isIn("id", senderIds)
                    }
                }.decodeList<UserDto>()

            senders.map { it.toDomain() }
        } catch (e: Exception) {
            Log.e("FriendRepo", "Lỗi lấy danh sách lời mời: ${e.message}")
            emptyList()
        }
    }

    override suspend fun acceptFriendRequest(senderId: String, receiverId: String): Boolean {
        return try {
            val params = mapOf(
                "p_sender_id" to senderId,
                "p_receiver_id" to receiverId
            )

            supabaseClient.postgrest.rpc("accept_friend_request", params)

            true
        } catch (e: Exception) {
            Log.e("FriendRepo", "Lỗi chấp nhận kết bạn qua RPC: ${e.message}")
            false
        }
    }

    override suspend fun declineFriendRequest(senderId: String, receiverId: String): Boolean {
        return try {
            supabaseClient.postgrest["friend_requests"].update(
                { set("status", "declined") }
            ) {
                filter {
                    eq("sender_id", senderId)
                    eq("receiver_id", receiverId)
                }
            }
            true
        } catch (e: Exception) {
            Log.e("FriendRepo", "Lỗi từ chối kết bạn: ${e.message}")
            false
        }
    }

    override suspend fun getFriendsList(currentUserId: String): List<User> {
        return try {
            val friendsData = supabaseClient.postgrest["friends"]
                .select {
                    filter {
                        or {
                            eq("user_id_1", currentUserId)
                            eq("user_id_2", currentUserId)
                        }
                    }
                }.decodeList<FriendDto>()

            if (friendsData.isEmpty()) return emptyList()

            val friendIds = friendsData.map {
                if (it.userId1 == currentUserId) it.userId2 else it.userId1
            }

            val friendsProfiles = supabaseClient.postgrest["users"]
                .select {
                    filter {
                        isIn("id", friendIds)
                    }
                }.decodeList<UserDto>()

            friendsProfiles.map { it.toDomain() }
        } catch (e: Exception) {
            Log.e("FriendRepo", "Lỗi lấy danh sách bạn bè: ${e.message}")
            emptyList()
        }
    }
    override fun subscribeToFriendReQuestListUpdates(receiverId: String): Flow<Unit> = callbackFlow {
        supabaseClient.realtime.connect()

        val channel = supabaseClient.channel("friend_request_update_$receiverId")

        val insertFlow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "friend_requests"
            filter("receiver_id", FilterOperator.EQ, receiverId)
        }

        val updateFlow = channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
            table = "friend_requests"
            filter("receiver_id", FilterOperator.EQ, receiverId)
        }

        val deleteFlow = channel.postgresChangeFlow<PostgresAction.Delete>(schema = "public") {
            table = "friend_requests"
            filter("receiver_id", FilterOperator.EQ, receiverId)
        }

        val jobInsert = launch { insertFlow.collect { trySend(Unit) } }
        val jobUpdate = launch { updateFlow.collect { trySend(Unit) } }
        val jobDelete = launch { deleteFlow.collect { trySend(Unit) } }

        channel.subscribe()

        awaitClose {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    supabaseClient.realtime.removeChannel(channel)
                } catch (e: Exception) {
                    Log.e("FriendRepo", "Lỗi đóng channel: ${e.message}")
                }
            }
        }
    }
    override fun subscribeToFriendListUpdates(userId: String): Flow<Unit> = callbackFlow {
        supabaseClient.realtime.connect()

        val channel = supabaseClient.channel("friend_list_update_$userId")

        // user_id_1 ---
        val insertFlow1 = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "friends"
            filter("user_id_1", FilterOperator.EQ, userId)
        }
        val deleteFlow1 = channel.postgresChangeFlow<PostgresAction.Delete>(schema = "public") {
            table = "friends"
            filter("user_id_1", FilterOperator.EQ, userId)
        }

        //  user_id_2 ---
        val insertFlow2 = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "friends"
            filter("user_id_2", FilterOperator.EQ, userId)
        }
        val deleteFlow2 = channel.postgresChangeFlow<PostgresAction.Delete>(schema = "public") {
            table = "friends"
            filter("user_id_2", FilterOperator.EQ, userId)
        }

        val jobInsert1 = launch { insertFlow1.collect { trySend(Unit) } }
        val jobDelete1 = launch { deleteFlow1.collect { trySend(Unit) } }
        val jobInsert2 = launch { insertFlow2.collect { trySend(Unit) } }
        val jobDelete2 = launch { deleteFlow2.collect { trySend(Unit) } }

        channel.subscribe()

        awaitClose {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    supabaseClient.realtime.removeChannel(channel)
                } catch (e: Exception) {
                    Log.e("FriendRepo", "Lỗi đóng channel friend_list: ${e.message}")
                }
            }
        }
    }

}