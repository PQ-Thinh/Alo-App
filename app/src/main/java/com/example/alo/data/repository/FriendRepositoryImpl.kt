package com.example.alo.data.repository

import android.util.Log
import com.example.alo.data.remote.dto.FriendDto
import com.example.alo.data.remote.dto.FriendRequestDto
import com.example.alo.domain.model.Friend
import com.example.alo.domain.model.FriendRequest
import com.example.alo.domain.repositories.FriendRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
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

    override suspend fun sendFriendRequest(senderId: String, receiverId: String) {
        try {
            // Gửi dữ liệu không cần hứng về
            val requestBody = mapOf("sender_id" to senderId, "receiver_id" to receiverId)
            supabaseClient.postgrest["friend_requests"].insert(requestBody)
        } catch (e: Exception) {
            Log.e("FriendRepo", "Lỗi gửi kết bạn: ${e.message}")
        }
    }

    override suspend fun getFriends(userId: String): List<Friend> {
        return try {
            // Tìm bạn bè dựa trên OR condition (userId1 = me OR userId2 = me)
            val dtos = supabaseClient.postgrest["friends"]
                .select { filter { or {
                    eq("user_id_1", userId)
                    eq("user_id_2", userId)
                } } }
                .decodeList<FriendDto>()
            dtos.map { it.toDomain() }
        } catch (e: Exception) {
            Log.e("FriendRepo", "Lỗi lấy danh sách bạn bè: ${e.message}")
            emptyList()
        }
    }
}