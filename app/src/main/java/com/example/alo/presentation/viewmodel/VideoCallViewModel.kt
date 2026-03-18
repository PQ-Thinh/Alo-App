package com.example.alo.presentation.viewmodel


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alo.data.service.StreamVideoManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.getstream.video.android.core.Call
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoCallViewModel @Inject constructor(
    private val streamVideoManager: StreamVideoManager
) : ViewModel() {

    var call: Call? by mutableStateOf(null)
        private set

    fun joinCall(conversationId: String) {
        viewModelScope.launch {
            try {
                val client = streamVideoManager.getClient()
                val newCall = client.call(type = "default", id = conversationId)

                call = newCall
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun leaveCall() {
        call?.leave()
        call = null
    }
}