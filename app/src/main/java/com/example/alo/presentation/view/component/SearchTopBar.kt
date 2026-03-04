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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.alo.presentation.viewmodel.ContactViewModel


@Composable
fun SearchTopBar(
    active: Boolean,
    onActiveChange: (Boolean) -> Unit,
    onNavigateToChat: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }

    val focusRequester = remember { FocusRequester() }

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
    LaunchedEffect(state.navigateToConversationId) {
        state.navigateToConversationId?.let { convId ->
            onActiveChange(false)
            onNavigateToChat(convId)
            viewModel.resetNavigation()
        }
    }

    // ==========================================
    // 1. THANH TÌM KIẾM "GIẢ" (KHI CHƯA ACTIVE)
    // ==========================================
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onActiveChange(true)
            }
    ) {
        OutlinedTextField(
            value = "",
            onValueChange = {},
            placeholder = { Text("Tìm kiếm bạn bè...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Tìm kiếm") },
            enabled = false,
            singleLine = true,
            shape = RoundedCornerShape(percent = 50),
            colors = OutlinedTextFieldDefaults.colors(
                disabledBorderColor = Color.Transparent,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }

    // ==========================================
    // 2. MÀN HÌNH TÌM KIẾM "THẬT" (FULL SCREEN DIALOG)
    // ==========================================
    if (active) {
        Dialog(
            onDismissRequest = {
                onActiveChange(false)
                query = ""
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .systemBarsPadding()
                    .imePadding()
            ) {
                // THANH TÌM KIẾM ĐANG HOẠT ĐỘNG
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        viewModel.searchUsers(it)
                    },
                    placeholder = { Text("Tìm kiếm...") },
                    leadingIcon = {
                        IconButton(onClick = {
                            onActiveChange(false)
                            query = ""
                        }) { Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại") }
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Xóa")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(percent = 50),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6C63FF),
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .focusRequester(focusRequester)
                )

                // VÙNG HIỂN THỊ KẾT QUẢ
                Box(modifier = Modifier.fillMaxSize()) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = 32.dp),
                            color = Color(0xFF6C63FF)
                        )
                    } else if (state.searchResults.isEmpty() && query.isNotEmpty()) {
                        Text(
                            text = "Không tìm thấy người dùng nào",
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = 32.dp),
                            color = Color.Gray
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            items(state.searchResults, key = { it.user.id }) { result ->
                                UserSearchResultItem(
                                    result = result,
                                    onAddFriendClick = { viewModel.sendFriendRequest(result.user.id) },
                                    onAcceptFriendClick = { viewModel.acceptFriendRequest(result.user.id) },
                                    onMessageClick = {

                                        viewModel.getOrCreateChatAndNavigate(result.user.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserSearchResultItem(
    result: UserSearchResult,
    onAddFriendClick: () -> Unit,
    onAcceptFriendClick: () -> Unit,
    onMessageClick: () -> Unit
) {
    val user = result.user
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onMessageClick() }
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
                    onClick = onAcceptFriendClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Chấp nhận", fontSize = 12.sp)
                }
            }
            "friends" -> {
                IconButton(onClick = onMessageClick) {
                    Icon(Icons.Default.Message, contentDescription = "Nhắn tin", tint = Color(0xFF6C63FF))
                }
            }
        }
    }
}