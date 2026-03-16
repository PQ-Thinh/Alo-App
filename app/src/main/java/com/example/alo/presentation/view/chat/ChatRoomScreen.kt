package com.example.alo.presentation.view.chat


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.alo.domain.model.Message
import com.example.alo.presentation.view.components.TypingIndicatorBubble
import com.example.alo.presentation.view.utils.formatTimeHeader
import com.example.alo.presentation.view.utils.getUserStatus
import com.example.alo.presentation.view.utils.shouldShowTimeHeader
import com.example.alo.presentation.viewmodel.ChatRoomViewModel
import com.example.alo.presentation.view.components.ChatBottomBar
import com.example.alo.presentation.view.components.MessageActionOverlay
import com.example.alo.presentation.view.components.MessageBubble


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    navController: NavController,
    viewModel: ChatRoomViewModel = hiltViewModel(),
) {
    //val partnerId by viewModel.partnerId.collectAsState()
    val partnerLastSeen by viewModel.partnerLastSeen.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val messageText by viewModel.messageText.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()

    val partnerName by viewModel.partnerName.collectAsState()
    val partnerAvatar by viewModel.partnerAvatar.collectAsState()

    val listState = rememberLazyListState()
    var activeDetailsMessageId by remember { mutableStateOf<String?>(null) }
    val isPartnerTyping by viewModel.isPartnerTyping.collectAsState()

    var selectedMessageForOverlay by remember { mutableStateOf<Message?>(null) }
    val clipboardManager = LocalClipboardManager.current
    var replyingToMessage by remember { mutableStateOf<Message?>(null) }

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
                                        .border(
                                            2.dp,
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            CircleShape
                                        )
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
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.shadow(elevation = 2.dp),
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
                onCancelReply = { replyingToMessage = null },
                onTextChange = { viewModel.onMessageTextChanged(it) },
                onSend = {
                    viewModel.sendMessage(replyToId = replyingToMessage?.id)

                    replyingToMessage = null
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.LightGray)
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                //.padding(bottom = 22.dp)
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

                val repliedMessage = message.replyToId?.let { id ->
                    messages.find { it.id == id }
                }

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
                        repliedMessage = repliedMessage,
                        isMine = isMine,
                        partnerAvatar = partnerAvatar,
                        partnerName = partnerName,
                        showAvatar = isLastInGroup,
                        showTime = showSmallTime,
                        showDetails = activeDetailsMessageId == message.id,

                        onMessageClick = {
                            activeDetailsMessageId = if (activeDetailsMessageId == message.id) null else message.id
                        },
                        onMessageLongClick = {
                            activeDetailsMessageId = null
                            selectedMessageForOverlay = message
                        }
                    )
                    Spacer(modifier = Modifier.height(if (isLastInGroup) 16.dp else 4.dp))
                }
            }

        }
    }
    if (selectedMessageForOverlay != null) {
        MessageActionOverlay(
            message = selectedMessageForOverlay!!,
            isMine = selectedMessageForOverlay!!.senderId == currentUserId,
            onDismiss = { selectedMessageForOverlay = null }, // Bấm ra ngoài để đóng
            onReply = {
                replyingToMessage = selectedMessageForOverlay
                selectedMessageForOverlay = null
            },
            onCopy = {
                clipboardManager.setText(AnnotatedString(selectedMessageForOverlay!!.encryptedContent))
                selectedMessageForOverlay = null
            },
            onReactionSelect = { emoji ->
                viewModel.addReaction(selectedMessageForOverlay!!.id, emoji)
                selectedMessageForOverlay = null
            }
        )
    }
}