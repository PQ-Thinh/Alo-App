package com.example.alo.domain.usecase

import com.example.alo.domain.repository.VideoCallRepository
import io.getstream.video.android.core.Call
import javax.inject.Inject

class CreateCallUseCase @Inject constructor(
    private val videoCallRepository: VideoCallRepository
) {
    /**
     * Tạo một cuộc gọi mới và ring tới các thành viên.
     * [callId]: ID duy nhất cho cuộc gọi (ví dụ: conversationId hoặc UUID mới).
     * [memberIds]: Danh sách userId của người nhận.
     */
    suspend operator fun invoke(
        callId: String,
        memberIds: List<String>
    ): Call {
        return videoCallRepository.createAndJoinCall(
            callId = callId,
            memberIds = memberIds
        )
    }
}
