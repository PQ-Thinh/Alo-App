package com.example.alo.data.repository

import android.util.Log
import com.example.alo.data.remote.dto.UserDto
import com.example.alo.domain.model.User
import com.example.alo.domain.repositories.UserRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : UserRepository {

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
            val userDto = user.id
            supabaseClient.postgrest["users"].upsert(userDto)
            true
        } catch (e: Exception) {
            Log.e("UserRepo", "Lỗi lưu Profile: ${e.message}")
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
}