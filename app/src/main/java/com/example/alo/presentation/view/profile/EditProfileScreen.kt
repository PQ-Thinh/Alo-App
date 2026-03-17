package com.example.alo.presentation.view.profile

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.alo.presentation.helper.UserProfileState
import com.example.alo.presentation.theme.AppBackgroundColor
import com.example.alo.presentation.theme.CardBackgroundColor
import com.example.alo.presentation.theme.ErrorColor
import com.example.alo.presentation.theme.TextPrimaryColor
import com.example.alo.presentation.theme.TextSecondaryColor
import com.example.alo.presentation.viewmodel.UserViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.res.painterResource
import com.example.alo.presentation.view.utils.ProfileBackgrounds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    userViewModel: UserViewModel
) {
    val profileState by userViewModel.profileState.collectAsState()
    val context = LocalContext.current
    val primaryColor = Color(0xFF6C63FF)

    var isInitialized by remember { mutableStateOf(false) }

    // Các trường dữ liệu form
    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var ageText by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf<Boolean?>(null) }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedAvatarBytes by remember { mutableStateOf<ByteArray?>(null) }

    // Các trường validate UI
    var usernameError by remember { mutableStateOf<String?>(null) }
    var displayNameError by remember { mutableStateOf<String?>(null) }

    // State chọn ngày sinh
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    var selectedBackground by remember { mutableStateOf("bg_1") }

    LaunchedEffect(datePickerState.selectedDateMillis) {
        datePickerState.selectedDateMillis?.let { millis ->
            val dob = Calendar.getInstance().apply { timeInMillis = millis }
            val today = Calendar.getInstance()
            var age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) age--

            ageText = if (age >= 0) "$age tuổi" else "Không hợp lệ"
        }
    }
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        uri?.let {
            val bytes = context.contentResolver.openInputStream(it)?.readBytes()
            selectedAvatarBytes = bytes
        }
    }

    LaunchedEffect(Unit) {
        if (profileState is UserProfileState.Idle) {
            userViewModel.fetchCurrentUserProfile()
        }
    }

    // Hiển thị loading trong toàn bộ màn hình khi fetching / updating profile
    if (profileState is UserProfileState.Loading) {
        Box(modifier = Modifier.fillMaxSize().background(AppBackgroundColor), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = primaryColor)
        }
        return
    }

    if (profileState is UserProfileState.Success && !isInitialized) {
        val user = (profileState as UserProfileState.Success).user
        username = user.username
        displayName = user.displayName
        bio = user.bio ?: ""
        phone = user.phone ?: ""
        gender = user.gender

        // Chuyển format yyyy-MM-dd từ DB sang dd/MM/yyyy để hiện lên UI
        ageText = if (!user.birthday.isNullOrBlank()) {
            try {
                val dbFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val uiFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val date = dbFormat.parse(user.birthday)
                uiFormat.format(date!!)
            } catch (e: Exception) {
                user.birthday
            }
        } else ""

        isInitialized = true
    }

    if (profileState !is UserProfileState.Success) {
        Box(modifier = Modifier.fillMaxSize().background(AppBackgroundColor), contentAlignment = Alignment.Center) {
            Text("Có lỗi xảy ra khi tải hồ sơ.", color = ErrorColor)
        }
        return
    }

    val user = (profileState as UserProfileState.Success).user
    val isGoogleAccount = user.avatarId == "google_oauth_avatar"

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextPrimaryColor,
        unfocusedTextColor = TextPrimaryColor,
        disabledTextColor = TextSecondaryColor,
        focusedBorderColor = primaryColor,
        unfocusedBorderColor = Color(0xFFE0E0E0),
        disabledBorderColor = Color(0xFFEEEEEE),
        focusedLabelColor = primaryColor,
        unfocusedLabelColor = TextSecondaryColor,
        disabledLabelColor = TextSecondaryColor,
        errorTextColor = ErrorColor,
        errorBorderColor = ErrorColor,
        errorLabelColor = ErrorColor,
        errorSupportingTextColor = ErrorColor
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chỉnh sửa hồ sơ", fontSize = 18.sp, color = TextPrimaryColor, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Trở về", tint = TextPrimaryColor)
                    }
                },
                actions = {
                    TextButton(onClick = {
                        // --- Xử lý Validate trước khi save ---
                        var isValid = true
                        if (displayName.isBlank()) {
                            displayNameError = "Tên hiển thị không được để trống"
                            isValid = false
                        } else {
                            displayNameError = null
                        }

                        if (!isGoogleAccount && username.isBlank()) {
                            usernameError = "Username không được để trống"
                            isValid = false
                        } else {
                            usernameError = null
                        }

                        // Nếu Validate chuẩn -> gọi Cập nhật
                        if (isValid) {
                            userViewModel.updateUserProfile(
                                displayName = displayName,
                                bio = bio,
                                phone = phone,
                                birthday = ageText,
                                gender = gender, // Lưu giới tính
                                newUsername = username,
                                newAvatarBytes = selectedAvatarBytes,
                                onSuccess = {
                                    Toast.makeText(context, "Cập nhật thành công", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                },
                                onError = { msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }) {
                        Text("Lưu", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackgroundColor)
            )
        }
    ) { paddingValues ->
        // Component chọn ngày
        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        showDatePicker = false
                        datePickerState.selectedDateMillis?.let { millis ->
                            val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            ageText = format.format(Date(millis))
                        }
                    }) {
                        Text("Chọn", color = primaryColor)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Hủy", color = TextSecondaryColor) }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackgroundColor)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // --- 1. ẢNH ĐẠI DIỆN ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp), // Tổng chiều cao của khu vực Header
                    contentAlignment = Alignment.TopCenter
                ) {
                    // Ảnh nền (Banner) nằm dưới cùng, chiếm 140.dp phía trên
                    Image(
                        painter = painterResource(id = ProfileBackgrounds.getDrawable(selectedBackground)),
                        contentDescription = "Background",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(16.dp)) // Bo góc cho đẹp
                    )

                    // Avatar đè lên, căn lề dưới (nằm giao giữa nền và khoảng trắng)
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .align(Alignment.BottomCenter)
                            .shadow(elevation = 6.dp, shape = CircleShape)
                            .background(AppBackgroundColor, CircleShape) // Tạo viền cắt với nền
                            .padding(4.dp) // Độ dày của viền cắt
                            .clip(CircleShape)
                            .background(Color(0xFFE8EAF6))
                            .clickable(enabled = !isGoogleAccount) {
                                imagePicker.launch("image/*")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = selectedImageUri ?: user.avatarUrl,
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        if (!isGoogleAccount) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Sửa", tint = Color.White)
                            }
                        }
                    }
                }

                if (isGoogleAccount) {
                    Text(
                        text = "Tài khoản Google không thể đổi ảnh đại diện",
                        color = ErrorColor,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                ) {
                    Text("Chọn ảnh bìa", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimaryColor)
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(ProfileBackgrounds.backgrounds) { (bgName, bgResId) ->
                            val isSelected = selectedBackground == bgName
                            Image(
                                painter = painterResource(id = bgResId),
                                contentDescription = "Background option",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(100.dp, 60.dp) // Khung ảnh thu nhỏ
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) primaryColor else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedBackground = bgName }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                // --- 2. CÁC TRƯỜNG DỮ LIỆU (ĐẢO NỔI) ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp), clip = false)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBackgroundColor) // Thẻ trắng
                        .padding(16.dp) // Padding bên trong thẻ
                ) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            usernameError = null
                        },
                        label = { Text("Username") },
                        isError = usernameError != null,
                        supportingText = { if (usernameError != null) Text(usernameError!!) },
                        enabled = !isGoogleAccount,
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors,
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (isGoogleAccount) {
                        Text(
                            text = "Tài khoản Google không thể đổi Username",
                            color = ErrorColor,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .align(Alignment.Start)
                                .padding(start = 4.dp, bottom = 8.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = {
                            displayName = it
                            displayNameError = null
                        },
                        label = { Text("Tên hiển thị") },
                        isError = displayNameError != null,
                        supportingText = { if (displayNameError != null) Text(displayNameError!!) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // -- GIỚI TÍNH --
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                        Text(
                            text = "Giới tính",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryColor
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = gender == true,
                                onClick = { gender = true },
                                colors = RadioButtonDefaults.colors(selectedColor = primaryColor, unselectedColor = TextSecondaryColor)
                            )
                            Text("Nam", color = TextPrimaryColor, modifier = Modifier.clickable { gender = true })
                            Spacer(modifier = Modifier.width(24.dp))
                            RadioButton(
                                selected = gender == false,
                                onClick = { gender = false },
                                colors = RadioButtonDefaults.colors(selectedColor = primaryColor, unselectedColor = TextSecondaryColor)
                            )
                            Text("Nữ", color = TextPrimaryColor, modifier = Modifier.clickable { gender = false })
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // -- NGÀY SINH --
                    Box(modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }) {
                        OutlinedTextField(
                            value = ageText,
                            onValueChange = { },
                            label = { Text("Ngày sinh (dd/MM/yyyy)") },
                            readOnly = true,
                            enabled = false,
                            modifier = Modifier.fillMaxWidth(),
                            colors = textFieldColors,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // -- TIỂU SỬ --
                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("Tiểu sử") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        colors = textFieldColors,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // -- SỐ ĐIỆN THOẠI --
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Số điện thoại") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}