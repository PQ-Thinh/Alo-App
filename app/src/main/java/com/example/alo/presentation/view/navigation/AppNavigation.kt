package com.example.alo.presentation.view.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import com.example.alo.presentation.view.auth.ProfileSetupScreen
import com.example.alo.presentation.view.auth.SignUpScreen
import com.example.alo.presentation.view.home.DashBoard
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
                route = Screen.ProfileSetup.route,
                arguments = listOf(
                    navArgument("userId") { type = NavType.StringType },
                    navArgument("email") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                val email = backStackEntry.arguments?.getString("email") ?: ""

                ProfileSetupScreen(
                    navController = navController,
                    userId = userId,
                    email = email
                )
            }
            composable(
                route = Screen.Dashboard.route,
                arguments = listOf(
                    navArgument("userId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""

                DashBoard(
                    navController = navController,
                    userId = userId
                )
            }
        }
    }
}