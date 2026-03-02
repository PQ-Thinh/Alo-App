package com.example.alo.presentation.view.chat

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.alo.presentation.viewmodel.ContactViewModel

@Composable
fun Contact(
    viewModel: ContactViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // Lắng nghe để hiển thị Toast thông báo
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Tiêu đề danh sách lời mời
        Text(
            text = "Lời mời kết bạn (${state.pendingRequests.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
        )

        // Hiển thị trạng thái
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF6C63FF))
            }
        } else if (state.pendingRequests.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(text = "Bạn không có lời mời kết bạn nào mới.", color = Color.Gray)
            }
        } else {
            // Hiển thị danh sách lời mời
            LazyColumn(
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(state.pendingRequests, key = { it.id }) { user ->
                    RequestItem(
                        user = user,
                        onAccept = { viewModel.acceptRequest(user.id) },
                        onDecline = { viewModel.declineRequest(user.id) }
                    )
                }
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Avatar
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // 2. Thông tin (Tên + Username)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.displayName,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "@${user.username}",
                fontSize = 14.sp,
                color = Color.Gray,
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
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Từ chối", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}