package com.example.alo.presentation.view.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.alo.presentation.helper.UserProfileState
import com.example.alo.presentation.viewmodel.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    userViewModel: UserViewModel
) {
    val profileState by userViewModel.profileState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (profileState is UserProfileState.Idle) {
            userViewModel.fetchCurrentUserProfile()
        }
    }
    if (profileState !is UserProfileState.Success) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val user = (profileState as UserProfileState.Success).user
    val isGoogleAccount = user.avatarId == "google_oauth_avatar"

    var username by remember { mutableStateOf(user.username) }
    var displayName by remember { mutableStateOf(user.displayName) }
    var bio by remember { mutableStateOf(user.bio ?: "") }
    var phone by remember { mutableStateOf(user.phone ?: "") }
    var birthday by remember { mutableStateOf(user.birthday ?: "") }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedAvatarBytes by remember { mutableStateOf<ByteArray?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        uri?.let {
            val bytes = context.contentResolver.openInputStream(it)?.readBytes()
            selectedAvatarBytes = bytes
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chỉnh sửa hồ sơ", fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Trở về")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        userViewModel.updateUserProfile(
                            displayName = displayName,
                            bio = bio,
                            phone = phone,
                            birthday = birthday,
                            newUsername = username,
                            newAvatarBytes = selectedAvatarBytes
                        )
                        navController.popBackStack()
                    }) {
                        Text("Lưu", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- 1. ẢNH ĐẠI DIỆN ---
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
                    .clickable(enabled = !isGoogleAccount) {
                        imagePicker.launch("image/*")
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = selectedImageUri ?: user.avatarUrl,
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                if (!isGoogleAccount) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.Edit, contentDescription = "Sửa", tint = Color.White) }
                }
            }

            if (isGoogleAccount) {
                Text(
                    text = "Tài khoản Google không thể đổi ảnh đại diện",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            // --- 2. CÁC TRƯỜNG DỮ LIỆU ---
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                enabled = !isGoogleAccount, // Khóa nếu là tk Google
                modifier = Modifier.fillMaxWidth()
            )
            if (isGoogleAccount) {
                Text(
                    text = "Tài khoản Google không thể đổi Username",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, top = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Tên hiển thị") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Tiểu sử") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Số điện thoại") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}