package com.example.alo.presentation.view.call

import androidx.compose.runtime.Composable
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
import io.getstream.video.android.core.call.state.ToggleMicrophone
import kotlinx.coroutines.flow.StateFlow

/**
 * Màn hình cuộc gọi đang diễn ra (Active call).
 * Sử dụng Stream SDK `CallContent` — cung cấp video grid tự động,
 * picture-in-picture, và controls.
 *
 * @param call        Stream [Call] đang active.
 * @param onCallEnded Callback khi user nhấn End call.
 */
@Composable
fun ActiveCallScreen(
    call: Call,
    onCallEnded: () -> Unit
) {
    VideoTheme {
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
                                isCameraEnabled = call.camera.isEnabled.value,
                                onCallAction = { call.camera.setEnabled(it.isEnabled) }
                            )
                        },
                        {
                            ToggleMicrophoneAction(
                                modifier = Modifier.size(52.dp),
                                isMicrophoneEnabled = call.microphone.isEnabled.value,
                                onCallAction = { call.microphone.setEnabled(it.isEnabled) }
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
    }
}