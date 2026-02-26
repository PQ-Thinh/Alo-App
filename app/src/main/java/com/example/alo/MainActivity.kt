package com.example.alo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.alo.presentation.view.navigation.AppNavigation
import com.example.alo.presentation.theme.AloTheme
import com.example.alo.presentation.viewmodel.SplashViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val splashViewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition {
            splashViewModel.isLoading.value
        }

        setContent {
            AloTheme {
                val startDestination by splashViewModel.startDestination.collectAsState()

                if (startDestination != null) {
                    AppNavigation(startDestination = startDestination!!)
                }
            }
        }
    }
}