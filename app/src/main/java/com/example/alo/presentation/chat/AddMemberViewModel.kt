package com.example.alo.presentation.chat

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alo.core.crypto.CryptoHelper
import com.example.alo.domain.model.User
import com.example.alo.domain.repository.AuthRepository
import com.example.alo.domain.repository.ConversationRepository
import com.example.alo.domain.repository.ParticipantRepository
import com.example.alo.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddMemberState(
    val conversationId: String = "",
    val searchQuery: String = "",
    val searchResults: List<User> = emptyList(),
    val currentMembers: Set<String> = emptySet(),
    val selectedUserIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val isAdding: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val groupKeysetBase64: String? = null
)

@HiltViewModel
class AddMemberViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository,
    private val participantRepository: ParticipantRepository,
    private val authRepository: AuthRepository,
    private val conversationRepository: ConversationRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val conversationId: String = checkNotNull(savedStateHandle["conversationId"])
    private val _state = MutableStateFlow(AddMemberState(conversationId = conversationId))
    val state: StateFlow<AddMemberState> = _state.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadCurrentMembersAndKey()
    }

    private fun loadCurrentMembersAndKey() {
        viewModelScope.launch {
            try {
                val participants = participantRepository.getParticipants(conversationId)
                val currentIds = participants.map { it.userId }.toSet()
                
                val currentUser = authRepository.getCurrentAuthUser() ?: return@launch
                val myParticipant = participants.find { it.userId == currentUser.id }
                
                var keysetBase64: String? = null
                myParticipant?.encryptedGroupKey?.let { wrappedKey ->
                     keysetBase64 = CryptoHelper.unwrapGroupKey(context, wrappedKey)
                }

                _state.update { it.copy(
                    currentMembers = currentIds,
                    groupKeysetBase64 = keysetBase64
                ) }
            } catch (e: Exception) {
                Log.e("AddMemberVM", "Lỗi load current members: ${e.message}")
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _state.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _state.update { it.copy(searchResults = emptyList()) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(500)
            _state.update { it.copy(isLoading = true) }
            try {
                val results = userRepository.searchUsers(query)
                _state.update { it.copy(searchResults = results, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun toggleSelection(userId: String) {
        if (_state.value.currentMembers.contains(userId)) return
        _state.update { currentState ->
            val newSelection = if (currentState.selectedUserIds.contains(userId)) {
                currentState.selectedUserIds - userId
            } else {
                currentState.selectedUserIds + userId
            }
            currentState.copy(selectedUserIds = newSelection)
        }
    }

    fun addMembers() {
        val selectedIds = _state.value.selectedUserIds.toList()
        val keysetBase64 = _state.value.groupKeysetBase64
        
        if (selectedIds.isEmpty()) return
        if (keysetBase64 == null) {
            _state.update { it.copy(error = "Không thể lấy Group Key để mời thành viên mới") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isAdding = true, error = null) }
            try {
                // Lấy profile đầy đủ để có publicEncryptKey
                val profiles = userRepository.getUsersByIds(selectedIds)
                
                for (profile in profiles) {
                    val encryptedKey = if (profile.publicEncryptKey.isNotEmpty()) {
                        CryptoHelper.wrapGroupKey(keysetBase64, profile.publicEncryptKey)
                    } else null
                    
                    participantRepository.addParticipant(
                        conversationId = conversationId,
                        userId = profile.id,
                        role = "member",
                        encryptedGroupKey = encryptedKey
                    )
                }
                _state.update { it.copy(isAdding = false, success = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isAdding = false, error = e.message) }
            }
        }
    }
}
