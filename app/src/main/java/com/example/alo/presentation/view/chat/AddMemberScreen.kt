package com.example.alo.presentation.view.chat

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
import com.example.alo.presentation.viewmodel.AddMemberViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMemberScreen(
    navController: NavController,
    viewModel: AddMemberViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.success) {
        if (state.success) {
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Color.White)) {
                TopAppBar(
                    title = { Text("Thêm thành viên", fontWeight = FontWeight.Black) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = { viewModel.addMembers() },
                            enabled = state.selectedUserIds.isNotEmpty() && !state.isAdding
                        ) {
                            Text(
                                "Thêm (${state.selectedUserIds.size})",
                                color = if (state.selectedUserIds.isNotEmpty()) primaryColor else Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = { Text("Số điện thoại, email hoặc tên", fontSize = 14.sp) },
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
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize().background(Color.White)) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = primaryColor)
            } else if (state.searchResults.isEmpty() && state.searchQuery.isNotEmpty()) {
                Text(
                    "Không tìm thấy người dùng",
                    modifier = Modifier.align(Alignment.Center),
                    color = TextSecondaryColor
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.searchResults) { user ->
                        val isAlreadyMember = state.currentMembers.contains(user.id)
                        val isSelected = state.selectedUserIds.contains(user.id)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isAlreadyMember) { viewModel.toggleSelection(user.id) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                             Box(modifier = Modifier.size(48.dp)) {
                                if (!user.avatarUrl.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = user.avatarUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize().clip(CircleShape).background(primaryColor.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(user.displayName.firstOrNull()?.uppercase() ?: "?", color = primaryColor, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(user.displayName, fontWeight = FontWeight.Bold, color = if (isAlreadyMember) Color.Gray else TextPrimaryColor)
                                if (isAlreadyMember) {
                                    Text("Đã là thành viên", fontSize = 12.sp, color = Color.Gray)
                                } else {
                                    Text(user.email, fontSize = 12.sp, color = TextSecondaryColor)
                                }
                            }
                            
                            if (!isAlreadyMember) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { viewModel.toggleSelection(user.id) },
                                    colors = CheckboxDefaults.colors(checkedColor = primaryColor)
                                )
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(start = 80.dp), color = AppBackgroundColor)
                    }
                }
            }
            
            if (state.isAdding) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                   Card(
                       shape = RoundedCornerShape(16.dp),
                       colors = CardDefaults.cardColors(containerColor = Color.White)
                   ) {
                       Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                           CircularProgressIndicator(color = primaryColor)
                           Spacer(modifier = Modifier.height(16.dp))
                           Text("Đang thêm thành viên...", fontWeight = FontWeight.Bold)
                       }
                   }
                }
            }
        }
    }
}
