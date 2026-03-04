package com.example.alo.presentation.helper

data class SearchState(
    val isLoading: Boolean = false,
    val searchResults: List<UserSearchResult> = emptyList(),
    val error: String? = null,
    val successMessage: String? = null,
    val navigateToConversationId: String? = null
)