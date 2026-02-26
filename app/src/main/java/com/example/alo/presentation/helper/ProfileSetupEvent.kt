package com.example.alo.presentation.helper

sealed class ProfileSetupEvent {
    data class EnteredUsername(val value: String) : ProfileSetupEvent()
    data class EnteredDisplayName(val value: String) : ProfileSetupEvent()
    data class EnteredPhone(val value: String) : ProfileSetupEvent()
    data class EnteredBirthday(val value: String) : ProfileSetupEvent()
    data class SelectedGender(val value: Boolean) : ProfileSetupEvent()
    data class EnteredBio(val value: String) : ProfileSetupEvent()
    data class SelectedAvatar(val bytes: ByteArray?) : ProfileSetupEvent()
    object Submit : ProfileSetupEvent()
}