package com.example.alo.presentation.view.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login_screen")
    object Intro : Screen("intro_screen")
    object SignUp : Screen("signup_screen")
    object Dashboard : Screen("dashboard_screen")
    object ChatRoom : Screen("chat_room_screen")
}