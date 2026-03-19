package com.example.alo.presentation.view.chat

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.alo.domain.model.Message
import com.example.alo.presentation.theme.AppBackgroundColor
import com.example.alo.presentation.theme.CardBackgroundColor
import com.example.alo.presentation.theme.TextPrimaryColor
import com.example.alo.presentation.theme.TextSecondaryColor
import com.example.alo.presentation.view.components.ChatBottomBar
import com.example.alo.presentation.view.components.EmptyChatGreeting
import com.example.alo.presentation.view.components.MessageActionOverlay
import com.example.alo.presentation.view.components.MessageBubble
import com.example.alo.presentation.view.components.TypingIndicatorBubble
import com.example.alo.presentation.view.navigation.Screen
import com.example.alo.presentation.view.utils.formatTimeHeader
import com.example.alo.presentation.view.utils.getUserStatus
import com.example.alo.presentation.view.utils.shouldShowTimeHeader
import com.example.alo.presentation.viewmodel.ChatRoomViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    navController: NavController,
    viewModel: ChatRoomViewModel = hiltViewModel(),
) {
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

    val context = LocalContext.current
    val conversationId = viewModel.conversationId

    val isShowingRawEncryption by viewModel.isShowingRawEncryption.collectAsState()

    val isFriend by viewModel.isFriend.collectAsState()

    var currentTimeTrigger by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(30_000L)
            currentTimeTrigger = System.currentTimeMillis()
        }
    }

    // KHỞI TẠO TRÌNH CHỌN ẢNH
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            val inputStream = context.contentResolver.openInputStream(selectedUri)
            val byteArray = inputStream?.readBytes()
            inputStream?.close()

            if (byteArray != null) {
                val fileName = "img_${System.currentTimeMillis()}.jpg"
                var fileSize = 0
                context.contentResolver.query(selectedUri, null, null, null, null)?.use { cursor ->
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    cursor.moveToFirst()
                    if (sizeIndex != -1) fileSize = cursor.getInt(sizeIndex)
                }

                viewModel.sendMediaMessage(
                    conversationId = conversationId,
                    byteArray = byteArray,
                    fileName = fileName,
                    fileSize = fileSize,
                    isImage = true
                )
            }
        }
    }

    // KHỞI TẠO TRÌNH CHỌN FILE TÀI LIỆU
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            var fileName = "document_${System.currentTimeMillis()}"
            var fileSize = 0

            context.contentResolver.query(selectedUri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                cursor.moveToFirst()
                if (nameIndex != -1) fileName = cursor.getString(nameIndex)
                if (sizeIndex != -1) fileSize = cursor.getInt(sizeIndex)
            }

            val inputStream = context.contentResolver.openInputStream(selectedUri)
            val byteArray = inputStream?.readBytes()
            inputStream?.close()

            if (byteArray != null) {
                viewModel.sendMediaMessage(
                    conversationId = conversationId,
                    byteArray = byteArray,
                    fileName = fileName,
                    fileSize = fileSize,
                    isImage = false
                )
            }
        }
    }

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
    val userStatus = remember(partnerLastSeen, currentTimeTrigger) {
        getUserStatus(partnerLastSeen)
    }

    Scaffold(
        containerColor = AppBackgroundColor,
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().background(AppBackgroundColor)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppBackgroundColor)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // --- ĐẢO TRÁI: Nút Back + User Info ---
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                        .shadow(elevation = 4.dp, shape = RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .background(CardBackgroundColor) // Đảo Trắng
                        .clickable { /* TODO: Chuyển đến xem Profile */ }
                        .padding(start = 4.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại", tint = TextPrimaryColor)
                    }
                    Spacer(modifier = Modifier.width(4.dp))

                    // Avatar
                    Box(modifier = Modifier.size(38.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color(0xFFE8EAF6)),
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
                                    color = Color(0xFF6C63FF),
                                    fontSize = 16.sp
                                )
                            }
                        }

                        if (userStatus.isOnline) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .align(Alignment.BottomEnd)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4CAF50))
                                    .border(2.dp, CardBackgroundColor, CircleShape) // Cắt viền với đảo trắng
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Name & Status
                    Column(verticalArrangement = Arrangement.Center) {
                        Text(
                            text = partnerName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimaryColor,
                            maxLines = 1
                        )
                        Text(
                            text = userStatus.statusText,
                            fontSize = 12.sp,
                            color = if (userStatus.isOnline) Color(0xFF4CAF50) else TextSecondaryColor,
                            maxLines = 1
                        )
                    }
                }

                // --- ĐẢO PHẢI: Các nút Hành động ---
                Row(
                    modifier = Modifier
                        .shadow(elevation = 4.dp, shape = RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .background(CardBackgroundColor) // Đảo Trắng
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.toggleEncryptionView() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isShowingRawEncryption) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Bật/Tắt giải mã",
                            tint = if (isShowingRawEncryption) Color(0xFFE91E63) else Color(0xFF6C63FF),
                            modifier = Modifier.size(20.dp) // Kích thước icon bên trong
                        )
                    }
                    IconButton(onClick = {

                    }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call",
                            tint = Color(0xFF6C63FF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Video Call",
                            tint = Color(0xFF6C63FF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
                AnimatedVisibility(visible = !isFriend) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFF3E0))
                            .padding(vertical = 8.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Người này chưa có trong danh sách bạn bè. Hãy cẩn thận khi chia sẻ thông tin cá nhân.",
                            fontSize = 12.sp,
                            color = Color(0xFFE65100),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        },
        bottomBar = {
            ChatBottomBar(
                text = messageText,
                replyingToMessage = replyingToMessage,
                partnerName = partnerName,
                currentUserId = currentUserId,
                onCancelReply = { replyingToMessage = null },
                onTextChange = { viewModel.onMessageTextChanged(it) },
                onSend = {
                    viewModel.sendMessage(replyToId = replyingToMessage?.id)
                    replyingToMessage = null
                },
                onAttachImage = {
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onAttachFile = {
                    filePickerLauncher.launch(
                        arrayOf("*/*")
                    )
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                //.padding(paddingValues)
                .background(AppBackgroundColor)
        ) {
            // THÊM KIỂM TRA Ở ĐÂY
            if (messages.isEmpty()) {
                EmptyChatGreeting(
                    partnerName = partnerName,
                    partnerAvatar = partnerAvatar
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppBackgroundColor)
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    reverseLayout = true,
                ) {

                    if (isPartnerTyping) {
                        item {
                            TypingIndicatorBubble(
                                partnerAvatar = partnerAvatar,
                                partnerName = partnerName
                            )
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

                        val previousMessage =
                            if (index < messages.size - 1) messages[index + 1] else null
                        val nextMessage = if (index > 0) messages[index - 1] else null

                        val showTimeHeader = shouldShowTimeHeader(
                            currentMessageTime = message.createdAt,
                            previousMessageTime = previousMessage?.createdAt
                        )

                        val isLastInGroup = nextMessage?.senderId != message.senderId
                        val showSmallTime = nextMessage?.senderId != message.senderId

                        Column {
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
                                        color = TextSecondaryColor,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier
                                            .background(
                                                CardBackgroundColor,
                                                RoundedCornerShape(12.dp)
                                            ) // Header Time cũng làm dạng đảo nhỏ
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
                                    activeDetailsMessageId =
                                        if (activeDetailsMessageId == message.id) null else message.id
                                },
                                onMessageLongClick = {
                                    activeDetailsMessageId = null
                                    selectedMessageForOverlay = message
                                },
                                showRawEncryption = isShowingRawEncryption
                            )
                            Spacer(modifier = Modifier.height(if (isLastInGroup) 16.dp else 4.dp))
                        }
                    }
                }
            }
        }
    }
    if (selectedMessageForOverlay != null) {
        MessageActionOverlay(
            message = selectedMessageForOverlay!!,
            isMine = selectedMessageForOverlay!!.senderId == currentUserId,
            onDismiss = { selectedMessageForOverlay = null },
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