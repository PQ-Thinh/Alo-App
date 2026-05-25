package com.example.alo.presentation.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.alo.core.utils.Constant
import com.example.alo.presentation.helper.CallUiState
import com.example.alo.presentation.auth.CreateNewPasswordScreen
import com.example.alo.presentation.auth.ForgotPasswordScreen
import com.example.alo.presentation.auth.LoginScreen
import com.example.alo.presentation.auth.OtpVerificationScreen
import com.example.alo.presentation.auth.ResetPasswordOtpScreen
import com.example.alo.presentation.auth.SignUpScreen
import com.example.alo.presentation.call.ActiveCallScreen
import com.example.alo.presentation.call.IncomingCallScreen
import com.example.alo.presentation.call.OutgoingCallScreen
import com.example.alo.presentation.chat.ChatRoomScreen
import com.example.alo.presentation.chat.GroupDetailScreen
import com.example.alo.presentation.chat.GroupMembersScreen
import com.example.alo.presentation.chat.AddMemberScreen
import com.example.alo.presentation.chat.CreateTaskScreen
import com.example.alo.presentation.home.AnimatedSplashScreen
import com.example.alo.presentation.home.CreateGroupScreen
import com.example.alo.presentation.home.DashboardScreen
import com.example.alo.presentation.home.IntroScreen
import com.example.alo.presentation.profile.EditProfileScreen
import com.example.alo.presentation.profile.ProfileScreen
import com.example.alo.presentation.profile.ProfileSetupScreen
import com.example.alo.presentation.auth.AuthViewModel
import com.example.alo.presentation.call.CallViewModel
import com.example.alo.presentation.profile.UserViewModel
import io.getstream.video.android.core.StreamVideo
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
            navController.navigate(Screen.ChatRoom.createRoute(pushConversationId))
        }
    }

    // Navigate to incoming call from FCM push (legacy - giữ lại cho backward compatibility)
    LaunchedEffect(pushCallId, pushCallAction) {
        if (pushCallId != null) {
            try {
                callViewModel.initStreamClient()
                when (pushCallAction) {
                    Constant.ACTION_INCOMING_CALL_ACCEPT -> {
                        callViewModel.acceptCall(pushCallId)
                        navController.navigate(Screen.ActiveCall.createRoute(pushCallId)) {
                            popUpTo(0) { inclusive = false }
                        }
                    }
                    Constant.ACTION_INCOMING_CALL_DECLINE -> {
                        callViewModel.rejectCall(pushCallId)
                    }
                    else -> {
                        navController.navigate(
                            Screen.IncomingCall.createRoute(pushCallId, pushCallerName ?: "Cuộc gọi đến")
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("NAV_DEBUG", "Lỗi xử lý push call: ${e.message}")
            } finally {
                onClearPushDetails()
            }
        }
    }

    // ĐÂY LÀ PHẦN CHÍNH: Lắng nghe ringingCall từ GetStream SDK
    // Khi SDK nhận cuộc gọi đến (qua WebSocket hoặc Push), nó cập nhật state này
    LaunchedEffect(Unit) {
        if (StreamVideo.isInstalled) {
            val currentUserId = StreamVideo.instance().user.id
            StreamVideo.instance().state.ringingCall.collect { ringingCall ->
                if (ringingCall != null) {
                    val callId = ringingCall.id
                    // QUAN TRỌNG: Kiểm tra xem mình có phải người tạo cuộc gọi không
                    // Nếu mình là CALLER → KHÔNG hiện IncomingCall (vì đã ở OutgoingCall rồi)
                    val createdById = ringingCall.state.createdBy.value?.id
                    Log.d("NAV_DEBUG", "ringingCall detected: $callId, createdBy=$createdById, me=$currentUserId")

                    if (createdById != null && createdById == currentUserId) {
                        Log.d("NAV_DEBUG", "Bỏ qua — mình là người gọi, không hiện IncomingCall")
                        return@collect
                    }

                    val currentState = callViewModel.uiState.value
                    if (currentState !is CallUiState.InCall && currentState !is CallUiState.Calling) {
                        val callerName = ringingCall.state.createdBy.value?.name ?: "Cuộc gọi đến"
                        Log.d("NAV_DEBUG", "Navigating to IncomingCall: $callId from $callerName")
                        navController.navigate(
                            Screen.IncomingCall.createRoute(callId, callerName)
                        )
                    }
                }
            }
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
            route = Screen.Profile.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val authViewModel: AuthViewModel = hiltViewModel()

            ProfileScreen(
                userId = userId,
                authViewModel = authViewModel,
                onNavigateToEditProfile = { id ->
                    navController.navigate(Screen.EditProfile.createRoute(id))
                },
                onLogoutSuccess = {
                    navController.navigate(Screen.Intro.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
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

        composable(
            route = Screen.GroupDetail.route,
            arguments = listOf(
                navArgument("conversationId") { type = NavType.StringType }
            )
        ) {
            GroupDetailScreen(navController = navController)
        }

        composable(
            route = Screen.GroupMembers.route,
            arguments = listOf(
                navArgument("conversationId") { type = NavType.StringType }
            )
        ) {
            GroupMembersScreen(navController = navController)
        }

        composable(route = Screen.CreateGroup.route) {
           CreateGroupScreen(navController = navController)
        }

        composable(
            route = Screen.AddMember.route,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
        ) {
            AddMemberScreen(navController = navController)
        }

        composable(
            route = Screen.CreateTask.route,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
        ) {
            CreateTaskScreen(navController = navController)
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
