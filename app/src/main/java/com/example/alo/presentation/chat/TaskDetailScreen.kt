package com.example.alo.presentation.chat

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.alo.presentation.theme.AppBackgroundColor
import com.example.alo.presentation.theme.CardBackgroundColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    navController: NavController,
    taskId: String,
    conversationId: String,
    viewModel: TaskDetailViewModel = hiltViewModel()
) {
    val task by viewModel.task.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("") }
    var assigneeId by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadTaskDetails(taskId, conversationId)
    }

    LaunchedEffect(task) {
        task?.let {
            title = it.title
            description = it.description ?: ""
            dueDate = it.dueDate ?: ""
            priority = it.priority
            assigneeId = it.assigneeId ?: ""
        }
    }

    Scaffold(
        containerColor = AppBackgroundColor,
        topBar = {
            TopAppBar(
                title = { Text("Chi tiết Công việc", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isAdmin) {
                        IconButton(onClick = {
                            viewModel.saveTaskDetails(title, description, dueDate, priority, assigneeId.takeIf { it.isNotBlank() })
                            Toast.makeText(context, "Đã lưu thay đổi", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Save, contentDescription = "Save", tint = Color(0xFF6C63FF))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackgroundColor)
            )
        }
    ) { paddingValues ->
        if (task == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF6C63FF))
            }
        } else {
            val isAssignee = currentUserId == task?.assigneeId
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Toggle Hoàn thành
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = {
                            if (isAdmin || isAssignee) {
                                viewModel.updateTaskStatus(!(task?.isCompleted ?: false))
                            } else {
                                Toast.makeText(context, "Chỉ người nhận việc hoặc trưởng nhóm mới có quyền cập nhật trạng thái", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (task?.isCompleted == true) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Status",
                            tint = if (task?.isCompleted == true) Color(0xFF43A047) else Color.Gray,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (task?.isCompleted == true) "Đã hoàn thành" else "Chưa hoàn thành",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (task?.isCompleted == true) Color(0xFF43A047) else Color.Gray
                    )
                }

                Divider()

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tiêu đề công việc") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isAdmin,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Mô tả") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    enabled = isAdmin,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 4
                )

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Ngày hết hạn (VD: 2024-12-31)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isAdmin,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Hiển thị Người tạo và Người nhận
                val creatorName by viewModel.creatorName.collectAsState()
                val assigneeName by viewModel.assigneeName.collectAsState()
                val members by viewModel.members.collectAsState()
                val memberNames by viewModel.memberNames.collectAsState()
                var expandedAssignee by remember { mutableStateOf(false) }

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = creatorName ?: "Đang tải...",
                        onValueChange = {},
                        label = { Text("Người giao việc") },
                        modifier = Modifier.weight(1f).padding(end = 4.dp),
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenuBox(
                        expanded = expandedAssignee && isAdmin,
                        onExpandedChange = { if (isAdmin) expandedAssignee = !expandedAssignee },
                        modifier = Modifier.weight(1f).padding(start = 4.dp)
                    ) {
                        OutlinedTextField(
                            value = memberNames[assigneeId] ?: assigneeName ?: "Chưa có",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Người nhận việc") },
                            trailingIcon = { if (isAdmin) ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAssignee) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            enabled = isAdmin,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedAssignee,
                            onDismissRequest = { expandedAssignee = false }
                        ) {
                            members.forEach { participant ->
                                DropdownMenuItem(
                                    text = { Text(memberNames[participant.userId] ?: participant.userId) },
                                    onClick = {
                                        assigneeId = participant.userId
                                        expandedAssignee = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Priority Selection
                Text("Mức độ ưu tiên:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("low" to "Thấp", "medium" to "Trung bình", "high" to "Cao").forEach { (key, label) ->
                        val isSelected = priority == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { if (isAdmin) priority = key },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (key == "high") Color(0xFFE53935).copy(alpha = 0.2f) else if (key == "medium") Color(0xFFFB8C00).copy(alpha = 0.2f) else Color(0xFF43A047).copy(alpha = 0.2f),
                                selectedLabelColor = if (key == "high") Color(0xFFE53935) else if (key == "medium") Color(0xFFFB8C00) else Color(0xFF43A047)
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (!isAdmin) {
                    Text(
                        text = "Lưu ý: Chỉ trưởng nhóm (Admin) mới có quyền chỉnh sửa chi tiết của công việc này.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }
        }
    }
}
