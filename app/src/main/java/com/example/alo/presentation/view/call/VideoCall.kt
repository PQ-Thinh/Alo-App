package com.example.alo.presentation.view.call

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.alo.presentation.theme.AppBackgroundColor
import com.example.alo.presentation.theme.CardBackgroundColor
import com.example.alo.presentation.viewmodel.VideoCallViewModel
import io.getstream.video.android.compose.permission.LaunchCallPermissions
import io.getstream.video.android.compose.theme.StreamColors
import io.getstream.video.android.compose.theme.StreamShapes
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.activecall.CallContent
import io.getstream.video.android.compose.ui.components.call.controls.actions.CancelCallAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.FlipCameraAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleCameraAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleMicrophoneAction

@Composable
fun VideoCallScreen(
    navController: NavController,
    conversationId: String,
    viewModel: VideoCallViewModel = hiltViewModel()
) {
    val call = viewModel.call
    val context = LocalContext.current
    val primaryColor = Color(0xFF6C63FF)

    LaunchedEffect(key1 = conversationId) {
        viewModel.joinCall(conversationId)
    }

    // Nếu call chưa khởi tạo xong thì hiện Loading
    if (call == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(AppBackgroundColor),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = primaryColor)
        }
        return
    }

    // Xin quyền Camera và Audio từ Android
    LaunchCallPermissions(
        call = call,
        onAllPermissionsGranted = {
            val result = call.join(create = true)
            result.onError {
                Toast.makeText(context, "Lỗi kết nối: ${it.message}", Toast.LENGTH_LONG).show()
                navController.popBackStack()
            }
        }
    )

    // Lắng nghe trạng thái của Mic và Camera
    val isCameraEnabled by call.camera.isEnabled.collectAsState()
    val isMicrophoneEnabled by call.microphone.isEnabled.collectAsState()

    VideoTheme(
        colors = StreamColors.defaultColors().copy(
//            appBackground = AppBackgroundColor,
//            primaryAccent = primaryColor,
            basePrimary = AppBackgroundColor,
            baseSecondary = AppBackgroundColor,
        ),
//        shapes = StreamShapes.defaultShapes().copy(
////            avatar = CircleShape,
//        )
    ) {
        CallContent(
            call = call,
            modifier = Modifier.fillMaxSize(),
            onBackPressed = {
                viewModel.leaveCall()
                navController.popBackStack()
            },
            // TÙY CHỈNH THANH CÔNG CỤ (ĐẢO NỔI)
            controlsContent = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Cụm Đảo Trắng Bo góc chứa các nút
                    Row(
                        modifier = Modifier
                            .shadow(elevation = 12.dp, shape = RoundedCornerShape(32.dp))
                            .background(CardBackgroundColor, RoundedCornerShape(32.dp))
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ToggleMicrophoneAction(
                            modifier = Modifier.size(48.dp),
                            isMicrophoneEnabled = isMicrophoneEnabled,
                            onCallAction = { call.microphone.setEnabled(it.isEnabled) }
                        )
                        ToggleCameraAction(
                            modifier = Modifier.size(48.dp),
                            isCameraEnabled = isCameraEnabled,
                            onCallAction = { call.camera.setEnabled(it.isEnabled) }
                        )
                        FlipCameraAction(
                            modifier = Modifier.size(48.dp),
                            onCallAction = { call.camera.flip() }
                        )
                        // Nút Tắt máy (Màu đỏ mặc định của SDK)
                        CancelCallAction(
                            modifier = Modifier.size(48.dp),
                            onCallAction = {
                                viewModel.leaveCall()
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        )
    }
}