package com.example.alo.presentation.view.call


import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.alo.presentation.theme.primaryColor

@Composable
fun IncomingCallScreen(
    callerName: String,
    callerAvatar: String?,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    var isAccepting by remember { mutableStateOf(false) }

    if (isAccepting) {
        RequestCallPermissions(
            onPermissionsGranted = {
                isAccepting = false
                onAccept()
            }
        )
    } else {
        IncomingCallContent(
            callerName = callerName,
            callerAvatar = callerAvatar,
            onAccept = { isAccepting = true },
            onDecline = onDecline
        )
    }
}

@Composable
private fun IncomingCallContent(
    callerName: String,
    callerAvatar: String?,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F0C29), Color(0xFF302B63), Color(0xFF24243E))
                )
            )
    ) {
        if (!callerAvatar.isNullOrBlank()) {
            AsyncImage(
                model = callerAvatar,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.2f),
                contentScale = ContentScale.Crop
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 100.dp, bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .scale(pulseScale)
                            .alpha(pulseAlpha)
                            .background(primaryColor, CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .scale(pulseScale * 0.7f)
                            .alpha(pulseAlpha * 0.8f)
                            .background(primaryColor, CircleShape)
                    )

                    Surface(
                        modifier = Modifier
                            .size(140.dp)
                            .border(4.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                        shape = CircleShape,
                        color = Color(0xFF2C3E50),
                        shadowElevation = 16.dp
                    ) {
                        if (!callerAvatar.isNullOrBlank()) {
                            AsyncImage(
                                model = callerAvatar,
                                contentDescription = "Avatar $callerName",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = callerName.firstOrNull()?.uppercase() ?: "?",
                                    color = Color.White,
                                    fontSize = 56.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = callerName,
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "ĐANG GỌI VIDEO...",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.sp
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FloatingActionButton(
                        onClick = onDecline,
                        containerColor = Color(0xFFE53935),
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(76.dp)
                    ) {
                        Icon(
                            Icons.Default.CallEnd,
                            contentDescription = "Từ chối",
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "TỪ CHỐI",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FloatingActionButton(
                        onClick = onAccept,
                        containerColor = Color(0xFF43A047),
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(76.dp)
                    ) {
                        Icon(
                            Icons.Default.Call,
                            contentDescription = "Nhấc máy",
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "NHẤC MÁY",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}