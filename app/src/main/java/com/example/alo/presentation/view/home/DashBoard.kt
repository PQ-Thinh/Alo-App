package com.example.alo.presentation.view.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.alo.presentation.helper.UserProfileState
import com.example.alo.presentation.viewmodel.UserViewModel

@Composable
fun DashBoard(
    navController: NavController,
    userId: String,
    userViewModel: UserViewModel = hiltViewModel()
) {
    val userState by userViewModel.userState.collectAsState()

    LaunchedEffect(key1 = userId) {
        userViewModel.fetchUserProfile(userId)
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (userState) {
            is UserProfileState.Idle, is UserProfileState.Loading -> {
                CircularProgressIndicator()
            }
            is UserProfileState.Success -> {
                val user = (userState as UserProfileState.Success).user
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Xin chào, ${user.displayName}!",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Email: ${user.email}")
                    Text(text = "Username: @${user.username}")
                    Text(text = "Bio: ${user.bio ?: "Chưa cập nhật"}")
                }
            }
            is UserProfileState.Error -> {
                Text(
                    text = (userState as UserProfileState.Error).message,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}