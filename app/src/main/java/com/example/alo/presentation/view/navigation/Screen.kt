package com.example.alo.presentation.view.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login_screen")
    object Intro : Screen("intro_screen")
    object SignUp : Screen("signup_screen")

    object ChatRoom : Screen("chat_room_screen")
    object Dashboard : Screen("dashboard")
    object Profile : Screen("profile")

    object ProfileSetup : Screen("profile_setup/{userId}/{email}") {
        fun createRoute(userId: String, email: String) = "profile_setup/$userId/$email"
    }
    object OtpVerification : Screen("otp_verification/{email}") {
        fun createRoute(email: String) = "otp_verification/$email"
    }
    object ForgotPassword : Screen("forgot_password")
    object ResetPasswordOtp : Screen("reset_password_otp/{email}") {
        fun createRoute(email: String) = "reset_password_otp/$email"
    }
    object CreateNewPassword : Screen("create_new_password")
}
sealed class Graph(val route: String) {
    object Root : Graph("root_graph")
    object Auth : Graph("auth_graph")
    object Main : Graph("main_graph")
}