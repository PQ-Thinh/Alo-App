package com.example.alo.presentation.call

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.os.Build
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.alo.presentation.helper.NetworkStatus
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.theme.StreamColors
import io.getstream.video.android.compose.ui.components.call.activecall.CallContent
import io.getstream.video.android.compose.ui.components.call.controls.ControlActions
import io.getstream.video.android.compose.ui.components.call.controls.actions.FlipCameraAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.LeaveCallAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleCameraAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleMicrophoneAction
import io.getstream.video.android.core.Call
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun ActiveCallScreen(
    call: Call,
    onCallEnded: () -> Unit,
    networkStatus: NetworkStatus = NetworkStatus.Connected
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(call) {
        val observer = object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val activity = context as? ComponentActivity ?: return
                    if (!activity.isInPictureInPictureMode) {
                        try {
                            val params = PictureInPictureParams.Builder()
                                .setAspectRatio(Rational(9, 16))
                                .build()
                            activity.enterPictureInPictureMode(params)
                        } catch (_: Exception) {}
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val scope = rememberCoroutineScope()
    var camBusy by remember { mutableStateOf(false) }
    var micBusy by remember { mutableStateOf(false) }

    val isCamEnabled by call.camera.isEnabled.collectAsState(initial = call.camera.isEnabled.value)
    val isMicEnabled by call.microphone.isEnabled.collectAsState(initial = call.microphone.isEnabled.value)

    val customColors = StreamColors.defaultColors().copy(
        basePrimary = Color(0xFF0F0C29),
        baseSecondary = Color(0xFF1A1A2E).copy(alpha = 0.8f)
    )

    val participants by call.state.participants.collectAsState()
    val showWaitingMessage = participants.size <= 1

    VideoTheme(colors = customColors) {
        Box(modifier = Modifier.fillMaxSize()) {
            CallContent(
                call = call,
                modifier = Modifier.fillMaxSize(),
                onBackPressed = onCallEnded,
                controlsContent = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(32.dp),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            ControlActions(
                                call = call,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                                actions = listOf(
                                    {
                                        ToggleCameraAction(
                                            modifier = Modifier.size(52.dp),
                                            isCameraEnabled = isCamEnabled,
                                            onCallAction = {
                                                if (camBusy) return@ToggleCameraAction
                                                camBusy = true
                                                scope.launch {
                                                    try {
                                                        call.camera.setEnabled(it.isEnabled)
                                                    } catch (_: Exception) {
                                                    } finally {
                                                        delay(350)
                                                        camBusy = false
                                                    }
                                                }
                                            }
                                        )
                                    },
                                    {
                                        ToggleMicrophoneAction(
                                            modifier = Modifier.size(52.dp),
                                            isMicrophoneEnabled = isMicEnabled,
                                            onCallAction = {
                                                if (micBusy) return@ToggleMicrophoneAction
                                                micBusy = true
                                                scope.launch {
                                                    try {
                                                        call.microphone.setEnabled(it.isEnabled)
                                                    } catch (_: Exception) {
                                                    } finally {
                                                        delay(350)
                                                        micBusy = false
                                                    }
                                                }
                                            }
                                        )
                                    },
                                    {
                                        FlipCameraAction(
                                            modifier = Modifier.size(52.dp),
                                            onCallAction = { call.camera.flip() }
                                        )
                                    },
                                    {
                                        LeaveCallAction(
                                            modifier = Modifier.size(52.dp),
                                            onCallAction = {
                                                call.leave()
                                                onCallEnded()
                                            }
                                        )
                                    }
                                )
                            )
                        }
                    }
                }
            )

            // Giữ lại phần thông báo "Đang đợi đối phương..."
            AnimatedVisibility(
                visible = showWaitingMessage,
                enter = androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Đang đợi đối phương kết nối...",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (networkStatus == NetworkStatus.Reconnecting) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(16.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Surface(
                        color = Color(0xFFD32F2F).copy(alpha = 0.9f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Mất kết nối. Đang thử lại...",
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
