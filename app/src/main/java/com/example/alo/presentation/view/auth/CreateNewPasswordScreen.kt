package com.example.alo.presentation.view.auth

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.alo.presentation.helper.UserState
import com.example.alo.presentation.view.navigation.Screen
import com.example.alo.presentation.viewmodel.SupabaseAuthViewModel

@Composable
fun CreateNewPasswordScreen(
    navController: NavController,
    viewModel: SupabaseAuthViewModel = hiltViewModel()
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val userState by viewModel.userState.collectAsState()
    val context = LocalContext.current

    // Biểu thức chính quy: >8 ký tự, có Hoa, Thường, Số, Ký tự đặc biệt
    val passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$".toRegex()

    LaunchedEffect(userState) {
        if (userState is UserState.PasswordChangedSuccess) {
            Toast.makeText(context, "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show()
            navController.navigate(Screen.Dashboard.route) {
                popUpTo(0) { inclusive = true }
            }
        } else if (userState is UserState.Error) {
            Toast.makeText(context, (userState as UserState.Error).message, Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Tạo Mật Khẩu Mới", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = { Text("Mật khẩu mới") },
            modifier = Modifier.fillMaxWidth()
        )
        Text("Ít nhất 8 ký tự, gồm chữ hoa, thường, số và ký tự đặc biệt (@,$,!,%,*,?,&).", color = Color.Gray, style = MaterialTheme.typography.bodySmall)

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Xác nhận mật khẩu mới") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(48.dp))

        Button(
            onClick = {
                if (!passwordPattern.matches(newPassword)) {
                    Toast.makeText(context, "Mật khẩu chưa đủ độ mạnh!", Toast.LENGTH_SHORT).show()
                } else if (newPassword != confirmPassword) {
                    Toast.makeText(context, "Mật khẩu xác nhận không khớp!", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.updateNewPassword(newPassword)
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF))
        ) {
            if (userState is UserState.Loading) CircularProgressIndicator(color = Color.White)
            else Text("Hoàn tất", fontWeight = FontWeight.Bold)
        }
    }
}