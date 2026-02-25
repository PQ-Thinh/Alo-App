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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.alo.presentation.view.auth.LoginScreen
import com.example.alo.presentation.view.auth.SignUpScreen
import com.example.alo.presentation.viewmodel.SplashViewModel
import androidx.navigation.navigation

@Composable
fun AppNavigation(splashViewModel: SplashViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val startDestination by splashViewModel.startDestination.collectAsState()

    if (startDestination == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        // Tự động quyết định Graph nào sẽ được load đầu tiên
        val initialGraph = if (startDestination?.startsWith("dashboard") == true) {
            Graph.Main.route
        } else {
            Graph.Auth.route
        }

        NavHost(
            navController = navController,
            route = Graph.Root.route,
            startDestination = initialGraph
        ) {

            // ==========================================
            // CỤM 1: AUTH GRAPH (CHƯA ĐĂNG NHẬP)
            // ==========================================
            navigation(
                route = Graph.Auth.route,
                startDestination = Screen.Login.route
            ) {
                composable(route = Screen.Intro.route) { /* Intro */ }

                composable(route = Screen.Login.route) {
                    LoginScreen(navController = navController)
                }

                composable(route = Screen.SignUp.route) {
                    SignUpScreen(navController = navController)
                }


            }

        }
    }
}