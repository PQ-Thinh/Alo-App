package com.example.alo.presentation.helper

data class ProfileSetupState(
    val username: String = "",
    val displayName: String = "",
    val phone: String = "",
    val birthday: String = "",
    val gender: Boolean? = null,
    val bio: String = "",
    val avatarBytes: ByteArray? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)