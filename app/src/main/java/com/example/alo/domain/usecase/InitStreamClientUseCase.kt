package com.example.alo.domain.usecase

import com.example.alo.domain.repository.VideoCallRepository
import javax.inject.Inject

class InitStreamClientUseCase @Inject constructor(
    private val videoCallRepository: VideoCallRepository
) {
    /**
     * Khởi tạo StreamVideo SDK client sau khi user đã đăng nhập thành công.
     * Gọi hàm này ngay sau login hoặc khi app resume với user đã có session.
     */
    suspend operator fun invoke(
        userId: String,
        displayName: String,
        avatarUrl: String? = null
    ) {
        videoCallRepository.initStreamClient(
            userId = userId,
            displayName = displayName,
            avatarUrl = avatarUrl
        )
    }
}
