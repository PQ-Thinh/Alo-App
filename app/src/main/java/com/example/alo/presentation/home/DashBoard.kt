package com.example.alo.presentation.home

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.alo.presentation.theme.AppBackgroundColor
import com.example.alo.presentation.theme.CardBackgroundColor
import com.example.alo.presentation.theme.TextPrimaryColor
import com.example.alo.presentation.theme.TextSecondaryColor
import com.example.alo.presentation.chat.Message
import com.example.alo.presentation.components.SearchTopBar
import com.example.alo.presentation.chat.Contact
import com.example.alo.presentation.navigation.Screen
import com.example.alo.presentation.profile.ProfileScreen
import com.example.alo.presentation.profile.UserViewModel
import com.example.alo.presentation.home.DashboardViewModel
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.ImageView
import android.graphics.drawable.AnimatedVectorDrawable
import com.example.alo.R

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    onNavigateToChatRoom: (String) -> Unit,
    onNavigateToProfile: (String) -> Unit,
    userViewModel: UserViewModel = hiltViewModel(),
    dashboardViewModel: DashboardViewModel = hiltViewModel()
) {
    val currentUserId by userViewModel.currentUserId.collectAsState()
    val hasAssignedTasks by dashboardViewModel.hasAssignedTasks.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    var expandedMenu by remember { mutableStateOf(false) }
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
                                    navController.navigate(Screen.CreateGroup.route)
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
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .shadow(elevation = 8.dp, spotColor = Color.Black.copy(alpha = 0.1f)),
                containerColor = CardBackgroundColor,
                tonalElevation = 0.dp
            ) {
                val items = listOf<NavItem>(
                    NavItem("Tin nhắn", Icons.Outlined.Email, Icons.Default.Email, 0),
                    NavItem("Danh bạ", Icons.Outlined.AccountBox, Icons.Default.AccountBox, 1),
                    NavItem("Cá nhân", Icons.Outlined.Person, Icons.Default.Person, 2)
                )

                items.forEach { item ->
                    val isSelected = pagerState.currentPage == item.index

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(item.index)
                            }
                        },
                        icon = {
                            Icon(
                                if (isSelected) item.filled else item.outlined,
                                contentDescription = item.title,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = { Text(item.title, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        alwaysShowLabel = false,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = primaryColor,
                            selectedTextColor = primaryColor,
                            unselectedIconColor = TextSecondaryColor,
                            unselectedTextColor = TextSecondaryColor,
                            indicatorColor = primaryColor.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        val screenHeight = LocalConfiguration.current.screenHeightDp.dp
        
        Box(modifier = Modifier.fillMaxSize()) {
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
                2 -> {
                    if (currentUserId != null) {
                        ProfileScreen(
                            userId = currentUserId!!,
                            onNavigateToEditProfile = { userId ->
                                navController.navigate(Screen.EditProfile.createRoute(userId))
                            },
                            onLogoutSuccess = {
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = primaryColor)
                        }
                    }
                }
            }
            
            // Animation FAB for Assigned Tasks
            if (hasAssignedTasks) {
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.MyTasks.route) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = screenHeight * 0.18f),
                    containerColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            ImageView(ctx).apply {
                                setBackgroundResource(R.drawable.avd_work_anim)
                                (background as? AnimatedVectorDrawable)?.start()
                            }
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
}
data class NavItem(
    val title: String,
    val outlined: ImageVector,
    val filled: ImageVector,
    val index: Int
)