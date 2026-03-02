package com.example.alo.presentation.helper

import com.example.alo.domain.model.User

data class UserSearchResult(
    val user: User,
    val relationStatus: String
)