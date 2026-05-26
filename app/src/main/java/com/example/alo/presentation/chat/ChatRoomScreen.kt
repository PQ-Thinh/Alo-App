package com.example.alo.presentation.chat

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.core.content.ContextCompat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
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
import com.example.alo.presentation.theme.primaryColor
import com.example.alo.presentation.components.ChatBottomBar
import com.example.alo.presentation.components.EmptyChatGreeting
import com.example.alo.presentation.components.MessageActionOverlay
import com.example.alo.presentation.components.MessageBubble
import com.example.alo.presentation.components.TypingIndicatorBubble
import com.example.alo.presentation.navigation.Screen
import com.example.alo.presentation.utils.formatTimeHeader
import com.example.alo.presentation.utils.getUserStatus
import com.example.alo.presentation.utils.shouldShowTimeHeader
import com.example.alo.presentation.call.CallViewModel
import com.example.alo.presentation.chat.ChatRoomViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    navController: NavController,
    viewModel: ChatRoomViewModel = hiltViewModel(),
    callViewModel: CallViewModel = hiltViewModel()
) {
    val partnerId by viewModel.partnerId.collectAsState()
    val partnerLastSeen by viewModel.partnerLastSeen.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val messageText by viewModel.messageText.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()

    val partnerName by viewModel.partnerName.collectAsState()
    val partnerAvatar by viewModel.partnerAvatar.collectAsState()


    val listState = rememberLazyListState()
    var activeDetailsMessageId by remember { mutableStateOf<String?>(null) }
    var highlightedMessageId by remember { mutableStateOf<String?>(null) }
    val isPartnerTyping by viewModel.isPartnerTyping.collectAsState()

    var selectedMessageForOverlay by remember { mutableStateOf<Message?>(null) }
    val clipboardManager = LocalClipboardManager.current
    var replyingToMessage by remember { mutableStateOf<Message?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val conversationId = viewModel.conversationId

    val isShowingRawEncryption by viewModel.isShowingRawEncryption.collectAsState()

    val isFriend by viewModel.isFriend.collectAsState()
    val isGroup by viewModel.isGroup.collectAsState()
    val memberProfiles by viewModel.memberProfiles.collectAsState()

    var currentTimeTrigger by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Search state
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResultIndices by remember { mutableStateOf<List<Int>>(emptyList()) }
    var currentSearchIndex by remember { mutableIntStateOf(0) }

    // Compute search results
    LaunchedEffect(searchQuery, messages) {
        if (searchQuery.length >= 2) {
            searchResultIndices = messages.mapIndexedNotNull { index, msg ->
                if (msg.encryptedContent.contains(searchQuery, ignoreCase = true)) index else null
            }
            currentSearchIndex = 0
            // Auto scroll to first result
            if (searchResultIndices.isNotEmpty()) {
                highlightedMessageId = messages[searchResultIndices[0]].id
                listState.animateScrollToItem(searchResultIndices[0])
            } else {
                highlightedMessageId = null
            }
        } else {
            searchResultIndices = emptyList()
            highlightedMessageId = null
        }
    }


    val permissionsToRequest = remember {
        mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val areGranted = permissionsMap.values.all { it }
        if (areGranted) {
            // Permissions granted → start the call
            callViewModel.startCall(
                callId = conversationId,
                memberIds = listOf(partnerId)
            )
            navController.navigate(
                Screen.OutgoingCall.createRoute(
                    callId = conversationId,
                    calleeName = partnerName,
                    calleeAvatar = partnerAvatar
                )
            )
        } else {
            Toast.makeText(context, "Vui lòng cấp quyền Camera và Mic để gọi video!", Toast.LENGTH_SHORT).show()
        }
    }

    val startCallAction = {
        val areAllGranted = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (areAllGranted) {
            callViewModel.startCall(
                callId = conversationId,
                memberIds = listOf(partnerId)
            )
            navController.navigate(
                Screen.OutgoingCall.createRoute(
                    callId = conversationId,
                    calleeName = partnerName,
                    calleeAvatar = partnerAvatar
                )
            )
        } else {
            permissionLauncher.launch(permissionsToRequest)
        }
    }

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
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // --- ĐẢO TRÁI: Nút Back + User Info ---
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                        .shadow(elevation = 8.dp, shape = RoundedCornerShape(28.dp), spotColor = primaryColor.copy(alpha = 0.2f))
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.White)
                        .clickable(enabled = if (isGroup) conversationId.isNotEmpty() else partnerId.isNotEmpty()) { 
                            if (isGroup) {
                                navController.navigate(Screen.GroupDetail.createRoute(conversationId))
                            } else {
                                navController.navigate(Screen.Profile.createRoute(partnerId))
                            }
                        }
                        .padding(start = 4.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại", tint = TextPrimaryColor, modifier = Modifier.size(22.dp))
                    }
                    
                    // Avatar with Online Status
                    Box(modifier = Modifier.size(42.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(primaryColor.copy(alpha = 0.1f)),
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
                                    fontWeight = FontWeight.Black,
                                    color = primaryColor,
                                    fontSize = 18.sp
                                )
                            }
                        }

                        if (userStatus.isOnline && !isGroup) {
                            Surface(
                                modifier = Modifier
                                    .size(13.dp)
                                    .align(Alignment.BottomEnd),
                                shape = CircleShape,
                                color = Color(0xFF4CAF50),
                                border = BorderStroke(2.dp, Color.White)
                            ) {}
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Name & Status
                    Column(verticalArrangement = Arrangement.Center) {
                        Text(
                            text = partnerName,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = TextPrimaryColor,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        
                        // Enhanced Status Display
                        val groupStatus by viewModel.groupStatus.collectAsState()
                        val isOnline = userStatus.isOnline && !isGroup
                        
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val pulseScale by infiniteTransition.animateFloat(
                            initialValue = 0.6f,
                            targetValue = 1.0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1200),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "scale"
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .then(
                                    if (isOnline || isGroup) {
                                        Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isOnline) Color(0xFF4CAF50).copy(alpha = 0.1f)
                                                else primaryColor.copy(alpha = 0.1f)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    } else Modifier
                                )
                        ) {
                            if (isOnline) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .shadow(elevation = 4.dp, shape = CircleShape, spotColor = Color(0xFF4CAF50).copy(alpha = pulseScale))
                                        .background(Color(0xFF4CAF50), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            } else if (isGroup) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            
                            Text(
                                text = if (isGroup) {
                                    groupStatus ?: "Nhóm • Nhấn để xem thành viên"
                                } else userStatus.statusText,
                                fontSize = 11.sp,
                                fontWeight = if (isOnline || isGroup) FontWeight.Bold else FontWeight.Medium,
                                color = if (isOnline) Color(0xFF2E7D32) else if (isGroup) primaryColor else TextSecondaryColor,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // --- ĐẢO PHẢI: Các nút Hành động ---
                Row(
                    modifier = Modifier
                        .shadow(elevation = 8.dp, shape = RoundedCornerShape(28.dp), spotColor = primaryColor.copy(alpha = 0.2f))
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.White)
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.toggleEncryptionView() },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isShowingRawEncryption) Color(0xFFFFE1E1) else primaryColor.copy(alpha = 0.05f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isShowingRawEncryption) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = "Bật/Tắt giải mã",
                                    tint = if (isShowingRawEncryption) Color(0xFFE91E63) else primaryColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    
                    IconButton(
                        onClick = {
                            isSearchActive = !isSearchActive
                            if (!isSearchActive) {
                                searchQuery = ""
                                highlightedMessageId = null
                            }
                        },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSearchActive) primaryColor.copy(alpha = 0.15f) else primaryColor.copy(alpha = 0.05f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Tìm kiếm",
                                    tint = primaryColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            if (!isGroup) startCallAction() else Toast.makeText(context, "Tính năng gọi nhóm đang phát triển", Toast.LENGTH_SHORT).show()
                        }, 
                        modifier = Modifier.size(42.dp),
                        enabled = !isGroup
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Video Call",
                            tint = if (isGroup) Color.LightGray else primaryColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
                AnimatedVisibility(visible = !isFriend) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFFFF8E1)
                    ) {
                        Text(
                            text = "Người này chưa có trong danh sách bạn bè. Hãy cẩn thận!",
                            fontSize = 11.sp,
                            color = Color(0xFFFF8F00),
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
                // Search Bar
                AnimatedVisibility(
                    visible = isSearchActive,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White,
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Tìm tin nhắn...", fontSize = 14.sp) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(22.dp),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondaryColor, modifier = Modifier.size(18.dp)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryColor,
                                    unfocusedBorderColor = Color(0xFFEEEEEE),
                                    focusedContainerColor = Color(0xFFF5F5F5),
                                    unfocusedContainerColor = Color(0xFFF5F5F5)
                                ),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                            )
                            if (searchResultIndices.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "${currentSearchIndex + 1}/${searchResultIndices.size}",
                                    fontSize = 12.sp,
                                    color = TextSecondaryColor,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = {
                                        if (searchResultIndices.isNotEmpty()) {
                                            currentSearchIndex = (currentSearchIndex - 1 + searchResultIndices.size) % searchResultIndices.size
                                            val idx = searchResultIndices[currentSearchIndex]
                                            highlightedMessageId = messages[idx].id
                                            coroutineScope.launch { listState.animateScrollToItem(idx) }
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Trước", tint = primaryColor, modifier = Modifier.size(20.dp))
                                }
                                IconButton(
                                    onClick = {
                                        if (searchResultIndices.isNotEmpty()) {
                                            currentSearchIndex = (currentSearchIndex + 1) % searchResultIndices.size
                                            val idx = searchResultIndices[currentSearchIndex]
                                            highlightedMessageId = messages[idx].id
                                            coroutineScope.launch { listState.animateScrollToItem(idx) }
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Sau", tint = primaryColor, modifier = Modifier.size(20.dp))
                                }
                            }
                            IconButton(
                                onClick = {
                                    isSearchActive = false
                                    searchQuery = ""
                                    highlightedMessageId = null
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Đóng", tint = TextSecondaryColor, modifier = Modifier.size(18.dp))
                            }
                        }
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
                        
                        val computedRepliedSenderName = repliedMessage?.let { replMsg ->
                            if (replMsg.senderId == currentUserId) {
                                "Bạn"
                            } else if (isGroup) {
                                memberProfiles[replMsg.senderId]?.displayName ?: "Thành viên"
                            } else {
                                partnerName
                            }
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
                        val isFirstInGroup = previousMessage?.senderId != message.senderId || showTimeHeader


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
                                isGroup = isGroup,
                                senderName = memberProfiles[message.senderId]?.displayName,
                                showSenderName = isFirstInGroup,
                                isHighlighted = highlightedMessageId == message.id,
                                memberAvatar = memberProfiles[message.senderId]?.avatarUrl,
                                repliedSenderName = computedRepliedSenderName,


                                onMessageClick = {
                                    activeDetailsMessageId =
                                        if (activeDetailsMessageId == message.id) null else message.id
                                },
                                onMessageLongClick = {
                                    activeDetailsMessageId = null
                                    selectedMessageForOverlay = message
                                },
                                onAvatarClick = { userId ->
                                    navController.navigate(Screen.Profile.createRoute(userId))
                                },
                                showRawEncryption = isShowingRawEncryption,
                                onSwipeToReply = {
                                    replyingToMessage = message
                                },
                                onReplyClick = { repliedMessageId ->
                                    val idx = messages.indexOfFirst { it.id == repliedMessageId }
                                    if (idx != -1) {
                                        coroutineScope.launch {
                                            highlightedMessageId = repliedMessageId
                                            listState.animateScrollToItem(idx)
                                            delay(1500L)
                                            if (highlightedMessageId == repliedMessageId) {
                                                highlightedMessageId = null
                                            }
                                        }
                                    }
                                }
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
