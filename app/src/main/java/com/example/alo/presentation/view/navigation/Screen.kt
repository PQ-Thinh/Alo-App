package com.example.alo.presentation.view.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login_screen")
    object Intro : Screen("intro_screen")
    object SignUp : Screen("signup_screen")
    object Dashboard : Screen("dashboard")
    object Profile : Screen("profile"){
        fun createRoute(userId: String) = "profile/$userId"
    }

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
    object ChatRoom : Screen("chat_room_screen/{conversationId}") {
        fun createRoute(conversationId: String) = "chat_room_screen/$conversationId"
    }
    object EditProfile : Screen("edit_profile/{userId}"){
        fun createRoute(userId: String) = "edit_profile/$userId"
    }

    object OutgoingCall : Screen("outgoing_call/{callId}/{calleeName}/{calleeAvatar}") {
        fun createRoute(callId: String, calleeName: String, calleeAvatar: String = "") =
            "outgoing_call/$callId/${calleeName.encodeUrl()}/${calleeAvatar.encodeUrl()}"
    }

    object IncomingCall : Screen("incoming_call/{callId}/{callerName}") {
        fun createRoute(callId: String, callerName: String) =
            "incoming_call/$callId/${callerName.encodeUrl()}"
    }

    object ActiveCall : Screen("active_call/{callId}") {
        fun createRoute(callId: String) = "active_call/$callId"
    }

    object CreateGroup : Screen("create_group")
}

private fun String.encodeUrl(): String =
    java.net.URLEncoder.encode(this, "UTF-8")

sealed class Graph(val route: String) {
    object Root : Graph("root_graph")
    object Auth : Graph("auth_graph")
    object Main : Graph("main_graph")
}