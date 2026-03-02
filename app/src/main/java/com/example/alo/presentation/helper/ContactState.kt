package com.example.alo.presentation.helper
import com.example.alo.domain.model.User

data class ContactState(
    val isLoading: Boolean = false,
    val pendingRequests: List<User> = emptyList(),
    val friends: List<User> = emptyList(),
    val error: String? = null,
    val successMessage: String? = null
)