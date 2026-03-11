package com.example.alo.presentation.view.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.alo.domain.model.Message
import com.example.alo.presentation.view.component.TypingIndicatorBubble
import com.example.alo.presentation.view.utils.formatMessageTime
import com.example.alo.presentation.view.utils.formatRelativeTime
import com.example.alo.presentation.view.utils.formatTimeHeader
import com.example.alo.presentation.view.utils.getUserStatus
import com.example.alo.presentation.view.utils.shouldShowTimeHeader
import com.example.alo.presentation.viewmodel.ChatRoomViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    navController: NavController,
    viewModel: ChatRoomViewModel = hiltViewModel(),
) {
    val partnerId by viewModel.partnerId.collectAsState()
    val partnerLastSeen by viewModel.partnerLastSeen.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val messageText by viewModel.messageText.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()

    val partnerName by viewModel.partnerName.collectAsState()
    val partnerAvatar by viewModel.partnerAvatar.collectAsState()

    val listState = rememberLazyListState()
    var activeReactionMessageId by remember { mutableStateOf<String?>(null) }
    var activeDetailsMessageId by remember { mutableStateOf<String?>(null) }
    val isPartnerTyping by viewModel.isPartnerTyping.collectAsState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }
    LaunchedEffect(isPartnerTyping) {
        if (isPartnerTyping) {
            listState.animateScrollToItem(0)
        }
    }
    val userStatus = getUserStatus(partnerLastSeen)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(Color.LightGray),
                                contentAlignment = Alignment.Center
                            ) {
                                if (partnerAvatar.isNotEmpty()) {
                                    AsyncImage(
                                        model = partnerAvatar,
                                        contentDescription = "Avatar",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        text = partnerName.firstOrNull()?.uppercase() ?: "?",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            // Vẽ chấm xanh đè lên nếu online
                            if (userStatus.isOnline) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .align(Alignment.BottomEnd)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4CAF50))
                                        .border(2.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        // Tên người dùng
                        Column(verticalArrangement = Arrangement.Center) {
                            Text(
                                text = partnerName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 1
                            )

                            Text(
                                text = userStatus.statusText,
                                fontSize = 12.sp,
                                color = if (userStatus.isOnline) Color(0xFF4CAF50) else Color.Gray,
                                maxLines = 1
                            )
                        }
                    }

                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                actions = {
                    Row() {
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Call",
                                tint = Color(0xFF6C63FF)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "Video Call",
                                tint = Color(0xFF6C63FF)
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            ChatBottomBar(
                text = messageText,
                onTextChange = { viewModel.onMessageTextChanged(it) },
                onSend = { viewModel.sendMessage() }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .padding(bottom = 22.dp)
            ,
            reverseLayout = true,
        ) {
            if (isPartnerTyping) {
                item {
                    TypingIndicatorBubble(partnerAvatar = partnerAvatar, partnerName = partnerName)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            items(
                count = messages.size,
                key = { index -> messages[index].id }
            ) { index ->

                val message = messages[index]
                val isMine = message.senderId == currentUserId

                // --- XỬ LÝ LOGIC GOM NHÓM Ở ĐÂY ---
                val previousMessage = if (index < messages.size - 1) messages[index + 1] else null

                val nextMessage = if (index > 0) messages[index - 1] else null

                val showTimeHeader = shouldShowTimeHeader(
                    currentMessageTime = message.createdAt,
                    previousMessageTime = previousMessage?.createdAt
                )

                // avatar tin nhắn CUỐI CÙNG của một cụm người gửi liên tiếp
                val isLastInGroup = nextMessage?.senderId != message.senderId

                // tin nhắn CUỐI CÙNG của một cụm
                val showSmallTime = nextMessage?.senderId != message.senderId

                Column {
                    // Hiển thị Header thời gian nếu cần (nằm giữa màn hình)
                    if (showTimeHeader) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = formatTimeHeader(message.createdAt),
                                fontSize = 12.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .background(Color(0xFFF0F0F0), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }

                    MessageBubble(
                        message = message,
                        isMine = isMine,
                        partnerAvatar = partnerAvatar,
                        partnerName = partnerName,
                        showAvatar = isLastInGroup,
                        showTime = showSmallTime,
                        showReactionBar = activeReactionMessageId == message.id,
                        showDetails = activeDetailsMessageId == message.id,
                        onMessageClick = {
                            activeReactionMessageId = null
                            activeDetailsMessageId = if (activeDetailsMessageId == message.id) null else message.id
                        },
                        onMessageLongClick = {
                            activeDetailsMessageId = null
                            activeReactionMessageId = if (activeReactionMessageId == message.id) null else message.id
                        },

                        onReactionSelect = { emoji ->
                            viewModel.addReaction(message.id, emoji)
                            activeReactionMessageId = null
                        }
                    )

                    Spacer(modifier = Modifier.height(if (isLastInGroup) 16.dp else 4.dp))
                }
            }

        }
    }
}

@Composable
fun MessageBubble(
    message: Message,
    isMine: Boolean,
    partnerAvatar: String,
    partnerName: String,
    showAvatar: Boolean,
    showTime: Boolean,
    showDetails: Boolean,
    showReactionBar: Boolean,
    onMessageClick: () -> Unit,
    onMessageLongClick: () -> Unit,
    onReactionSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMine) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (showAvatar) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                if (showAvatar) {
                    if (partnerAvatar.isNotEmpty()) {
                        AsyncImage(
                            model = partnerAvatar,
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = partnerName.firstOrNull()?.uppercase() ?: "?",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
        ) {
            // 1. VẼ THANH CẢM XÚC (TRƯỢT TỪ TRÊN XUỐNG)
            AnimatedVisibility(
                visible = showReactionBar,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 20 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { 20 })
            ) {
                ReactionBar(onReactionSelected = onReactionSelect)
            }

            // Khai báo biến để check cho gọn và tái sử dụng
            val hasReactions = message.reactions.isNotEmpty()

            Box(
                modifier = Modifier.padding(bottom = if (hasReactions) 14.dp else 0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isMine || !showAvatar) 16.dp else 4.dp,
                                bottomEnd = if (!isMine || !showAvatar) 16.dp else 4.dp
                            )
                        )
                        .background(if (isMine) Color(0xFFE5EFFF) else MaterialTheme.colorScheme.surfaceVariant)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { onMessageClick() },
                                onLongPress = { onMessageLongClick() }
                            )
                        }
                        .defaultMinSize(minWidth = if (hasReactions) 70.dp else 0.dp)
                        .padding(
                            start = 14.dp,
                            end = 14.dp,
                            top = 10.dp,
                            bottom = if (hasReactions) 16.dp else 10.dp
                        )
                        .widthIn(max = 260.dp)
                ) {
                    Text(
                        text = message.encryptedContent,
                        color = if (isMine) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp
                    )
                }

                // --- THANH CẢM XÚC ---
                if (hasReactions) {
                    val reactionCounts = message.reactions
                        .groupBy { it.reactionIcon }
                        .mapValues { entry -> entry.value.sumOf { it.count } }

                    val sortedIcons = reactionCounts.entries.sortedByDescending { it.value }.map { it.key }
                    val displayIcons = sortedIcons.take(2)
                    val totalReactions = reactionCounts.values.sum()

                    Row(
                        modifier = Modifier
                            .align(if (isMine) Alignment.BottomStart else Alignment.BottomEnd)
                            .offset(x = if (isMine) 10.dp else (-10).dp, y = 12.dp)
                            .background(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                            .clickable { /* TODO: Mở danh sách chi tiết */ },
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        displayIcons.forEach { icon ->
                            Text(text = icon, fontSize = 12.sp, modifier = Modifier.offset(y = (-1).dp))
                        }

                        if (totalReactions > 1) {
                            Text(
                                text = totalReactions.toString(),
                                fontSize = 11.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 2.dp)
                            )
                        }
                    }
                }
            }


            AnimatedVisibility(
                visible = showDetails,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -10 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -10 })
            ) {
                Row(
                    modifier = Modifier.padding(top = 4.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Giờ gửi chi tiết
                    Text(
                        text = formatMessageTime(message.createdAt),
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    // Kiểm tra trạng thái đã xem (Chỉ áp dụng cho tin nhắn mình gửi)
                    if (isMine) {
                        Spacer(modifier = Modifier.width(8.dp))

                        if (message.seenBy.isNotEmpty()) {

                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(Color.LightGray),
                                contentAlignment = Alignment.Center
                            ) {
                                if (partnerAvatar.isNotEmpty()) {
                                    AsyncImage(
                                        model = partnerAvatar,
                                        contentDescription = "Seen Avatar",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        text = partnerName.firstOrNull()?.uppercase() ?: "?",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        } else {
                            Text(text = "Đã nhận", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }

            if (showTime && !showReactionBar) {
                Text(
                    text = formatMessageTime(message.createdAt),
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
                )
            }
        }
    }
}

@Composable
fun ChatBottomBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 4.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .imePadding()
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                placeholder = { Text("Nhập tin nhắn...") },
                shape = RoundedCornerShape(24.dp),
                maxLines = 4
            )
            IconButton(
                onClick = onSend,
                enabled = text.trim().isNotEmpty(),
                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Gửi")
            }
        }
    }
}
@Composable
fun ReactionBar(
    onReactionSelected: (String) -> Unit
) {
    val reactions = listOf("❤️", "👍", "😆", "😮", "😢", "😡")

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp, // Đổ bóng nhẹ cho thanh cảm xúc nổi lên
        modifier = Modifier.padding(bottom = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            reactions.forEach { emoji ->
                Text(
                    text = emoji,
                    fontSize = 24.sp,
                    modifier = Modifier.clickable { onReactionSelected(emoji) }
                )
            }
        }
    }
}