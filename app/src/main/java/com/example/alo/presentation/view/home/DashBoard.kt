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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.alo.presentation.view.chat.Message
import com.example.alo.presentation.view.component.SearchTopBar
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

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = if (isSearchActive) Modifier.fillMaxWidth() else Modifier.weight(1f)) {
                    SearchTopBar(
                        active = isSearchActive,
                        onActiveChange = { isSearchActive = it }
                    )
                }

                if (!isSearchActive) {
                    Box(modifier = Modifier.padding(end = 16.dp)) {
                        IconButton(
                            onClick = { expandedMenu = true },
                            modifier = Modifier.background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(12.dp)
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Tùy chọn")
                        }

                        DropdownMenu(
                            expanded = expandedMenu,
                            onDismissRequest = { expandedMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Thêm bạn") },
                                onClick = {
                                    expandedMenu = false
                                    // TODO: Mở popup hoặc chuyển màn hình Thêm bạn
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.PersonAdd,
                                        contentDescription = null
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Tạo nhóm") },
                                onClick = {
                                    expandedMenu = false
                                    // TODO: Mở popup hoặc chuyển màn hình Tạo nhóm
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.GroupAdd,
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
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
                            selectedIconColor = Color(0xFF6C63FF),
                            selectedTextColor = Color(0xFF6C63FF),
                            unselectedIconColor = Color.LightGray,
                            unselectedTextColor = Color.LightGray,
                            indicatorColor = Color.White
                        )
                    )
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
                1 -> Contact()
                2 -> ProfileScreen(
                    onNavigateToProfile = { userId ->
                        onNavigateToProfile(userId)
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