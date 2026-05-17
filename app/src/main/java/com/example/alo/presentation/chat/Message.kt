package com.example.alo.presentation.chat

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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
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
import com.example.alo.presentation.theme.AppBackgroundColor
import com.example.alo.presentation.utils.formatRelativeTime
import com.example.alo.presentation.utils.getUserStatus
import com.example.alo.presentation.chat.ChatListViewModel
import com.example.alo.presentation.theme.CardBackgroundColor
import com.example.alo.presentation.theme.ErrorColor
import com.example.alo.presentation.theme.TextPrimaryColor
import com.example.alo.presentation.theme.TextSecondaryColor
import com.example.alo.presentation.theme.primaryColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive


@Composable
fun Message(
    viewModel: ChatListViewModel = hiltViewModel(),
    onNavigateToChatRoom: (String) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    var selectedFilter by remember { mutableStateOf("Tất cả") }
    val filteredList = remember(state.chatList, selectedFilter) {
        if (selectedFilter == "Nhóm") {
            state.chatList.filter { it.isGroup }
        } else {
            state.chatList
        }
    }

    var currentTimeTrigger by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(30_000L)
            currentTimeTrigger = System.currentTimeMillis()
        }
    }
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.fetchChatList(isSilentRefresh = true)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(AppBackgroundColor)) {
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
                        tint = ErrorColor,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = state.error ?: "Đã xảy ra lỗi không xác định",
                        color = ErrorColor,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.fetchChatList(isSilentRefresh = true) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF))
                    ) {
                        Text("Thử lại", color = Color.White)
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
                            .background(Color(0xFFE8EAF6), CircleShape),
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
                        fontSize = 18.sp,
                        color = TextPrimaryColor,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Hãy tìm kiếm bạn bè và bắt đầu trò chuyện ngay nhé!",
                        fontSize = 14.sp,
                        color = TextSecondaryColor,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { /* TODO */ },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.PersonSearch, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tìm bạn bè", color = Color.White)
                    }
                }
            }

            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    val recentActiveUsers = remember(state.chatList, currentTimeTrigger) {
                        state.chatList
                            .sortedByDescending { it.targetLastSeen }
                            .take(15)
                    }

                    if (recentActiveUsers.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Liên hệ gần đây",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondaryColor,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .shadow(elevation = 3.dp, shape = RoundedCornerShape(20.dp)),
                            shape = RoundedCornerShape(20.dp),
                            color = CardBackgroundColor
                        ) {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(recentActiveUsers, key = { it.conversationId }) { chat ->
                                    val userStatus = remember(chat.targetLastSeen, currentTimeTrigger) {
                                        getUserStatus(chat.targetLastSeen)
                                    }
                                    ChatOnlineItem(
                                        chat = chat,
                                        onClick = { onNavigateToChatRoom(chat.conversationId) },
                                        userStatus = userStatus
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tin nhắn",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimaryColor
                        )
                        
                        // Filter Chips
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Tất cả", "Nhóm").forEach { filter ->
                                val isSelected = selectedFilter == filter
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) Color(0xFF6C63FF) else Color(0xFFE8EAF6))
                                        .clickable { selectedFilter = filter }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = filter,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else TextSecondaryColor
                                    )
                                }
                            }
                        }
                    }

                    // Danh sách Chat chính
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 16.dp) 
                    ) {
                        items(filteredList, key = { it.conversationId }) { chat ->
                            val userStatus = getUserStatus(chat.targetLastSeen)
                            ChatItem(
                                chat = chat,
                                onClick = { onNavigateToChatRoom(chat.conversationId) },
                                userStatus = userStatus,
                                currentTimeTrigger = currentTimeTrigger
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
    userStatus: UserStatus,
    currentTimeTrigger: Long
) {
    val hasUnread = chat.unreadCount > 0
    val backgroundColor = if (hasUnread) Color(0xFFF4F3FF) else CardBackgroundColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(60.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color(0xFFE8EAF6)),
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
                        color = Color(0xFF6C63FF)
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
                        .border(2.dp, backgroundColor, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = chat.chatName ?: "Người dùng ẩn danh",
                    fontWeight = if (hasUnread) FontWeight.ExtraBold else FontWeight.SemiBold,
                    fontSize = 17.sp,
                    color = TextPrimaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                
                if (!chat.isGroup) {
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    if (userStatus.isOnline) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(Color(0xFF4CAF50), CircleShape)
                        )
                    } else {
                        val shortTime = userStatus.statusText
                            .replace("Hoạt động ", "")
                            .replace(" phút trước", "p")
                            .replace(" giờ trước", "h")
                            .replace(" ngày trước", "d")
                            .replace("Vừa mới truy cập", "1p")
                            
                        Text(
                            text = shortTime,
                            fontSize = 11.sp,
                            color = TextSecondaryColor.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (!chat.status.isNullOrEmpty()) {
                    // HIỂN THỊ STATUS NHÓM Ở NGOÀI DANH SÁCH
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = primaryColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = chat.status,
                            fontSize = 10.sp,
                            color = primaryColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            val prefix = if (chat.lastMessageSenderId == chat.currentUserId) "Bạn: " else ""
            val previewText = chat.lastMessagePreview ?: "Bắt đầu trò chuyện..."
            Text(
                text = "$prefix$previewText",
                fontSize = 14.sp,
                fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal,
                color = if (hasUnread) TextPrimaryColor else TextSecondaryColor,
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
                color = if (hasUnread) Color(0xFF6C63FF) else TextSecondaryColor,
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
            .width(64.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        // Khung chứa Avatar và Badge trạng thái
        Box(modifier = Modifier.size(60.dp)) {

            // 1. Ảnh Avatar
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color(0xFFE8EAF6)),
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
                        color = Color(0xFF6C63FF)
                    )
                }
            }

            Box(modifier = Modifier.align(Alignment.BottomEnd)) {
                if (userStatus.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                            .border(2.dp, CardBackgroundColor, CircleShape)
                    )
                } else {
                    val shortTimeText = userStatus.statusText
                        .replace("Hoạt động ", "")
                        .replace(" phút trước", " phút")
                        .replace(" giờ trước", " giờ")
                        .replace(" ngày trước", " ngày")
                        .replace("Vừa mới truy cập", "1p")

                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 16.dp)
                            .height(16.dp)
                            .border(1.5.dp, CardBackgroundColor, CircleShape)
                            .background(color = Color(0xFFF5F5F5), shape = CircleShape)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center // Giữ nguyên cái này ở Box
                    ) {
                        Text(
                            text = shortTimeText,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF9E9E9E),
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                            style = TextStyle(
                                // Gọt bỏ hoàn toàn lớp lót đệm tàng hình của Font Android
                                platformStyle = PlatformTextStyle(
                                    includeFontPadding = false
                                ),
                                // Ép dòng chữ phải nằm chính giữa trục dọc của khung Text
                                lineHeightStyle = LineHeightStyle(
                                    alignment = LineHeightStyle.Alignment.Center,
                                    trim = LineHeightStyle.Trim.Both
                                )
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 3. Tên người dùng (Luôn cố định 1 dòng)
        val shortName = chat.chatName?.split(" ")?.lastOrNull() ?: "?"
        Text(
            text = shortName,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimaryColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
