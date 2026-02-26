package com.example.alo.presentation.view.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.alo.presentation.view.navigation.Screen
import kotlinx.coroutines.launch

@Composable
fun IntroScreen(navController: NavController) {
    // Khởi tạo các biến Animation cho Logo và Text
    val logoScale = remember { Animatable(0f) }
    val contentAlpha = remember { Animatable(0f) }
    val buttonOffsetY = remember { Animatable(50f) }

    // Hàm tạo vòng lặp sóng lan tỏa (Ripple Effect)
    @Composable
    fun waveProgress(delayMillis: Int): State<Float> {
        val infiniteTransition = rememberInfiniteTransition(label = "wave_$delayMillis")
        return infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = LinearOutSlowInEasing),
                repeatMode = RepeatMode.Restart,
                initialStartOffset = StartOffset(delayMillis)
            ),
            label = "wave_progress"
        )
    }

    // Tạo 3 lớp sóng lệch nhịp nhau
    val wave1 by waveProgress(0)
    val wave2 by waveProgress(1000)
    val wave3 by waveProgress(2000)

    // 3. Kích hoạt Animation khi màn hình mở lên
    LaunchedEffect(Unit) {
        launch {
            // Hiệu ứng nảy (Bouncy) cho Logo
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            // Fade in nội dung
            contentAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1000, delayMillis = 500)
            )
        }
        launch {
            // Nút trượt từ dưới lên
            buttonOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 1000, delayMillis = 500, easing = FastOutSlowInEasing)
            )
        }
    }

    // 4. Vẽ Giao diện
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E1E2C), Color(0xFF2D2D44))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // --- TẦNG DƯỚI: CANVAS VẼ SÓNG ---
        Canvas(modifier = Modifier.fillMaxSize()) {
            val maxRadius = size.minDimension
            val waveColor = Color(0xFF6C63FF)

            drawCircle(
                color = waveColor.copy(alpha = (1f - wave1) * 0.5f),
                radius = wave1 * maxRadius
            )
            drawCircle(
                color = waveColor.copy(alpha = (1f - wave2) * 0.5f),
                radius = wave2 * maxRadius
            )
            drawCircle(
                color = waveColor.copy(alpha = (1f - wave3) * 0.5f),
                radius = wave3 * maxRadius
            )
        }

        // --- TẦNG GIỮA: LOGO & TITLE ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(logoScale.value)
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF6C63FF), Color(0xFF9D95FF))),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Forum,
                    contentDescription = "Logo",
                    tint = Color.White,
                    modifier = Modifier.size(50.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "ALO",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 8.sp,
                    color = Color.White
                ),
                modifier = Modifier.alpha(contentAlpha.value)
            )

            Text(
                text = "Kết nối không giới hạn",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.LightGray,
                    letterSpacing = 1.sp
                ),
                modifier = Modifier.alpha(contentAlpha.value)
            )
        }

        // --- TẦNG TRÊN CÙNG: NÚT BUTTON ---
        Button(
            onClick = {
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Intro.route) { inclusive = true }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            shape = RoundedCornerShape(percent = 50),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
                .fillMaxWidth(0.7f)
                .height(56.dp)
                .offset(y = buttonOffsetY.value.dp)
                .alpha(contentAlpha.value)
        ) {
            Text(
                text = "BẮT ĐẦU NGAY",
                color = Color(0xFF2D2D44),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}