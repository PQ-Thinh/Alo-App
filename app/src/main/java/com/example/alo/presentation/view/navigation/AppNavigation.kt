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
import com.example.alo.presentation.view.call.VideoCallScreen
import com.example.alo.presentation.view.chat.ChatRoomScreen
import com.example.alo.presentation.view.home.AnimatedSplashScreen
import com.example.alo.presentation.view.home.DashboardScreen
import com.example.alo.presentation.view.profile.EditProfileScreen
import com.example.alo.presentation.view.profile.ProfileScreen
import com.example.alo.presentation.viewmodel.UserViewModel

@Composable
fun AppNavigation(
    startDestination: String,
    pushConversationId: String?
) {
    val navController = rememberNavController()
    LaunchedEffect(pushConversationId) {
        if (pushConversationId != null) {
            navController.navigate("chat_room_screen/$pushConversationId")
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
        composable(
            route = Screen.VideoCall.route,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: return@composable
            VideoCallScreen(navController = navController, conversationId = conversationId)
        }
    }
}