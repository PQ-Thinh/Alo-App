package com.example.alo.presentation.view.auth

import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    navController: NavController,
    viewModel: SupabaseAuthViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("") }
    val userState by viewModel.userState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(userState) {
        when (userState) {
            is UserState.PasswordResetOtpSent -> {
                Toast.makeText(context, "Đã gửi mã xác nhận!", Toast.LENGTH_SHORT).show()
                navController.navigate(Screen.ResetPasswordOtp.createRoute(email))
            }
            is UserState.Error -> {
                Toast.makeText(context, (userState as UserState.Error).message, Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Quên Mật Khẩu \uD83D\uDD12", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(12.dp))
            Text("Nhập email bạn đã đăng ký, chúng tôi sẽ gửi mã gồm 6 chữ số để đặt lại mật khẩu.", color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(modifier = Modifier.height(48.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email của bạn") },
                leadingIcon = { Icon(Icons.Default.Email, null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        viewModel.sendPasswordResetEmail(email)
                    } else {
                        Toast.makeText(context, "Email không hợp lệ!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF))
            ) {
                if (userState is UserState.Loading) CircularProgressIndicator(color = Color.White)
                else Text("Gửi mã xác nhận", fontWeight = FontWeight.Bold)
            }
        }
    }
}