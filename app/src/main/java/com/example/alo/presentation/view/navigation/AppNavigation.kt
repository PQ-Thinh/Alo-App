package com.example.alo.presentation.view.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.alo.presentation.helper.CallUiState
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
import com.example.alo.presentation.viewmodel.UserViewModel
import java.net.URLDecoder
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AppNavigation(
    startDestination: String,
    pushConversationId: String?,
    pushCallId: String? = null,
    pushCallerName: String? = null
) {
    val navController = rememberNavController()
    val callViewModel: CallViewModel = hiltViewModel()

    // (Stream client khởi tạo được di chuyển vào DashboardScreen route)

    // Navigate to chat room from push notification
    LaunchedEffect(pushConversationId) {
        if (pushConversationId != null) {
            navController.navigate("chat_room_screen/$pushConversationId")
        }
    }

    // Navigate to incoming call from FCM push
    LaunchedEffect(pushCallId) {
        if (pushCallId != null) {
            navController.navigate(
                Screen.IncomingCall.createRoute(pushCallId, pushCallerName ?: "Cuộc gọi đến")
            )
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
                navArgument("calleeName") { type = NavType.StringType },
                navArgument("calleeAvatar") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val callId = backStackEntry.arguments?.getString("callId") ?: return@composable
            val calleeName = URLDecoder.decode(
                backStackEntry.arguments?.getString("calleeName") ?: "", "UTF-8"
            )
            val calleeAvatar = URLDecoder.decode(
                backStackEntry.arguments?.getString("calleeAvatar") ?: "", "UTF-8"
            ).takeIf { it.isNotBlank() }

            val state = callViewModel.uiState.collectAsState().value

            when (state) {
                is CallUiState.Initializing -> {
                    // Màn hình loading khi đang kết nối API để tạo cuộc gọi
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1A1A2E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Đang kết nối...", color = Color.White)
                        }
                    }
                }
                is CallUiState.Calling -> {
                    // Khi đã có Call object hợp lệ, RingingCallContent sẽ hiển thị
                    OutgoingCallScreen(
                        call = state.call,
                        calleeName = calleeName,
                        calleeAvatar = calleeAvatar,
                        onCallEnded = {
                            callViewModel.endCall()
                            navController.popBackStack()
                        }
                    )
                }
                is CallUiState.Error -> {
                    // Báo lỗi nếu không tạo được cuộc gọi
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1A1A2E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Lỗi: ${(state as CallUiState.Error).message}", color = Color(0xFFE53935))
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { navController.popBackStack() }) { 
                                Text("Quay lại") 
                            }
                        }
                    }
                }
                else -> {
                    // Fallback an toàn thay cho white screen
                    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A2E)))
                }
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
            val state = callViewModel.uiState.collectAsState().value
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