package com.example.alo.presentation.view.chat

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.alo.domain.model.User
import com.example.alo.presentation.theme.*
import com.example.alo.presentation.viewmodel.CreateTaskViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskScreen(
    navController: NavController,
    viewModel: CreateTaskViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }

    LaunchedEffect(state.success) {
        if (state.success) {
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Giao việc mới", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.saveTask() }, enabled = state.title.isNotBlank() && !state.isSaving) {
                        Text("Lưu", color = if (state.title.isNotBlank()) primaryColor else Color.Gray, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = AppBackgroundColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Text("Chi tiết công việc", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimaryColor)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = state.title,
                onValueChange = { viewModel.onTitleChanged(it) },
                label = { Text("Tiêu đề") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primaryColor)
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = state.description,
                onValueChange = { viewModel.onDescriptionChanged(it) },
                label = { Text("Mô tả chi tiết") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primaryColor)
            )
            Spacer(modifier = Modifier.height(24.dp))

            Text("Thêm thông tin", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimaryColor)
            Spacer(modifier = Modifier.height(12.dp))

            // Assignee Row
            var showMemberPicker by remember { mutableStateOf(false) }
            ListItem(
                headlineContent = { Text("Người thực hiện") },
                supportingContent = { Text(state.assignee?.displayName ?: "Chưa chọn") },
                leadingContent = { 
                    if (state.assignee != null) {
                         AsyncImage(
                            model = state.assignee!!.avatarUrl,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null, tint = primaryColor)
                    }
                },
                modifier = Modifier.clickable { showMemberPicker = true }.clip(RoundedCornerShape(12.dp))
            )
            
            // Due Date Row
            val dateFormat = SimpleDateFormat("HH:mm, dd/MM/yyyy", Locale.getDefault())
            ListItem(
                headlineContent = { Text("Hạn hoàn thành") },
                supportingContent = { Text(state.dueDate?.let { dateFormat.format(it.time) } ?: "Chưa thiết lập") },
                leadingContent = { Icon(Icons.Default.Event, contentDescription = null, tint = ErrorColor) },
                modifier = Modifier.clickable {
                    DatePickerDialog(context, { _, year, month, dayOfMonth ->
                        calendar.set(Calendar.YEAR, year)
                        calendar.set(Calendar.MONTH, month)
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                        
                        TimePickerDialog(context, { _, hourOfDay, minute ->
                            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                            calendar.set(Calendar.MINUTE, minute)
                            viewModel.onDueDateSelected(calendar.clone() as Calendar)
                        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
                        
                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                }.clip(RoundedCornerShape(12.dp))
            )

            if (showMemberPicker) {
                MemberPickerDialog(
                    members = state.members,
                    onDismiss = { showMemberPicker = false },
                    onSelected = { user ->
                        viewModel.onAssigneeSelected(user)
                        showMemberPicker = false
                    }
                )
            }
        }
    }
    
    if (state.isSaving) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = primaryColor)
        }
    }
}

@Composable
fun MemberPickerDialog(
    members: List<User>,
    onDismiss: () -> Unit,
    onSelected: (User?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chọn người thực hiện") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                item {
                    ListItem(
                        headlineContent = { Text("Bỏ chọn") },
                        modifier = Modifier.clickable { onSelected(null) }
                    )
                }
                items(members) { user ->
                    ListItem(
                        headlineContent = { Text(user.displayName) },
                        leadingContent = {
                            AsyncImage(
                                model = user.avatarUrl,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        },
                        modifier = Modifier.clickable { onSelected(user) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Đóng") }
        }
    )
}
