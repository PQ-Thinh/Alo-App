package com.example.alo.presentation.view.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.example.alo.presentation.helper.UserProfileState
import com.example.alo.presentation.theme.AppBackgroundColor
import com.example.alo.presentation.theme.CardBackgroundColor
import com.example.alo.presentation.theme.ErrorColor
import com.example.alo.presentation.theme.TextPrimaryColor
import com.example.alo.presentation.theme.TextSecondaryColor
import com.example.alo.presentation.viewmodel.AuthViewModel
import com.example.alo.presentation.viewmodel.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: UserViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    onNavigateToProfile: (String) -> Unit,
    onLogoutSuccess: () -> Unit
) {
    val profileState by viewModel.profileState.collectAsState()
    val primaryColor = Color(0xFF6C63FF)

    LaunchedEffect(Unit) {
        viewModel.fetchCurrentUserProfile()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hồ sơ của tôi", fontWeight = FontWeight.Bold, color = TextPrimaryColor) },
                actions = {
                    IconButton(onClick = {
                        authViewModel.logout()
                        onLogoutSuccess()
                    }) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Đăng xuất",
                            tint = ErrorColor // Dùng màu lỗi tĩnh thay vì theo Theme
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppBackgroundColor // Màu nền App xám nhạt
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackgroundColor) // Nền tổng thể xám nhạt
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            when (val state = profileState) {
                is UserProfileState.Idle, is UserProfileState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = primaryColor)
                    }
                }
                is UserProfileState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message, color = ErrorColor)
                    }
                }
                is UserProfileState.Success -> {
                    val user = state.user
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // AVATAR
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .border(3.dp, primaryColor.copy(alpha = 0.3f), CircleShape)
                                .padding(4.dp)
                        ) {
                            if (user.avatarUrl != null) {
                                AsyncImage(
                                    model = user.avatarUrl,
                                    contentDescription = "Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(Color(0xFFE8EAF6)), // Nền mặc định avatar
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, null, modifier = Modifier.size(50.dp), tint = primaryColor)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = user.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryColor
                        )
                        Text(
                            text = "@${user.username}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryColor
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Nút Edit Profile Full-width ở giữa
                        Button(
                            onClick = { onNavigateToProfile(user.id) },
                            modifier = Modifier.fillMaxWidth(0.7f),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Chỉnh sửa hồ sơ", fontWeight = FontWeight.SemiBold, color = Color.White)
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // THÔNG TIN CÁ NHÂN (Hiệu ứng Đảo nổi)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp), clip = false)
                                .clip(RoundedCornerShape(16.dp))
                                .background(CardBackgroundColor) // Thẻ màu trắng
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                ProfileInfoItem(icon = Icons.Rounded.Email, label = "Email", value = user.email)
                                HorizontalDivider(color = AppBackgroundColor, thickness = 1.dp) // Dùng màu xám nền làm đường kẻ siêu nhạt
                                ProfileInfoItem(icon = Icons.Rounded.Phone, label = "Số điện thoại", value = user.phone?.takeIf { it.isNotBlank() } ?: "Chưa cập nhật")
                                HorizontalDivider(color = AppBackgroundColor, thickness = 1.dp)
                                ProfileInfoItem(icon = Icons.Rounded.DateRange, label = "Ngày sinh", value = user.birthday?.takeIf { it.isNotBlank() } ?: "Chưa cập nhật")
                                HorizontalDivider(color = AppBackgroundColor, thickness = 1.dp)

                                val genderText = when (user.gender) {
                                    true -> "Nam"
                                    false -> "Nữ"
                                    else -> "Chưa cập nhật"
                                }
                                ProfileInfoItem(icon = Icons.Rounded.Person, label = "Giới tính", value = genderText)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // TIỂU SỬ (BIO) (Hiệu ứng Đảo nổi)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp), clip = false)
                                .clip(RoundedCornerShape(16.dp))
                                .background(CardBackgroundColor) // Thẻ màu trắng
                        ) {
                            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Info, contentDescription = null, tint = primaryColor, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "Tiểu sử", fontWeight = FontWeight.Bold, color = TextPrimaryColor)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = user.bio?.takeIf { it.isNotBlank() } ?: "Người dùng này chưa có tiểu sử.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (user.bio.isNullOrBlank()) TextSecondaryColor else TextPrimaryColor,
                                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2f
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileInfoItem(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Box (Kiểu dáng hiện đại)
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFF6C63FF).copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF6C63FF),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondaryColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = TextPrimaryColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}