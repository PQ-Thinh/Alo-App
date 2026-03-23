package com.example.alo.presentation.view.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.alo.presentation.view.auth.CreateNewPasswordScreen
import com.example.alo.presentation.view.auth.ForgotPasswordScreen
import com.example.alo.presentation.view.home.IntroScreen
import com.example.alo.presentation.view.auth.LoginScreen
import com.example.alo.presentation.view.auth.OtpVerificationScreen
import com.example.alo.presentation.view.auth.ResetPasswordOtpScreen
import com.example.alo.presentation.view.profile.ProfileSetupScreen
import com.example.alo.presentation.view.auth.SignUpScreen
import com.example.alo.presentation.view.call.ActiveCallScreen
import com.example.alo.presentation.view.call.IncomingCallScreen
import com.example.alo.presentation.view.call.OutgoingCallScreen
import com.example.alo.presentation.view.chat.ChatRoomScreen
import com.example.alo.presentation.view.home.AnimatedSplashScreen
import com.example.alo.presentation.view.home.DashboardScreen
import com.example.alo.presentation.view.profile.EditProfileScreen
import com.example.alo.presentation.viewmodel.CallViewModel
import com.example.alo.presentation.viewmodel.CallUiState
import com.example.alo.presentation.viewmodel.UserViewModel
import java.net.URLDecoder

@Composable
fun AppNavigation(
    startDestination: String,
    pushConversationId: String?,
    pushCallId: String? = null        // FCM incoming call payload
) {
    val navController = rememberNavController()

    // Navigate to chat room from push notification
    LaunchedEffect(pushConversationId) {
        if (pushConversationId != null) {
            navController.navigate("chat_room_screen/$pushConversationId")
        }
    }

    // Navigate to incoming call from FCM push
    LaunchedEffect(pushCallId) {
        if (pushCallId != null) {
            navController.navigate(Screen.IncomingCall.createRoute(pushCallId, "Cuộc gọi đến"))
        }
    }

    NavHost(
        navController = navController,
        startDestination = "animated_splash"
    ) {
        composable("animated_splash") {
            AnimatedSplashScreen(
                onSplashFinished = {
                    navController.navigate(startDestination) {
                        popUpTo("animated_splash") { inclusive = true }
                    }
                }
            )
        }
        composable(route = Screen.Intro.route) {
            IntroScreen(navController = navController)
        }

        composable(route = Screen.Login.route) {
            LoginScreen(navController = navController)
        }

        composable(route = Screen.SignUp.route) {
            SignUpScreen(navController = navController)
        }
        composable(
            route = Screen.OtpVerification.route,
            arguments = listOf(navArgument("email") { type = NavType.StringType })
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            OtpVerificationScreen(navController = navController, email = email)
        }
        composable(
            route = Screen.ProfileSetup.route,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("email") { type = NavType.StringType }
            )
        ) {
            ProfileSetupScreen(
                navController = navController,
                onSetupComplete = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.ProfileSetup.route) { inclusive = true }
                    }
                }
            )
        }
        composable(route = Screen.ForgotPassword.route) {
            ForgotPasswordScreen(navController = navController)
        }

        composable(
            route = Screen.ResetPasswordOtp.route,
            arguments = listOf(navArgument("email") { type = NavType.StringType })
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            ResetPasswordOtpScreen(navController = navController, email = email)
        }

        composable(route = Screen.CreateNewPassword.route) {
            CreateNewPasswordScreen(navController = navController)
        }

        composable(
            route = Screen.EditProfile.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable

            val userViewModel: UserViewModel = hiltViewModel()
            EditProfileScreen(
                navController = navController,
                userViewModel = userViewModel
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                navController = navController,
                onNavigateToChatRoom = { conversationId ->
                    navController.navigate(Screen.ChatRoom.createRoute(conversationId))
                },
                onNavigateToProfile = { currentUserId ->
                    navController.navigate(Screen.Profile.createRoute(currentUserId))
                }
            )
        }


        composable(
            route = Screen.ChatRoom.route,
            arguments = listOf(
                navArgument("conversationId") { type = NavType.StringType }
            )
        ) {

            ChatRoomScreen(navController = navController)
        }
        composable(Screen.EditProfile.route) {
            val userViewModel: UserViewModel = hiltViewModel()
            EditProfileScreen(
                navController = navController,
                userViewModel = userViewModel
            )
        }

        // ──────────────────────────────────────────
        // VIDEO CALL ROUTES
        // ──────────────────────────────────────────

        // Outgoing call
        composable(
            route = Screen.OutgoingCall.route,
            arguments = listOf(
                navArgument("callId") { type = NavType.StringType },
                navArgument("calleeName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val callId = backStackEntry.arguments?.getString("callId") ?: return@composable
            val calleeName = URLDecoder.decode(
                backStackEntry.arguments?.getString("calleeName") ?: "", "UTF-8"
            )
            val callViewModel: CallViewModel = hiltViewModel()
            val uiState = callViewModel.uiState

            LaunchedEffect(Unit) {
                // callViewModel.startCall đã được gọi từ ChatRoomScreen
                // Nếu state chưa phải Calling, có thể retry ở đây
            }

            val state = uiState.value
            if (state is CallUiState.Calling) {
                OutgoingCallScreen(
                    call = state.call,
                    calleeName = calleeName,
                    calleeAvatar = null,
                    onCallEnded = {
                        callViewModel.endCall()
                        navController.popBackStack()
                    }
                )
            }
        }

        // Incoming call (từ FCM)
        composable(
            route = Screen.IncomingCall.route,
            arguments = listOf(
                navArgument("callId") { type = NavType.StringType },
                navArgument("callerName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val callId = backStackEntry.arguments?.getString("callId") ?: return@composable
            val callerName = URLDecoder.decode(
                backStackEntry.arguments?.getString("callerName") ?: "Không rõ", "UTF-8"
            )
            val callViewModel: CallViewModel = hiltViewModel()

            IncomingCallScreen(
                callerName = callerName,
                callerAvatar = null,
                onAccept = {
                    callViewModel.acceptCall(callId)
                    navController.navigate(Screen.ActiveCall.createRoute(callId)) {
                        popUpTo(Screen.IncomingCall.route) { inclusive = true }
                    }
                },
                onDecline = {
                    navController.popBackStack()
                }
            )
        }

        // Active call
        composable(
            route = Screen.ActiveCall.route,
            arguments = listOf(
                navArgument("callId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val callId = backStackEntry.arguments?.getString("callId") ?: return@composable
            val callViewModel: CallViewModel = hiltViewModel()

            val state = callViewModel.uiState.value
            if (state is CallUiState.InCall) {
                ActiveCallScreen(
                    call = state.call,
                    onCallEnded = {
                        callViewModel.endCall()
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}