package com.example.alo.presentation.view.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.alo.presentation.helper.CallUiState
import com.example.alo.presentation.view.auth.CreateNewPasswordScreen
import com.example.alo.presentation.view.auth.ForgotPasswordScreen
import com.example.alo.presentation.view.auth.LoginScreen
import com.example.alo.presentation.view.auth.OtpVerificationScreen
import com.example.alo.presentation.view.auth.ResetPasswordOtpScreen
import com.example.alo.presentation.view.auth.SignUpScreen
import com.example.alo.presentation.view.call.ActiveCallScreen
import com.example.alo.presentation.view.call.IncomingCallScreen
import com.example.alo.presentation.view.call.OutgoingCallScreen
import com.example.alo.presentation.view.chat.ChatRoomScreen
import com.example.alo.presentation.view.home.AnimatedSplashScreen
import com.example.alo.presentation.view.home.DashboardScreen
import com.example.alo.presentation.view.home.IntroScreen
import com.example.alo.presentation.view.profile.EditProfileScreen
import com.example.alo.presentation.view.profile.ProfileSetupScreen
import com.example.alo.presentation.viewmodel.CallViewModel
import com.example.alo.presentation.viewmodel.UserViewModel
import java.net.URLDecoder

@Composable
fun AppNavigation(
    startDestination: String,
    pushConversationId: String?,
    pushCallId: String? = null,
    pushCallerName: String? = null,
    pushCallAction: String? = null,
    onClearPushDetails: () -> Unit = {}
) {
    val navController = rememberNavController()
    val callViewModel: CallViewModel = hiltViewModel()

    // Navigate to chat room from push notification
    LaunchedEffect(pushConversationId) {
        if (pushConversationId != null) {
            navController.navigate("chat_room_screen/$pushConversationId")
        }
    }

    // Navigate to incoming call from FCM push
    LaunchedEffect(pushCallId, pushCallAction) {
        if (pushCallId != null) {
            callViewModel.initStreamClient()
            when (pushCallAction) {
                com.example.alo.MainActivity.ACTION_INCOMING_CALL_ACCEPT -> {
                    callViewModel.acceptCall(pushCallId)
                    navController.navigate(Screen.ActiveCall.createRoute(pushCallId)) {
                        popUpTo(0) { inclusive = false }
                    }
                }
                com.example.alo.MainActivity.ACTION_INCOMING_CALL_DECLINE -> {
                    callViewModel.rejectCall(pushCallId)
                }
                else -> {
                    navController.navigate(
                        Screen.IncomingCall.createRoute(pushCallId, pushCallerName ?: "Cuộc gọi đến")
                    )
                }
            }
            onClearPushDetails() // Xóa sạch dấu vết sau khi đã xử lý xong
        }
    }

    val actualStart = if (pushCallId != null || pushConversationId != null) {
        startDestination
    } else {
        "animated_splash"
    }

    NavHost(
        navController = navController,
        startDestination = actualStart
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
            LaunchedEffect(Unit) {
                callViewModel.initStreamClient()
            }
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
            ChatRoomScreen(
                navController = navController,
                callViewModel = callViewModel
            )
        }

        // ──────────────────────────────────────────
        // Gọi Graph riêng biệt của Video Call
        // ──────────────────────────────────────────
        videoCallGraph(navController, callViewModel)
    }
}

// ──────────────────────────────────────────
// TÁCH RIÊNG LUỒNG VIDEO CALL ROUTES VÀO ĐÂY
// ──────────────────────────────────────────
fun NavGraphBuilder.videoCallGraph(
    navController: NavController,
    callViewModel: CallViewModel
) {
    // 1. Outgoing call
    composable(
        route = Screen.OutgoingCall.route,
        arguments = listOf(
            navArgument("callId") { type = NavType.StringType },
            navArgument("calleeName") { type = NavType.StringType },
            navArgument("calleeAvatar") { type = NavType.StringType; defaultValue = "" }
        )
    ) { backStackEntry ->
        val callId = backStackEntry.arguments?.getString("callId") ?: return@composable
        val calleeName = URLDecoder.decode(backStackEntry.arguments?.getString("calleeName") ?: "", "UTF-8")
        val calleeAvatar = URLDecoder.decode(backStackEntry.arguments?.getString("calleeAvatar") ?: "", "UTF-8").takeIf { it.isNotBlank() }

        val state = callViewModel.uiState.collectAsState().value
        val networkStatus = callViewModel.networkStatus.collectAsState().value

        // Tự động thoát nếu đối phương hủy cuộc gọi
        LaunchedEffect(state) {
            if (state is CallUiState.Ended || state is CallUiState.Idle) {
                navController.popBackStack()
            }
        }

        OutgoingCallScreen(
            uiState = state,
            onCallEnded = {
                callViewModel.endCall()
                navController.popBackStack()
            },
            onCallAccepted = { acceptedCall ->
                callViewModel.onCallAccepted(acceptedCall)
                navController.navigate(Screen.ActiveCall.createRoute(callId)) {
                    popUpTo(Screen.OutgoingCall.route) { inclusive = true }
                }
            },
            networkStatus = networkStatus
        )
    }

    // 2. Incoming call (từ FCM)
    composable(
        route = Screen.IncomingCall.route,
        arguments = listOf(
            navArgument("callId") { type = NavType.StringType },
            navArgument("callerName") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val callId = backStackEntry.arguments?.getString("callId") ?: return@composable
        val callerName = URLDecoder.decode(backStackEntry.arguments?.getString("callerName") ?: "Không rõ", "UTF-8")

        val state = callViewModel.uiState.collectAsState().value

        // Tự động thoát nếu người gọi hủy cuộc gọi trước khi mình bắt máy
        LaunchedEffect(state) {
            if (state is CallUiState.Ended || state is CallUiState.Idle) {
                navController.popBackStack()
            }
        }

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
                callViewModel.rejectCall(callId)
                navController.popBackStack()
            }
        )
    }

    // 3. Active call
    composable(
        route = Screen.ActiveCall.route,
        arguments = listOf(
            navArgument("callId") { type = NavType.StringType }
        )
    ) {
        val state = callViewModel.uiState.collectAsState().value
        val networkStatus = callViewModel.networkStatus.collectAsState().value

        // Chống kẹt màn hình: Nếu state rớt khỏi InCall (Bị cúp máy), lập tức tắt màn hình
        LaunchedEffect(state) {
            if (state is CallUiState.Ended || state is CallUiState.Idle || state is CallUiState.Error) {
                navController.popBackStack()
            }
        }

        if (state is CallUiState.InCall) {
            ActiveCallScreen(
                call = state.call,
                onCallEnded = {
                    callViewModel.endCall()
                    navController.popBackStack()
                },
                networkStatus = networkStatus
            )
        }
    }
}
