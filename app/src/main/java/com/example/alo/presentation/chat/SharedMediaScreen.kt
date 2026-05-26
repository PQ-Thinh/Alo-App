package com.example.alo.presentation.chat

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.alo.domain.model.Attachment
import com.example.alo.presentation.theme.AppBackgroundColor
import com.example.alo.presentation.theme.TextPrimaryColor
import com.example.alo.presentation.theme.TextSecondaryColor
import com.example.alo.presentation.theme.primaryColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedMediaScreen(
    navController: NavController,
    viewModel: SharedMediaViewModel = hiltViewModel(),
    initialTab: Int = 0
) {
    val state by viewModel.state.collectAsState()
    var selectedTabIndex by remember { mutableStateOf(initialTab) }
    
    val images = state.attachments.filter { it.fileType == "IMAGE" }
    val files = state.attachments.filter { it.fileType == "FILE" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kho lưu trữ", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimaryColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppBackgroundColor,
                    titleContentColor = TextPrimaryColor
                )
            )
        },
        containerColor = AppBackgroundColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = AppBackgroundColor,
                contentColor = primaryColor
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Ảnh/Video (${images.size})") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Tài liệu (${files.size})") }
                )
            }

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = primaryColor)
                }
            } else {
                when (selectedTabIndex) {
                    0 -> MediaGrid(images = images)
                    1 -> FileList(files = files)
                }
            }
        }
    }
}

@Composable
fun MediaGrid(images: List<Attachment>) {
    if (images.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Không có ảnh nào", color = TextSecondaryColor)
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(images) { attachment ->
                AsyncImage(
                    model = attachment.fileUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(2.dp)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.LightGray)
                        .clickable {
                            // Can add image viewer navigation later
                        },
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
fun FileList(files: List<Attachment>) {
    val context = LocalContext.current
    if (files.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Không có tài liệu nào", color = TextSecondaryColor)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(files) { attachment ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(attachment.fileUrl))
                            context.startActivity(intent)
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = primaryColor.copy(alpha = 0.1f)
                    ) {
                        Icon(
                            Icons.Default.InsertDriveFile,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = attachment.fileName ?: "File",
                            color = TextPrimaryColor,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${(attachment.fileSize ?: 0) / 1024} KB",
                            color = TextSecondaryColor,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
