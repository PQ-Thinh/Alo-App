package com.example.alo.data.service

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.getstream.video.android.core.GEO
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.model.User
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamVideoManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var client: StreamVideo? = null

    /**
     * Khởi tạo Client với thông tin User thật
     */
    fun initialize(apiKey: String, userId: String, userName: String, avatarUrl: String?, token: String) {
        if (client != null) return

        val streamUser = User(
            id = userId,
            name = userName,
            image = avatarUrl ?: "https://ui-avatars.com/api/?name=$userName&background=6C63FF&color=fff",
        )

        client = StreamVideoBuilder(
            context = context,
            apiKey = apiKey,
            geo = GEO.GlobalEdgeNetwork,
            user = streamUser,
            token = token,
        ).build()
    }

    fun getClient(): StreamVideo {
        return client ?: throw IllegalStateException("StreamVideo chưa được khởi tạo! Hãy gọi initialize() sau khi đăng nhập thành công.")
    }

    fun disconnect() {
        client?.logOut()
        client = null
    }
}