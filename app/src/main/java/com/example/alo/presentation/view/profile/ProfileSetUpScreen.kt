package com.example.alo.presentation.view.profile

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.alo.presentation.helper.ProfileSetupEvent
import com.example.alo.presentation.helper.ProfileSetupState
import com.example.alo.presentation.viewmodel.UserViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfileSetupScreen(
    navController: NavController,
    viewModel: UserViewModel = hiltViewModel(),
    onSetupComplete: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            onSetupComplete()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        StepIndicator(pageCount = 3, currentPage = pagerState.currentPage)

        // Pager chứa 3 màn hình trượt
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            userScrollEnabled = false
        ) { page ->
            when (page) {
                0 -> StepOneBasicInfo(state, viewModel)
                1 -> StepTwoDetails(state, viewModel)
                2 -> StepThreeAvatar(state, viewModel)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (pagerState.currentPage > 0) {
                OutlinedButton(onClick = {
                    coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                }) {
                    Text("Quay lại")
                }
            } else {
                Spacer(modifier = Modifier.width(8.dp))
            }

            if (pagerState.currentPage < 2) {
                Button(onClick = {
                    coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }) {
                    Text("Tiếp theo")
                }
            } else {
                Button(
                    onClick = { viewModel.onEvent(ProfileSetupEvent.Submit) },
                    enabled = !state.isLoading
                ) {
                    if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    else Text("Hoàn thành")
                }
            }
        }

        state.error?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
fun StepIndicator(pageCount: Int, currentPage: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp, top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (i in 0 until pageCount) {
            val color = if (i <= currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .background(color, RoundedCornerShape(3.dp))
            )
        }
    }
}

@Composable
fun StepOneBasicInfo(state: ProfileSetupState, viewModel: UserViewModel) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text("Bước 1: Thông tin cơ bản", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = state.username,
            onValueChange = { viewModel.onEvent(ProfileSetupEvent.EnteredUsername(it)) },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = state.displayName,
            onValueChange = { viewModel.onEvent(ProfileSetupEvent.EnteredDisplayName(it)) },
            label = { Text("Tên hiển thị") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepTwoDetails(state: ProfileSetupState, viewModel: UserViewModel) {
    var showDatePicker by remember { mutableStateOf(false) }
    var ageText by remember { mutableStateOf("") }
    val datePickerState = rememberDatePickerState()

    LaunchedEffect(datePickerState.selectedDateMillis) {
        datePickerState.selectedDateMillis?.let { millis ->
            val dob = Calendar.getInstance().apply { timeInMillis = millis }
            val today = Calendar.getInstance()

            var age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
                age--
            }

            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val dateString = sdf.format(dob.time)

            ageText = if (age >= 0) "$age tuổi" else "Không hợp lệ"
            viewModel.onEvent(ProfileSetupEvent.EnteredBirthday(dateString))
        }
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text("Bước 2: Thông tin chi tiết", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = state.phone,
            onValueChange = { viewModel.onEvent(ProfileSetupEvent.EnteredPhone(it)) },
            label = { Text("Số điện thoại") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        Text("Giới tính", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = state.gender == true, onClick = { viewModel.onEvent(ProfileSetupEvent.SelectedGender(true)) })
            Text("Nam", modifier = Modifier.clickable { viewModel.onEvent(ProfileSetupEvent.SelectedGender(true)) })
            Spacer(Modifier.width(16.dp))
            RadioButton(selected = state.gender == false, onClick = { viewModel.onEvent(ProfileSetupEvent.SelectedGender(false)) })
            Text("Nữ", modifier = Modifier.clickable { viewModel.onEvent(ProfileSetupEvent.SelectedGender(false)) })
        }
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = if (state.birthday.isNotEmpty()) "${state.birthday} ($ageText)" else "",
            onValueChange = {},
            label = { Text("Ngày sinh") },
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.DateRange, contentDescription = "Chọn ngày sinh")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = state.bio,
            onValueChange = { viewModel.onEvent(ProfileSetupEvent.EnteredBio(it)) },
            label = { Text("Tiểu sử (Bio)") },
            modifier = Modifier.fillMaxWidth()
        )
    }

    // Dialog DatePicker
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Xong") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Hủy") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun StepThreeAvatar(state: ProfileSetupState, viewModel: UserViewModel) {
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                val inputStream = context.contentResolver.openInputStream(it)
                val bytes = inputStream?.readBytes()
                viewModel.onEvent(ProfileSetupEvent.SelectedAvatar(bytes))
            }
        }
    )

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Bước 3: Cập nhật ảnh đại diện", style = MaterialTheme.typography.titleLarge, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(32.dp))

        if (state.avatarBytes != null) {
            val bitmap = remember(state.avatarBytes) {
                BitmapFactory.decodeByteArray(state.avatarBytes, 0, state.avatarBytes.size)
            }
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Avatar Preview",
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text("Chưa có ảnh", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(onClick = {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }) {
            Text(if (state.avatarBytes != null) "Thay đổi ảnh" else "Chọn ảnh từ thư viện")
        }
    }
}