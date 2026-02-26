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
    object Profile : Screen("profile/{userId}") {
        fun createRoute(userId: String) = "profile/$userId"
    }
}
sealed class Graph(val route: String) {
    object Root : Graph("root_graph")
    object Auth : Graph("auth_graph")
    object Main : Graph("main_graph")
}