package com.example.alo.presentation.view.call

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.ringing.RingingCallContent
import com.example.alo.presentation.helper.CallUiState

/**
 * Màn hình cuộc gọi đi (Outgoing call).
 * Dùng RingingCallContent của Stream SDK để hiển thị trạng thái đang ring.
 *
 * @param uiState    Trạng thái hiện tại của cuộc gọi (Initializing, Calling, Error...).
 * @param calleeName Tên người nhận (hiển thị cho đẹp).
 * @param calleeAvatar URL avatar người nhận.
 * @param onCallEnded Callback khi cuộc gọi kết thúc/bị từ chối/hủy.
 */
@Composable
fun OutgoingCallScreen(
    uiState: CallUiState,
    calleeName: String,
    calleeAvatar: String?,
    onCallEnded: () -> Unit
) {
    VideoTheme {
        when (uiState) {
            is CallUiState.Initializing -> {
                // Màn hình loading khi đang kết nối API để tạo cuộc gọi
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1A1A2E)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Đang kết nối...", color = Color.White)
                    }
                }
            }
            is CallUiState.Calling -> {
                val call = uiState.call
                // Stream SDK cung cấp RingingCallContent xử lý cả outgoing ring + incoming accept
            RingingCallContent(
                call = call,
                modifier = Modifier.fillMaxSize(),
                onBackPressed = onCallEnded,
                onAcceptedContent = {
                    // Khi đối phương accept → chuyển sang ActiveCallScreen
                    ActiveCallScreen(call = call, onCallEnded = onCallEnded)
                },
                onRejectedContent = {
                    // Bị từ chối → hiển thị overlay thông báo rồi đóng
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1A1A2E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Cuộc gọi bị từ chối", color = Color.White, fontSize = 20.sp)
                            Spacer(modifier = Modifier.height(24.dp))
                            FilledIconButton(
                                onClick = onCallEnded,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = Color(0xFFE53935)
                                ),
                                modifier = Modifier.size(64.dp)
                            ) {
                                Icon(Icons.Default.CallEnd, contentDescription = "Đóng", tint = Color.White)
                            }
                        }
                    }
                    },
                    onNoAnswerContent = {
                        // Không trả lời → hiển thị thông báo rồi đóng
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1A1A2E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Không có phản hồi", color = Color.White, fontSize = 20.sp)
                            Spacer(modifier = Modifier.height(24.dp))
                            FilledIconButton(
                                onClick = onCallEnded,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = Color(0xFFE53935)
                                ),
                                modifier = Modifier.size(64.dp)
                            ) {
                                Icon(Icons.Default.CallEnd, contentDescription = "Đóng", tint = Color.White)
                            }
                        }
                    }

                })
            }
            is CallUiState.Error -> {
                // Báo lỗi nếu không tạo được cuộc gọi
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1A1A2E)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Lỗi: ${(uiState as CallUiState.Error).message}", color = Color(0xFFE53935))
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onCallEnded) {
                            Text("Quay lại")
                        }
                    }
                }
            }
            else -> {

                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A2E)))
            }

        }
    }
}