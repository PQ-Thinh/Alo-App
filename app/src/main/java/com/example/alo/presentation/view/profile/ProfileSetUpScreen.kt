package com.example.alo.presentation.view.profile

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.alo.presentation.helper.ProfileSetupEvent
import com.example.alo.presentation.helper.ProfileSetupState
import com.example.alo.presentation.viewmodel.UserViewModel
import kotlinx.coroutines.launch

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
        // Pager chứa 3 màn hình trượt
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
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
                Button(onClick = {
                    coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                }) {
                    Text("Quay lại")
                }
            } else {
                Spacer(modifier = Modifier.width(8.dp)) // Chỗ trống để giữ bố cục
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
                    if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    else Text("Hoàn thành")
                }
            }
        }

        // Hiển thị lỗi nếu có
        state.error?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

// --- CÁC COMPONENT GIAO DIỆN CON ---

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

@Composable
fun StepTwoDetails(state: ProfileSetupState, viewModel: UserViewModel) {
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
        OutlinedTextField(
            value = state.bio,
            onValueChange = { viewModel.onEvent(ProfileSetupEvent.EnteredBio(it)) },
            label = { Text("Tiểu sử (Bio)") },
            modifier = Modifier.fillMaxWidth()
        )

    }
}
@Composable
fun StepThreeAvatar(state: ProfileSetupState, viewModel: UserViewModel) {

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text("Bước 3: Cập nhật ảnh đại diện", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        Button(onClick = {
            // FIXME: Gọi Intent mở Gallery chọn ảnh, convert sang ByteArray rồi ném vào hàm dưới
            // viewModel.onEvent(ProfileSetupEvent.SelectedAvatar(byteArray))
        }) {
            Text("Chọn ảnh từ thư viện")
        }

        if (state.avatarBytes != null) {
            Text("Đã chọn 1 ảnh!", color = MaterialTheme.colorScheme.primary)
        }
    }
}