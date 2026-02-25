package com.example.alo.presentation.view.auth

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.alo.domain.model.User
import com.example.alo.presentation.helper.UserProfileState
import com.example.alo.presentation.viewmodel.UserViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProfileSetupScreen(
    navController: NavController,
    userId: String,
    email: String,
    userViewModel: UserViewModel = hiltViewModel()
) {
    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }

    val context = LocalContext.current
    val userState by userViewModel.userState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Hoàn thiện hồ sơ", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username (Định danh duy nhất)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text("Tên hiển thị") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text("Tiểu sử ngắn (Bio)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (username.isBlank() || displayName.isBlank()) {
                    Toast.makeText(context, "Vui lòng nhập đủ tên", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                val currentTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date())

                val newUserProfile = User(
                    id = userId,
                    username = username,
                    displayName = displayName,
                    email = email,
                    bio = bio,
                    phone = null,
                    avatarId = "default_avatar",
                    avatarUrl = null,
                    publicKey = "fake_public_key_for_now",
                    createdAt = currentTime,
                    updatedAt = currentTime
                )

                userViewModel.saveUserProfile(newUserProfile) {
                    Toast.makeText(context, "Lưu thành công!", Toast.LENGTH_SHORT).show()
                    navController.navigate("dashboard") {
                        popUpTo("profile_setup") { inclusive = true }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = userState !is UserProfileState.Loading
        ) {
            if (userState is UserProfileState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Lưu và Tiếp tục")
            }
        }
    }
}