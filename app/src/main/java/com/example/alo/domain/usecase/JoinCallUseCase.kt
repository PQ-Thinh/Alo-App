package com.example.alo.domain.usecase

import com.example.alo.domain.repository.VideoCallRepository
import io.getstream.video.android.core.Call
import javax.inject.Inject

class JoinCallUseCase @Inject constructor(
    private val videoCallRepository: VideoCallRepository
) {
    /**
     * Tham gia vào một cuộc gọi đang diễn ra (dùng khi nhận được push notification
     * incoming call và người dùng nhấn Accept).
     * [callId]: ID cuộc gọi lấy từ FCM payload.
     */
    suspend operator fun invoke(callId: String): Call {
        return videoCallRepository.joinCall(callId = callId)
    }
}
