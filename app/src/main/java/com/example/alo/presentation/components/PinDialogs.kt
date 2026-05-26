package com.example.alo.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.alo.presentation.theme.ErrorColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val PinPrimary = Color(0xFF6C63FF)
private val PinPrimaryLight = Color(0xFF8B83FF)
private val PinSurface = Color(0xFFF8F9FC)
private val PinDotEmpty = Color(0xFFD1D5DB)
private val PinDotFilled = Color(0xFF6C63FF)
private val PinKeyBg = Color(0xFFF1F3F8)
private val PinKeyPressed = Color(0xFFE0DFFE)

// ═══════════════════════════════════════════════════════
// PinSetupDialog – Thiết lập mã PIN mới (2 bước)
// ═══════════════════════════════════════════════════════
@Composable
fun PinSetupDialog(
    onDismissRequest: () -> Unit,
    onPinConfirmed: (String) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSuccess by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val currentPin = if (step == 1) pin else confirmPin

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            shadowElevation = 16.dp,
            tonalElevation = 4.dp
        ) {
            if (showSuccess) {
                // Hiệu ứng xác nhận thành công
                PinSuccessContent(
                    message = "Đã thiết lập khóa!",
                    onFinish = { onPinConfirmed(pin) }
                )
            } else {
                Column(
                    modifier = Modifier.padding(top = 28.dp, bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Icon
                    PinHeaderIcon(
                        icon = Icons.Default.Shield,
                        gradientColors = listOf(PinPrimary, PinPrimaryLight)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Title với animation chuyển bước
                    AnimatedContent(
                        targetState = step,
                        transitionSpec = {
                            (slideInVertically { it / 2 } + fadeIn()) togetherWith
                                    (slideOutVertically { -it / 2 } + fadeOut())
                        },
                        label = "step_title"
                    ) { currentStep ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (currentStep == 1) "Thiết lập mã PIN" else "Xác nhận mã PIN",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (currentStep == 1)
                                    "Nhập 6 chữ số để bảo vệ cuộc trò chuyện"
                                else
                                    "Nhập lại mã PIN để xác nhận",
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Step indicator
                    PinStepIndicator(currentStep = step)

                    Spacer(modifier = Modifier.height(20.dp))

                    // PIN Dots
                    PinDotRow(
                        filledCount = currentPin.length,
                        totalDots = 6,
                        hasError = errorMessage != null
                    )

                    // Error message
                    AnimatedVisibility(visible = errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            color = ErrorColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Numeric Keypad
                    PinKeypad(
                        onDigit = { digit ->
                            errorMessage = null
                            if (currentPin.length < 6) {
                                if (step == 1) pin += digit else confirmPin += digit
                            }
                            // Auto-submit khi đủ 6 số
                            val updatedPin = if (step == 1) pin else confirmPin
                            if (updatedPin.length == 6) {
                                scope.launch {
                                    delay(200) // Cho user thấy dot cuối được fill
                                    if (step == 1) {
                                        step = 2
                                    } else {
                                        if (pin == confirmPin) {
                                            showSuccess = true
                                        } else {
                                            errorMessage = "Mã PIN không khớp, hãy thử lại"
                                            confirmPin = ""
                                        }
                                    }
                                }
                            }
                        },
                        onBackspace = {
                            if (step == 1 && pin.isNotEmpty()) {
                                pin = pin.dropLast(1)
                            } else if (step == 2 && confirmPin.isNotEmpty()) {
                                confirmPin = confirmPin.dropLast(1)
                            }
                            errorMessage = null
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Cancel button
                    TextButton(onClick = onDismissRequest) {
                        Text(
                            "Hủy bỏ",
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// PinAuthDialog – Nhập PIN để mở khóa / xác thực
// ═══════════════════════════════════════════════════════
@Composable
fun PinAuthDialog(
    onDismissRequest: () -> Unit,
    onPinValidated: (String) -> Unit,
    externalError: String? = null
) {
    var pin by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    val errorMessage = externalError ?: localError
    val scope = rememberCoroutineScope()

    // Reset pin khi có external error (PIN sai)
    LaunchedEffect(externalError) {
        if (externalError != null) {
            delay(600)
            pin = ""
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            shadowElevation = 16.dp,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(top = 28.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon – Lock với animation lắc lư
                PinHeaderIcon(
                    icon = Icons.Default.Lock,
                    gradientColors = listOf(Color(0xFFFF6B6B), Color(0xFFEE5A24))
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Cuộc trò chuyện bị khóa",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Nhập mã PIN để tiếp tục",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // PIN Dots
                PinDotRow(
                    filledCount = pin.length,
                    totalDots = 6,
                    hasError = errorMessage != null
                )

                // Error message
                AnimatedVisibility(visible = errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = ErrorColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Numeric Keypad
                PinKeypad(
                    onDigit = { digit ->
                        localError = null
                        if (pin.length < 6) {
                            pin += digit
                        }
                        if (pin.length == 6) {
                            scope.launch {
                                delay(200)
                                onPinValidated(pin)
                            }
                        }
                    },
                    onBackspace = {
                        if (pin.isNotEmpty()) {
                            pin = pin.dropLast(1)
                        }
                        localError = null
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = onDismissRequest) {
                    Text(
                        "Hủy bỏ",
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// Shared Components
// ═══════════════════════════════════════════════════════

/** Header icon tròn với gradient */
@Composable
private fun PinHeaderIcon(
    icon: ImageVector,
    gradientColors: List<Color>
) {
    val infiniteTransition = rememberInfiniteTransition(label = "icon_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .size(64.dp)
            .scale(pulse)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(gradientColors)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(30.dp)
        )
    }
}

/** Step indicator (bước 1/2) cho PinSetupDialog */
@Composable
private fun PinStepIndicator(currentStep: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(2) { index ->
            val step = index + 1
            val isActive = step == currentStep
            val isDone = step < currentStep

            val width by animateFloatAsState(
                targetValue = if (isActive) 24f else 8f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "step_width"
            )

            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(width.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isActive -> PinPrimary
                            isDone -> PinPrimary.copy(alpha = 0.4f)
                            else -> PinDotEmpty
                        }
                    )
            )
        }
    }
}

/** Hàng hiển thị 6 chấm PIN */
@Composable
private fun PinDotRow(
    filledCount: Int,
    totalDots: Int,
    hasError: Boolean
) {
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(hasError) {
        if (hasError) {
            // Hiệu ứng rung lắc khi sai
            repeat(3) {
                shakeOffset.animateTo(10f, tween(50))
                shakeOffset.animateTo(-10f, tween(50))
            }
            shakeOffset.animateTo(0f, tween(50))
        }
    }

    Row(
        modifier = Modifier
            .graphicsLayer { translationX = shakeOffset.value },
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalDots) { index ->
            val isFilled = index < filledCount
            val dotScale by animateFloatAsState(
                targetValue = if (isFilled) 1f else 0.75f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "dot_scale"
            )

            val dotColor = when {
                hasError -> ErrorColor
                isFilled -> PinDotFilled
                else -> PinDotEmpty
            }

            Box(
                modifier = Modifier
                    .size(16.dp)
                    .scale(dotScale)
                    .clip(CircleShape)
                    .background(dotColor)
                    .then(
                        if (!isFilled && !hasError) {
                            Modifier.border(1.5.dp, PinDotEmpty.copy(alpha = 0.6f), CircleShape)
                        } else {
                            Modifier
                        }
                    )
            )
        }
    }
}

/** Bàn phím số tùy chỉnh */
@Composable
private fun PinKeypad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "⌫")
    )

    Column(
        modifier = Modifier.padding(horizontal = 36.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { key ->
                    if (key.isEmpty()) {
                        // Ô trống
                        Spacer(modifier = Modifier.size(64.dp))
                    } else {
                        PinKey(
                            label = key,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (key == "⌫") onBackspace() else onDigit(key)
                            }
                        )
                    }
                }
            }
        }
    }
}

/** Một phím trên bàn phím số */
@Composable
private fun PinKey(
    label: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "key_scale"
    )

    val isBackspace = label == "⌫"

    Box(
        modifier = Modifier
            .size(64.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(if (isPressed) PinKeyPressed else PinKeyBg)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isBackspace) {
            Icon(
                imageVector = Icons.Default.Backspace,
                contentDescription = "Xóa",
                tint = Color(0xFF64748B),
                modifier = Modifier.size(22.dp)
            )
        } else {
            Text(
                text = label,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1E293B)
            )
        }
    }
}

/** Hiệu ứng thành công sau khi setup PIN */
@Composable
private fun PinSuccessContent(
    message: String,
    onFinish: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(1200)
        onFinish()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "success_pulse")
    val successScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "success_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .scale(successScale)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF10B981), Color(0xFF34D399))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF10B981)
        )
    }
}
