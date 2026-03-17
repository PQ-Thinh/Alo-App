package com.example.alo.presentation.view.home

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.alo.presentation.theme.AppBackgroundColor
import com.example.alo.presentation.theme.CardBackgroundColor
import com.example.alo.presentation.theme.TextPrimaryColor
import com.example.alo.presentation.theme.TextSecondaryColor
import com.example.alo.presentation.view.chat.Message
import com.example.alo.presentation.view.components.SearchTopBar
import com.example.alo.presentation.view.chat.Contact
import com.example.alo.presentation.view.navigation.Screen
import com.example.alo.presentation.view.profile.ProfileScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    onNavigateToChatRoom: (String) -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    var expandedMenu by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val primaryColor = Color(0xFF6C63FF)

    Scaffold(
        // Cập nhật màu nền tổng thể của màn hình thành Xám nhạt để Đảo nổi trắng nổi bật
        containerColor = AppBackgroundColor,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppBackgroundColor)
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = if (isSearchActive) Modifier.fillMaxWidth() else Modifier.weight(1f)) {
                    SearchTopBar(
                        active = isSearchActive,
                        onActiveChange = { isSearchActive = it },
                        onNavigateToChat = { conversationId ->
                            onNavigateToChatRoom(conversationId)
                        }
                    )
                }

                if (!isSearchActive) {
                    Box(modifier = Modifier.padding(end = 16.dp)) {
                        IconButton(
                            onClick = { expandedMenu = true },
                            modifier = Modifier.background(
                                CardBackgroundColor, // Đổi màu nút Add thành màu trắng nổi
                                RoundedCornerShape(12.dp)
                            ).shadow(2.dp, RoundedCornerShape(12.dp)) // Nút Add cũng hơi nổi nhẹ
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Tùy chọn", tint = primaryColor)
                        }

                        DropdownMenu(
                            expanded = expandedMenu,
                            onDismissRequest = { expandedMenu = false },
                            modifier = Modifier.background(CardBackgroundColor)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Thêm bạn", color = TextPrimaryColor) },
                                onClick = {
                                    expandedMenu = false
                                    // TODO: Mở popup hoặc chuyển màn hình Thêm bạn
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = primaryColor)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Tạo nhóm", color=TextPrimaryColor) },
                                onClick = {
                                    expandedMenu = false
                                    // TODO: Mở popup hoặc chuyển màn hình Tạo nhóm
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.GroupAdd, contentDescription = null, tint = primaryColor)
                                }
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Hiệu ứng "Đảo nổi" (Floating Bottom Bar)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp) // Cách hai bên và lề dưới
                    .shadow(
                        elevation = 16.dp, // Đổ bóng tạo cảm giác lơ lửng
                        shape = RoundedCornerShape(24.dp), // Bo tròn 4 góc
                        spotColor = Color.Black.copy(alpha = 0.1f) // Làm mềm màu bóng
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(CardBackgroundColor) // Màu trắng
            ) {
                NavigationBar(
                    modifier = Modifier.height(72.dp),
                    containerColor = Color.Transparent, // Để trong suốt để ăn theo màu Box
                    tonalElevation = 0.dp,
                    windowInsets = WindowInsets(0, 0, 0, 0) // Loại bỏ vùng đệm mặc định của hệ thống
                ) {
                    val items = listOf(
                        Triple("Tin nhắn", Icons.Default.Email, 0),
                        Triple("Danh bạ", Icons.Default.AccountBox, 1),
                        Triple("Cá nhân", Icons.Default.Person, 2)
                    )

                    items.forEach { (title, icon, index) ->
                        val isSelected = pagerState.currentPage == index

                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            icon = { Icon(icon, contentDescription = title) },
                            label = { Text(title, fontWeight = FontWeight.SemiBold) },
                            alwaysShowLabel = false,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = primaryColor,
                                selectedTextColor = primaryColor,
                                unselectedIconColor = TextSecondaryColor,
                                unselectedTextColor = TextSecondaryColor,
                                indicatorColor = primaryColor.copy(alpha = 0.1f) // Khối highlight nhạt phía sau icon
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { page ->
            when (page) {
                0 -> Message(
                    onNavigateToChatRoom = { conversationId ->
                        onNavigateToChatRoom(conversationId)
                        Log.e("DashboardScreen", "Mở phòng chat: $conversationId")
                    })
                1 -> Contact(
                    onNavigateToChatRoom = { conversationId ->
                        onNavigateToChatRoom(conversationId)
                    }
                )
                2 -> ProfileScreen(
                    onNavigateToProfile = { userId ->
                        navController.navigate(Screen.EditProfile.createRoute(userId))
                    },
                    onLogoutSuccess = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}