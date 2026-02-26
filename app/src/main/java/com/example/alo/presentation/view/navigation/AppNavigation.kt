package com.example.alo.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.alo.presentation.view.auth.LoginScreen
import com.example.alo.presentation.view.profile.ProfileSetupScreen
import com.example.alo.presentation.view.auth.SignUpScreen
import com.example.alo.presentation.view.home.DashboardScreen
import com.example.alo.presentation.view.navigation.Screen
import com.example.alo.presentation.view.profile.ProfileScreen
import com.example.alo.presentation.viewmodel.SplashViewModel

@Composable
fun AppNavigation(splashViewModel: SplashViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val startDestination by splashViewModel.startDestination.collectAsState()

    if (startDestination == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        val initialRoute = if (startDestination == "dashboard") Screen.Dashboard.route else Screen.Login.route

        NavHost(
            navController = navController,
            startDestination = initialRoute
        ) {
            composable(route = Screen.Intro.route) {

            }
            composable(route = Screen.Login.route) {
                 LoginScreen(navController = navController)
            }

            composable(route = Screen.SignUp.route) {
                SignUpScreen(navController = navController)
            }

            composable(
                route = Screen.Dashboard.route,
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                DashboardScreen(navController = navController, userId = userId)
            }

            composable(
                route = Screen.ProfileSetup.route,
                arguments = listOf(
                    navArgument("userId") { type = NavType.StringType },
                    navArgument("email") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                ProfileSetupScreen(
                    navController = navController,
                    onSetupComplete = {
                        navController.navigate(Screen.Dashboard.createRoute(userId)) {
                            popUpTo(Screen.ProfileSetup.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(
                route = Screen.Profile.route,
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                ProfileScreen(navController = navController, userId = userId)
            }
        }

    }
}