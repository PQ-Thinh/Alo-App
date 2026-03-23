package com.example.alo.presentation.view.call

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.getstream.video.android.core.Call

/**
 * Màn hình cuộc gọi đến (Incoming call).
 * Hiển thị thông tin người gọi và nút Accept/Decline.
 *
 * @param callerName   Tên người gọi.
 * @param callerAvatar URL avatar người gọi.
 * @param onAccept     Callback khi nhấn Accept.
 * @param onDecline    Callback khi nhấn Decline.
 */
@Composable
fun IncomingCallScreen(
    callerName: String,
    callerAvatar: String?,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val micGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false

        if (cameraGranted && micGranted) {
            // Có quyền -> Bắt máy
            onAccept()
        } else {
            // Không cho quyền -> Cúp máy luôn cho an toàn
            onDecline()
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A0A1A), Color(0xFF1A237E))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Avatar người gọi
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3949AB)),
                contentAlignment = Alignment.Center
            ) {
                if (!callerAvatar.isNullOrBlank()) {
                    AsyncImage(
                        model = callerAvatar,
                        contentDescription = "Avatar $callerName",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = callerName.firstOrNull()?.uppercase() ?: "?",
                        color = Color.White,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = callerName,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Đang gọi video cho bạn...",
                color = Color(0xFFB0BEC5),
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(64.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(64.dp, Alignment.CenterHorizontally),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Decline
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FilledIconButton(
                        onClick = onDecline,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color(0xFFE53935)
                        ),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(
                            Icons.Default.CallEnd,
                            contentDescription = "Từ chối",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Từ chối", color = Color(0xFFEF9A9A), fontSize = 14.sp)
                }

                // Accept
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FilledIconButton(
                        onClick = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.CAMERA,
                                    Manifest.permission.RECORD_AUDIO
                                )
                            )
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color(0xFF43A047)
                        ),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(
                            Icons.Default.Call,
                            contentDescription = "Chấp nhận",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Chấp nhận", color = Color(0xFFA5D6A7), fontSize = 14.sp)
                }
            }
        }
    }
}