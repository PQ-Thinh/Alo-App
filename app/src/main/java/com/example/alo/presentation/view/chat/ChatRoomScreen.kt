package com.example.alo.presentation.view.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.alo.domain.model.Message
import com.example.alo.presentation.viewmodel.ChatRoomViewModel
import com.example.alo.presentation.viewmodel.UserViewModel
import androidx.compose.runtime.collectAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    navController: NavController,
    viewModel: ChatRoomViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val messageText by viewModel.messageText.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val state by userViewModel.state.collectAsState()

    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${state.displayName}") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
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
                .padding(horizontal = 16.dp),
            reverseLayout = true,
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(
                items = messages,
                key = { it.id }
            ) { message ->
                val isMine = message.senderId == currentUserId
                MessageBubble(message = message, isMine = isMine)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message, isMine: Boolean) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMine) 16.dp else 4.dp,
                        bottomEnd = if (isMine) 4.dp else 16.dp
                    )
                )
                .background(if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp)
                .widthIn(max = 280.dp)
        ) {
            Text(
                text = message.encryptedContent,
                color = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
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