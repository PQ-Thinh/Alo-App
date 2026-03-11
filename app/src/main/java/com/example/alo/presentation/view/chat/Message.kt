package com.example.alo.presentation.view.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import coil3.compose.AsyncImage
import com.example.alo.domain.model.ChatList
import com.example.alo.presentation.helper.UserStatus
import com.example.alo.presentation.view.utils.formatRelativeTime
import com.example.alo.presentation.view.utils.getUserStatus
import com.example.alo.presentation.viewmodel.ChatListViewModel

@Composable
fun Message(
    viewModel: ChatListViewModel = hiltViewModel(),
    onNavigateToChatRoom: (String) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.fetchChatList(isSilentRefresh = true)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFF6C63FF)
                )
            }

            state.error != null -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Lỗi",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = state.error ?: "Đã xảy ra lỗi không xác định",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.fetchChatList(isSilentRefresh = true) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF))
                    ) {
                        Text("Thử lại")
                    }
                }
            }

            state.chatList.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .background(Color(0xFFF0F0FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = "Chưa có tin nhắn",
                            tint = Color(0xFF6C63FF),
                            modifier = Modifier.size(60.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Bạn chưa có cuộc trò chuyện nào",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Hãy tìm kiếm bạn bè và bắt đầu trò chuyện ngay nhé!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { /* TODO */ },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.PersonSearch, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tìm bạn bè")
                    }
                }
            }

            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    val onlineUsers = state.chatList.filter { getUserStatus(it.targetLastSeen).isOnline }

                    if (onlineUsers.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Đang hoạt động",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )

                        // Khối Background bo góc cho LazyRow (Modern UI)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ) {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp) // Cân bằng khoảng cách tự động
                            ) {
                                items(onlineUsers, key = { it.conversationId }) { chat ->
                                    ChatOnlineItem(
                                        chat = chat,
                                        onClick = { onNavigateToChatRoom(chat.conversationId) },
                                        userStatus = getUserStatus(chat.targetLastSeen)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Tiêu đề khu vực Message
                    Text(
                        text = "Tin nhắn",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    // Danh sách Chat chính
                    LazyColumn(
                        modifier = Modifier.weight(1f), // Chiếm phần không gian còn lại
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(state.chatList, key = { it.conversationId }) { chat ->
                            val userStatus = getUserStatus(chat.targetLastSeen)
                            ChatItem(
                                chat = chat,
                                onClick = { onNavigateToChatRoom(chat.conversationId) },
                                userStatus = userStatus
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatItem(
    chat: ChatList,
    onClick: () -> Unit,
    userStatus: UserStatus
) {
    val hasUnread = chat.unreadCount > 0
    val backgroundColor = if (hasUnread) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(modifier = Modifier.size(60.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!chat.chatAvatar.isNullOrEmpty()) {
                    AsyncImage(
                        model = chat.chatAvatar,
                        contentDescription = "Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val initial = chat.chatName?.firstOrNull()?.uppercase() ?: "?"
                    Text(
                        text = initial,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (userStatus.isOnline) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                        .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chat.chatName ?: "Người dùng ẩn danh",
                fontWeight = if (hasUnread) FontWeight.ExtraBold else FontWeight.SemiBold,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            val prefix = if (chat.lastMessageSenderId == chat.currentUserId) "Bạn: " else ""
            val previewText = chat.lastMessagePreview ?: "Bắt đầu trò chuyện..."
            Text(
                text = "$prefix$previewText",
                fontSize = 14.sp,
                fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal,
                color = if (hasUnread) MaterialTheme.colorScheme.onBackground else Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.height(50.dp)
        ) {
            Text(
                text = formatRelativeTime(chat.lastMessageTime.toString()),
                fontSize = 12.sp,
                color = if (hasUnread) Color(0xFF6C63FF) else Color.Gray,
                fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (hasUnread) {
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 22.dp)
                        .height(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF6C63FF))
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (chat.unreadCount > 99) "99+" else chat.unreadCount.toString(),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(22.dp))
            }
        }
    }
}

@Composable
fun ChatOnlineItem(
    chat: ChatList,
    onClick: () -> Unit,
    userStatus: UserStatus
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(64.dp) // Cố định chiều rộng để text không làm vỡ layout
            .clickable(onClick = onClick)
    ) {
        // Avatar
        Box(modifier = Modifier.size(60.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!chat.chatAvatar.isNullOrEmpty()) {
                    AsyncImage(
                        model = chat.chatAvatar,
                        contentDescription = "Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val initial = chat.chatName?.firstOrNull()?.uppercase() ?: "?"
                    Text(
                        text = initial,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (userStatus.isOnline) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                        .border(2.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Tên hiển thị (chỉ lấy chữ cuối cùng - Tên thật trong tiếng Việt)
        val shortName = chat.chatName?.split(" ")?.lastOrNull() ?: "?"
        Text(
            text = shortName,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}