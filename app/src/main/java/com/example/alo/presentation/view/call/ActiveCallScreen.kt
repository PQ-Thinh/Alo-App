package com.example.alo.presentation.view.call

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.ui.components.call.activecall.CallContent
import io.getstream.video.android.compose.ui.components.call.controls.ControlActions
import io.getstream.video.android.compose.ui.components.call.controls.actions.FlipCameraAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.LeaveCallAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleCameraAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleMicrophoneAction
import io.getstream.video.android.core.Call
import io.getstream.video.android.compose.theme.VideoTheme
import com.example.alo.presentation.helper.NetworkStatus
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import android.app.PictureInPictureParams
import android.os.Build
import android.util.Rational
import androidx.activity.ComponentActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Màn hình cuộc gọi đang diễn ra (Active call).
 * Sử dụng Stream SDK `CallContent` — cung cấp video grid tự động,
 * picture-in-picture, và controls.
 *
 * @param call        Stream [Call] đang active.
 * @param onCallEnded Callback khi user nhấn End call.
 */
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
                        val params = PictureInPictureParams.Builder()
                            .setAspectRatio(Rational(9, 16))
                            .build()
                        activity.enterPictureInPictureMode(params)
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

    VideoTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            CallContent(
                call = call,
                modifier = Modifier.fillMaxSize(),
                onBackPressed = onCallEnded,
                controlsContent = {
                    ControlActions(
                        call = call,
                        actions = listOf(
                            {
                                ToggleCameraAction(
                                    modifier = Modifier.size(52.dp),
                                    isCameraEnabled = isCamEnabled as Boolean,
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
                                    isMicrophoneEnabled = isMicEnabled as Boolean,
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
            )
            if (networkStatus == NetworkStatus.Reconnecting) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFB71C1C))
                        .padding(12.dp)
                ) {
                    Text("Đang khôi phục kết nối...", color = Color.White)
                }
            }
        }
    }
}