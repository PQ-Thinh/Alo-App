package com.example.alo.presentation.view.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login_screen")
    object Intro : Screen("intro_screen")
    object SignUp : Screen("signup_screen")
    object ChatRoom : Screen("chat_room_screen")
    object Dashboard : Screen("dashboard/{userId}") {
        fun createRoute(userId: String) = "dashboard/$userId"
    }
    object ProfileSetup : Screen("profile_setup/{userId}/{email}") {
        fun createRoute(userId: String, email: String) = "profile_setup/$userId/$email"
    }
}