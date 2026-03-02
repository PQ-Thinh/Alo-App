package com.example.alo.presentation.view.component

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
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
import com.example.alo.presentation.helper.UserSearchResult
import com.example.alo.presentation.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTopBar(
    active: Boolean,
    onActiveChange: (Boolean) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    var query by remember { mutableStateOf("") }


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

    SearchBar(
        query = query,
        onQueryChange = {
            query = it
            viewModel.searchUsers(it)
        },
        onSearch = { viewModel.searchUsers(it) },
        active = active,
        onActiveChange = { onActiveChange(it) },
        placeholder = { Text("Tìm kiếm...") },
        leadingIcon = {
            if (active) {
                IconButton(onClick = {
                    onActiveChange(false)
                    query = ""
                }) { Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại") }
            } else {
                Icon(Icons.Default.Search, contentDescription = "Tìm kiếm")
            }
        },
        trailingIcon = {
            if (active && query.isNotEmpty()) {
                IconButton(onClick = { query = "" }) {
                    Icon(Icons.Default.Close, contentDescription = "Xóa")
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (active) 0.dp else 16.dp, vertical = 8.dp)
    ) {
        // --- VÙNG HIỂN THỊ KẾT QUẢ TÌM KIẾM ---
        Box(modifier = Modifier.fillMaxSize()) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 32.dp),
                    color = Color(0xFF6C63FF)
                )
            } else if (state.searchResults.isEmpty() && query.isNotEmpty() && !state.isLoading) {
                Text(
                    text = "Không tìm thấy người dùng nào",
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 32.dp),
                    color = Color.Gray
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(state.searchResults, key = { it.user.id }) { result ->
                        UserSearchResultItem(
                            result = result,
                            onAddFriendClick = { viewModel.sendFriendRequest(result.user.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UserSearchResultItem(
    result: UserSearchResult,
    onAddFriendClick: () -> Unit
) {
    val user = result.user
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Avatar
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (!user.avatarUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = user.avatarUrl,
                    contentDescription = "Avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = user.displayName.firstOrNull()?.uppercase() ?: "?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // 2. Tên và Username
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
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 3. Nút hành động dựa trên Trạng thái quan hệ (RelationStatus)
        when (result.relationStatus) {
            "none" -> {
                Button(
                    onClick = onAddFriendClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Kết bạn", fontSize = 14.sp)
                }
            }
            "request_sent" -> {
                OutlinedButton(
                    onClick = { /* Bỏ qua, hoặc xử lý Hủy lời mời sau */ },
                    enabled = false
                ) {
                    Text("Đã gửi lời mời", fontSize = 12.sp)
                }
            }
            "request_received" -> {
                Button(
                    onClick = { /* Xử lý chấp nhận kết bạn sau */ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)) // Màu xanh lá
                ) {
                    Text("Chấp nhận", fontSize = 12.sp)
                }
            }
            "friends" -> {
                IconButton(onClick = { /* Chuyển sang màn hình chat 1-1 */ }) {
                    Icon(Icons.Default.Message, contentDescription = "Nhắn tin", tint = Color(0xFF6C63FF))
                }
            }
        }
    }
}