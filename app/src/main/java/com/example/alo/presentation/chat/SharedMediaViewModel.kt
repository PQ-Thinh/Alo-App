package com.example.alo.presentation.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alo.domain.model.Attachment
import com.example.alo.domain.repository.AttachmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SharedMediaState(
    val conversationId: String = "",
    val attachments: List<Attachment> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SharedMediaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val attachmentRepository: AttachmentRepository
) : ViewModel() {

    private val conversationId: String = checkNotNull(savedStateHandle["conversationId"])
    
    private val _state = MutableStateFlow(SharedMediaState(conversationId = conversationId))
    val state: StateFlow<SharedMediaState> = _state.asStateFlow()

    init {
        loadAttachments()
    }

    private fun loadAttachments() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val attachments = attachmentRepository.getAttachmentsByConversation(conversationId)
                _state.update { it.copy(attachments = attachments, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
}
