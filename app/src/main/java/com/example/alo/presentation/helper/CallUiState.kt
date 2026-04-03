package com.example.alo.presentation.helper

import io.getstream.video.android.core.Call

sealed class CallUiState {
    object Idle : CallUiState()
    object Initializing : CallUiState()
    data class Calling(val call: Call) : CallUiState()
    data class InCall(val call: Call) : CallUiState()
    object Ended : CallUiState()
    data class Error(val message: String) : CallUiState()
}

enum class NetworkStatus {
    Connected,
    Reconnecting
}
