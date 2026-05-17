package com.example.alo.presentation.chat

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.alo.presentation.theme.*
import com.example.alo.presentation.navigation.Screen
import com.example.alo.presentation.chat.GroupDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupMembersScreen(
    navController: NavController,
    viewModel: GroupDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val filteredMembers = remember(state.members, searchQuery) {
        if (searchQuery.isBlank()) state.members
        else state.members.filter { it.user.displayName.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Color.White)) {
                TopAppBar(
                    title = { 
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text("Thành viên (${state.members.size})", fontWeight = FontWeight.Black) 
                            if (state.isLoading) {
                                Spacer(modifier = Modifier.width(8.dp))
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = primaryColor)
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Tìm tên thành viên", fontSize = 14.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondaryColor) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = Color(0xFFEEEEEE),
                        focusedContainerColor = Color(0xFFF9F9F9),
                        unfocusedContainerColor = Color(0xFFF9F9F9),
                    ),
                    singleLine = true
                )
            }
        },
        containerColor = AppBackgroundColor
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            android.util.Log.d("UI_CLICK", "User click vào nút Thêm thành viên - ID: ${state.conversationId}")
                            navController.navigate(Screen.AddMember.createRoute(state.conversationId)) 
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
                            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = primaryColor)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Thêm thành viên", fontWeight = FontWeight.Bold, color = primaryColor)
                }
                HorizontalDivider(color = AppBackgroundColor, thickness = 1.dp)
            }

            items(filteredMembers, key = { it.user.id }) { member ->
                MemberRowItem(
                    member = member,
                    isAdmin = state.isAdmin,
                    isMe = member.user.id == state.currentUserId,
                    onRemove = { viewModel.removeMember(member.user.id) },
                    onRoleUpdate = { newRole -> viewModel.updateMemberRole(member.user.id, newRole) },
                    onMemberClick = { userId -> navController.navigate(Screen.Profile.createRoute(userId)) }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 82.dp), color = AppBackgroundColor, thickness = 1.dp)
            }
        }
    }
}

@Composable
fun MemberRowItem(
    member: com.example.alo.presentation.chat.UserWithRole,
    isAdmin: Boolean,
    isMe: Boolean,
    onRemove: () -> Unit,
    onRoleUpdate: (String) -> Unit,
    onMemberClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                android.util.Log.d("UI_CLICK", "Xem thông tin thành viên: ${member.user.id}")
                onMemberClick(member.user.id) 
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(50.dp)) {
            if (!member.user.avatarUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = member.user.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(primaryColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        member.user.displayName.firstOrNull()?.uppercase() ?: "?",
                        color = primaryColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isMe) "${member.user.displayName} (Bạn)" else member.user.displayName,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryColor,
                    fontSize = 16.sp
                )
                if (member.role == "admin") {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)),
                        color = primaryColor.copy(alpha = 0.1f)
                    ) {
                        Text(
                            "Trưởng nhóm",
                            fontSize = 10.sp,
                            color = primaryColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Text(text = "Nhấn để xem trang cá nhân", fontSize = 12.sp, color = TextSecondaryColor)
        }

        if (isAdmin && !isMe) {
            var showOptions by remember { mutableStateOf(false) }
            IconButton(onClick = { showOptions = true }) {
                Icon(Icons.Default.MoreHoriz, contentDescription = "Options", tint = TextSecondaryColor)
            }
            DropdownMenu(expanded = showOptions, onDismissRequest = { showOptions = false }) {
                DropdownMenuItem(
                    text = { Text("Chỉ định trưởng nhóm", color = TextPrimaryColor) },
                    onClick = { 
                        onRoleUpdate("admin")
                        showOptions = false 
                    },
                    leadingIcon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("Xóa khỏi nhóm", color = ErrorColor) },
                    onClick = {
                        onRemove()
                        showOptions = false
                    },
                    leadingIcon = { Icon(Icons.Default.PersonRemove, contentDescription = null, tint = ErrorColor) }
                )
            }
        }
    }
}
