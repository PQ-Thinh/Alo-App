package com.example.alo.presentation.view.chat

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.example.alo.domain.model.User
import com.example.alo.presentation.helper.UserStatus
import com.example.alo.presentation.theme.AppBackgroundColor
import com.example.alo.presentation.theme.CardBackgroundColor
import com.example.alo.presentation.theme.TextPrimaryColor
import com.example.alo.presentation.theme.TextSecondaryColor
import com.example.alo.presentation.view.utils.getUserStatus
import com.example.alo.presentation.viewmodel.ContactViewModel

@Composable
fun Contact(
    viewModel: ContactViewModel = hiltViewModel(),
    onNavigateToChatRoom: (String) -> Unit
) {

    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.error, state.successMessage) {
        state.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
        state.successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackgroundColor), // Nền xám nhạt để làm nổi bật các thẻ trắng
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // --- PHẦN 1: LỜI MỜI KẾT BẠN ---
            item {
                Text(
                    text = "Lời mời kết bạn (${state.pendingRequests.size})",
                    fontSize = 18.sp,
                    color = TextPrimaryColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                )
            }

            if (state.pendingRequests.isEmpty()) {
                item {
                    Text(
                        text = "Không có lời mời kết bạn mới.",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = TextSecondaryColor,
                        fontSize = 14.sp
                    )
                }
            } else {
                items(state.pendingRequests, key = { "req_${it.id}" }) { user ->
                    RequestItem(
                        user = user,
                        onAccept = { viewModel.acceptRequest(user.id) },
                        onDecline = { viewModel.declineRequest(user.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp)) // Bỏ HorizontalDivider đi để UI thoáng hơn, chỉ dùng khoảng trống
            }

            // --- PHẦN 2: DANH SÁCH BẠN BÈ ---
            item {
                Text(
                    text = "Bạn bè (${state.friends.size})",
                    fontSize = 18.sp,
                    color = TextPrimaryColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (state.friends.isEmpty()) {
                item {
                    Text(
                        text = "Bạn chưa có người bạn nào.",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = TextSecondaryColor,
                        fontSize = 14.sp
                    )
                }
            } else {
                items(state.friends, key = { "friend_${it.id}" }) { friend ->
                    val userStatus = getUserStatus(friend.lastSeen)
                    FriendItem(
                        user = friend,
                        userStatus,
                        onClick = {
                            viewModel.onFriendClicked(friend.id) { conversationId ->
                                onNavigateToChatRoom(conversationId)
                            }
                        }
                    )
                }
            }
        }

        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color(0xFF6C63FF)
            )
        }
    }
}

@Composable
fun FriendItem(
    user: User,
    userStatus: UserStatus,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp) // Margin tạo vùng giãn cách cho thẻ nổi
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                clip = false
            )
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackgroundColor) // Thẻ màu trắng
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp), // Padding bên trong thẻ
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(modifier = Modifier.size(50.dp)) {
            // Avatar chính
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color(0xFFE8EAF6)), // Màu xám nhạt lót avatar mặc định
                contentAlignment = Alignment.Center
            ) {
                if (user.avatarUrl != null) {
                    AsyncImage(
                        model = user.avatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = user.displayName.firstOrNull()?.uppercase() ?: "?",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6C63FF)
                    )
                }
            }

            // Vẽ CHẤM XANH
            if (userStatus.isOnline) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                        .border(2.dp, CardBackgroundColor, CircleShape) // Cắt viền tiệp màu với đảo trắng
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Name & Bio/Username
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.displayName,
                color = TextPrimaryColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = user.bio ?: "@${user.username}",
                fontSize = 13.sp,
                color = TextSecondaryColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row {
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Call",
                    tint = Color(0xFF6C63FF)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = "Video Call",
                    tint = Color(0xFF6C63FF)
                )
            }
        }
    }
}

@Composable
fun RequestItem(
    user: User,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                clip = false
            )
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackgroundColor) // Thẻ màu trắng
            .padding(horizontal = 16.dp, vertical = 12.dp), // Thêm padding trong
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xFFE8EAF6)),
            contentAlignment = Alignment.Center
        ) {
            if (user.avatarUrl != null) {
                AsyncImage(
                    model = user.avatarUrl,
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = user.displayName.firstOrNull()?.uppercase() ?: "?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF6C63FF)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.displayName,
                color = TextPrimaryColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "@${user.username}",
                fontSize = 14.sp,
                color = TextSecondaryColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row {
            IconButton(
                onClick = onAccept,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF6C63FF).copy(alpha = 0.15f), CircleShape)
            ) {
                Icon(Icons.Default.Check, contentDescription = "Chấp nhận", tint = Color(0xFF6C63FF))
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onDecline,
                modifier = Modifier
                    .size(40.dp)
                    .background(AppBackgroundColor, CircleShape) // Dùng màu xám nhạt để nhấn chìm nút Từ chối xuống
            ) {
                Icon(Icons.Default.Close, contentDescription = "Từ chối", tint = TextSecondaryColor)
            }
        }
    }
}