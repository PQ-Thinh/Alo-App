package com.example.alo.presentation.chat

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.alo.presentation.theme.*
import com.example.alo.presentation.navigation.Screen
import com.example.alo.presentation.chat.GroupDetailViewModel
import com.example.alo.presentation.chat.UserWithRole
import kotlinx.coroutines.launch
import com.example.alo.presentation.components.GlobalLoadingOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    navController: NavController,
    viewModel: GroupDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showEditNameDialog by remember { mutableStateOf(false) }
    var showEditStatusDialog by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showDissolveConfirmDialog by remember { mutableStateOf(false) }

    var newGroupName by remember { mutableStateOf(state.groupName) }
    var newGroupStatus by remember { mutableStateOf(state.groupStatus ?: "") }
    var newTaskTitle by remember { mutableStateOf("") }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val bytes = context.contentResolver.openInputStream(it)?.readBytes()
            if (bytes != null) {
                viewModel.updateGroupAvatar(bytes)
            }
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            if (it == "Đã rời nhóm" || it == "Đã giải tán nhóm") {
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
            viewModel.clearMessages()
        }
    }

    GlobalLoadingOverlay(
        isLoading = state.isDissolvingGroup,
        text = "Đang giải tán nhóm..."
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thông tin nhóm", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimaryColor)
                    }
                },
                actions = {
                    IconButton(onClick = { /* More actions */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = TextPrimaryColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = TextPrimaryColor
                )
            )
        },
        containerColor = AppBackgroundColor
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Header: Group Info with Premium Look
            // GROUP HEADER (AVATAR, NAME, STATUS, STATS)
            item {
                Spacer(modifier = Modifier.height(24.dp))
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. AVATAR AREA
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Surface(
                            modifier = Modifier
                                .size(110.dp)
                                .shadow(20.dp, CircleShape, spotColor = primaryColor.copy(alpha = 0.5f)),
                            shape = CircleShape,
                            color = Color.White,
                            border = BorderStroke(3.dp, Color.White)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .clickable { imagePickerLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                if (!state.groupAvatar.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = state.groupAvatar,
                                        contentDescription = "Group Avatar",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                brush = Brush.linearGradient(
                                                    colors = listOf(primaryColor, primaryColor.copy(alpha = 0.7f))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = state.groupName.firstOrNull()?.uppercase() ?: "?",
                                            fontSize = 44.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Edit Badge
                        Surface(
                            modifier = Modifier
                                .size(34.dp)
                                .offset(x = (-2).dp, y = (-2).dp),
                            shape = CircleShape,
                            color = Color.White,
                            tonalElevation = 6.dp,
                            shadowElevation = 6.dp
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Sửa ảnh",
                                tint = primaryColor,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 2. NAME & EDIT
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Text(
                            text = state.groupName,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimaryColor,
                            textAlign = TextAlign.Center
                        )
                        IconButton(
                            onClick = { 
                                newGroupName = state.groupName
                                showEditNameDialog = true 
                            },
                            modifier = Modifier.size(36.dp).padding(start = 6.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit, 
                                contentDescription = "Sửa tên", 
                                modifier = Modifier.size(18.dp), 
                                tint = TextSecondaryColor.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // 3. STATUS / BIO
                    if (!state.groupStatus.isNullOrEmpty()) {
                        Surface(
                            modifier = Modifier
                                .padding(
                                top = 10.dp)
                                .padding(horizontal = 40.dp)
                                .clickable { 
                                    newGroupStatus = state.groupStatus ?: ""
                                    showEditStatusDialog = true 
                                }
                               ,
                            shape = RoundedCornerShape(12.dp),
                            color = primaryColor.copy(alpha = 0.05f)
                        ) {
                            Text(
                                text = "“${state.groupStatus}”",
                                fontSize = 14.sp,
                                color = TextPrimaryColor.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Normal,
                                fontStyle = FontStyle.Italic,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else if (state.isAdmin) {
                        TextButton(
                            onClick = { 
                                newGroupStatus = ""
                                showEditStatusDialog = true 
                            },
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text("+ Thêm trạng thái nhóm", fontSize = 13.sp, color = primaryColor, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4. STATS PILL (Member Count)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = AppBackgroundColor,
                        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.1f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${state.members.size} thành viên",
                                fontSize = 13.sp,
                                color = TextPrimaryColor,
                                fontWeight = FontWeight.Bold
                            )
                            
                            if (state.isLoading) {
                                Spacer(modifier = Modifier.width(8.dp))
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp), 
                                    strokeWidth = 2.dp, 
                                    color = primaryColor
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Quick Actions with Modern Styling
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    QuickActionItem(Icons.Default.PersonAdd, "Thêm tv") { 
                        Log.d("UI_CLICK", "User click vào nút Thêm TV - ID: ${state.conversationId}")
                        navController.navigate(Screen.AddMember.createRoute(state.conversationId))
                    }
                    QuickActionItem(Icons.Default.Search, "Tìm kiếm") {
                        navController.popBackStack() // Quay về ChatRoom để tìm kiếm
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Kho lưu trữ (Shared Media)
            item {
                SectionHeader("Kho lưu trữ")
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        MediaRowItem(Icons.Default.Image, "Ảnh, video", "${state.mediaCount}") {}
                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = AppBackgroundColor, thickness = 1.dp)
                        MediaRowItem(Icons.Default.InsertDriveFile, "File, tài liệu", "${state.fileCount}") {}
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Shared Tasks - Chỉ hiển thị cho Admin
            if (state.isAdmin) {
                item {
                    SectionHeader("Công việc chung (Beta)")
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.03f)),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    modifier = Modifier.size(36.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    color = primaryColor.copy(alpha = 0.1f)
                                ) {
                                    Icon(Icons.Default.Assignment, contentDescription = null, tint = primaryColor, modifier = Modifier.padding(8.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Việc cần làm của nhóm", fontWeight = FontWeight.Bold, color = TextPrimaryColor, fontSize = 15.sp)
                                Spacer(modifier = Modifier.weight(1f))
                                IconButton(onClick = { navController.navigate(Screen.CreateTask.createRoute(state.conversationId)) }) {
                                    Icon(Icons.Default.AddCircle, contentDescription = null, tint = primaryColor, modifier = Modifier.size(24.dp))
                                }
                            }
                            
                            if (state.tasks.isEmpty()) {
                                Text(
                                    "Chưa có công việc nào. Hãy thêm công việc đầu tiên!",
                                    fontSize = 13.sp,
                                    color = TextSecondaryColor,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            } else {
                                state.tasks.forEach { task ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                navController.navigate(Screen.TaskDetail.createRoute(task.id, state.conversationId))
                                            }
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = task.isCompleted,
                                            onCheckedChange = { viewModel.toggleTaskCompletion(task) },
                                            colors = CheckboxDefaults.colors(checkedColor = primaryColor, uncheckedColor = TextSecondaryColor.copy(alpha = 0.5f))
                                        )
                                        Text(
                                            text = task.title,
                                            fontSize = 14.sp,
                                            color = if (task.isCompleted) TextSecondaryColor else TextPrimaryColor,
                                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                                            modifier = Modifier.weight(1f),
                                            fontWeight = if (task.isCompleted) FontWeight.Normal else FontWeight.Medium
                                        )
                                        IconButton(onClick = { viewModel.deleteTask(task.id) }) {
                                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextSecondaryColor.copy(alpha = 0.5f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Member Header / Navigation
            item {
                SectionHeader("Thành viên")
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                Log.d("UI_CLICK", "Navigating to Group Members list - ID: ${state.conversationId}")
                                navController.navigate(Screen.GroupMembers.createRoute(state.conversationId)) 
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = primaryColor.copy(alpha = 0.1f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.People, contentDescription = null, tint = primaryColor)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Thành viên nhóm", fontWeight = FontWeight.Bold, color = TextPrimaryColor)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${state.members.size} người", fontSize = 12.sp, color = TextSecondaryColor)
                                if (state.isLoading) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 1.5.dp, color = primaryColor)
                                }
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondaryColor)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Leave Group
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.leaveGroup() },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, ErrorColor.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    enabled = !state.isLeaving
                ) {
                    if (state.isLeaving) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = ErrorColor, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Logout, contentDescription = null, tint = ErrorColor, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Rời khỏi nhóm", color = ErrorColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
                
                if (state.isAdmin) {
                    TextButton(
                        onClick = { showDissolveConfirmDialog = true },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        contentPadding = PaddingValues(16.dp),
                        enabled = !state.isDissolvingGroup
                    ) {
                        if (state.isDissolvingGroup) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = ErrorColor, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Giải tán nhóm", color = ErrorColor.copy(alpha = 0.7f), fontSize = 14.sp)
                    }
                }
            }
        }
    }

    // Dialogs ... (unchanged or lightly polished)
    if (showEditNameDialog) {
        ModernEditDialog(
            title = "Đổi tên nhóm",
            value = newGroupName,
            onValueChange = { newGroupName = it },
            onConfirm = {
                viewModel.updateGroupName(newGroupName)
                showEditNameDialog = false
            },
            onDismiss = { showEditNameDialog = false }
        )
    }

    if (showEditStatusDialog) {
        ModernEditDialog(
            title = "Trạng thái nhóm",
            value = newGroupStatus,
            onValueChange = { newGroupStatus = it },
            onConfirm = {
                viewModel.updateGroupStatus(newGroupStatus)
                showEditStatusDialog = false
            },
            onDismiss = { showEditStatusDialog = false },
            placeholder = "Ví dụ: Đang bận họp..."
        )
    }

    // Dialog xác nhận giải tán nhóm
    if (showDissolveConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDissolveConfirmDialog = false },
            title = {
                Text("Giải tán nhóm", fontWeight = FontWeight.Bold, color = ErrorColor)
            },
            text = {
                Text(
                    "Bạn có chắc chắn muốn giải tán nhóm \"${state.groupName}\"?\n\nTất cả tin nhắn và dữ liệu của nhóm sẽ bị xóa vĩnh viễn. Hành động này không thể hoàn tác.",
                    color = TextPrimaryColor,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDissolveConfirmDialog = false
                        viewModel.dissolveGroup()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorColor),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Giải tán", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDissolveConfirmDialog = false }) {
                    Text("Hủy", color = TextSecondaryColor)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White
        )
    }
}

@Composable
fun MediaRowItem(icon: ImageVector, title: String, count: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = RoundedCornerShape(8.dp),
            color = AppBackgroundColor
        ) {
            Icon(icon, contentDescription = null, tint = TextSecondaryColor, modifier = Modifier.padding(6.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, color = TextPrimaryColor, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.weight(1f))
        Text(count, color = TextSecondaryColor)
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondaryColor, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun QuickActionItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(8.dp)
    ) {
        Surface(
            modifier = Modifier.size(52.dp),
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 4.dp,
            tonalElevation = 2.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = primaryColor, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(label, fontSize = 12.sp, color = TextPrimaryColor, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ModernEditDialog(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    placeholder: String = ""
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text(placeholder) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = TextSecondaryColor.copy(alpha = 0.3f)
                )
            )
        },
        confirmButton = {
            Button(onClick = onConfirm, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = primaryColor)) {
                Text("Xác nhận", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = TextSecondaryColor)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White
    )
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.Black,
        fontSize = 17.sp,
        color = TextPrimaryColor,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)
    )
}

@Composable
fun MemberItem(
    member: UserWithRole,
    isAdmin: Boolean,
    isMe: Boolean,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(48.dp)) {
            if (!member.user.avatarUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = member.user.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().clip(CircleShape).background(
                        brush = Brush.linearGradient(
                            colors = listOf(primaryColor.copy(alpha = 0.2f), primaryColor.copy(alpha = 0.1f))
                        )
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(member.user.displayName.firstOrNull()?.uppercase() ?: "?", color = primaryColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
            // Online status dot? (Optional)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isMe) "${member.user.displayName} (Bạn)" else member.user.displayName,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryColor,
                    fontSize = 15.sp
                )
                if (member.role == "admin") {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)),
                        color = primaryColor.copy(alpha = 0.1f)
                    ) {
                        Text("Trưởng nhóm", fontSize = 10.sp, color = primaryColor, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
            Text(text = "Nhấn để xem trang cá nhân", fontSize = 12.sp, color = TextSecondaryColor)
        }
        
        if (isAdmin && !isMe) {
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.PersonRemove, contentDescription = "Remove", tint = ErrorColor.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
            }
        } else {
             Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondaryColor.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
        }
    }
}
