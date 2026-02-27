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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding() // Tránh tai thỏ/thanh trạng thái
            .navigationBarsPadding() // Tránh thanh điều hướng dưới
            .imePadding() // Đẩy UI lên khi hiện bàn phím
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // Thanh tiến trình
        StepIndicator(pageCount = 3, currentPage = pagerState.currentPage)

        // Pager chứa 3 màn hình trượt
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            userScrollEnabled = false // Bắt buộc dùng nút để chuyển trang
        ) { page ->
            when (page) {
                0 -> StepOneBasicInfo(state, viewModel)
                1 -> StepTwoDetails(state, viewModel)
                2 -> StepThreeAvatar(state, viewModel)
            }
        }

        // Thanh nút điều hướng dưới cùng
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (pagerState.currentPage > 0) {
                OutlinedButton(
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                    modifier = Modifier.height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Quay lại", fontWeight = FontWeight.Bold)
                }
            } else {
                Spacer(modifier = Modifier.width(8.dp))
            }

            if (pagerState.currentPage < 2) {
                Button(
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                    modifier = Modifier.height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF))
                ) {
                    Text("Tiếp theo", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = { viewModel.onEvent(ProfileSetupEvent.Submit) },
                    enabled = !state.isLoading,
                    modifier = Modifier.height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF))
                ) {
                    if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("Hoàn thành", fontWeight = FontWeight.Bold)
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp, top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (i in 0 until pageCount) {
            val color = if (i <= currentPage) Color(0xFF6C63FF) else MaterialTheme.colorScheme.surfaceVariant
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Thông tin cơ bản", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        Text("Hãy cho mọi người biết bạn là ai", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 32.dp))

        OutlinedTextField(
            value = state.username,
            onValueChange = { viewModel.onEvent(ProfileSetupEvent.EnteredUsername(it)) },
            label = { Text("Username (Định danh)") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF6C63FF))
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = state.displayName,
            onValueChange = { viewModel.onEvent(ProfileSetupEvent.EnteredDisplayName(it)) },
            label = { Text("Tên hiển thị") },
            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF6C63FF))
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
            if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) age--

            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            ageText = if (age >= 0) "$age tuổi" else "Không hợp lệ"
            viewModel.onEvent(ProfileSetupEvent.EnteredBirthday(sdf.format(dob.time)))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Chi tiết cá nhân", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        Text("Bổ sung thông tin để kết nối tốt hơn", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 24.dp))

        OutlinedTextField(
            value = state.phone,
            onValueChange = { viewModel.onEvent(ProfileSetupEvent.EnteredPhone(it)) },
            label = { Text("Số điện thoại") },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF6C63FF))
        )

        Spacer(Modifier.height(16.dp))

        // Chọn ngày sinh
        OutlinedTextField(
            value = if (state.birthday.isNotEmpty()) "${state.birthday} ($ageText)" else "",
            onValueChange = {},
            label = { Text("Ngày sinh") },
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.DateRange, contentDescription = "Chọn ngày sinh", tint = Color(0xFF6C63FF))
                }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF6C63FF))
        )

        Spacer(Modifier.height(16.dp))

        Text("Giới tính", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { viewModel.onEvent(ProfileSetupEvent.SelectedGender(true)) }) {
                RadioButton(selected = state.gender == true, onClick = { viewModel.onEvent(ProfileSetupEvent.SelectedGender(true)) }, colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF6C63FF)))
                Text("Nam", modifier = Modifier.padding(end = 24.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { viewModel.onEvent(ProfileSetupEvent.SelectedGender(false)) }) {
                RadioButton(selected = state.gender == false, onClick = { viewModel.onEvent(ProfileSetupEvent.SelectedGender(false)) }, colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF6C63FF)))
                Text("Nữ")
            }
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = state.bio,
            onValueChange = { viewModel.onEvent(ProfileSetupEvent.EnteredBio(it)) },
            label = { Text("Tiểu sử (Bio)") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(100.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF6C63FF))
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { showDatePicker = false }) { Text("Xong", color = Color(0xFF6C63FF)) } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Hủy") } }
        ) { DatePicker(state = datePickerState) }
    }
}

@Composable
fun StepThreeAvatar(state: ProfileSetupState, viewModel: UserViewModel) {
    val context = LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                val bytes = context.contentResolver.openInputStream(it)?.readBytes()
                viewModel.onEvent(ProfileSetupEvent.SelectedAvatar(bytes))
            }
        }
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Ảnh đại diện", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        Text("Thêm khuôn mặt để bạn bè nhận ra bạn", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 40.dp))

        if (state.avatarBytes != null) {
            val bitmap = remember(state.avatarBytes) { BitmapFactory.decodeByteArray(state.avatarBytes, 0, state.avatarBytes.size) }
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Avatar Preview",
                modifier = Modifier.size(160.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.size(160.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            modifier = Modifier.height(50.dp).fillMaxWidth(0.7f),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF))
        ) {
            Text(if (state.avatarBytes != null) "Thay đổi ảnh" else "Chọn từ thư viện", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}