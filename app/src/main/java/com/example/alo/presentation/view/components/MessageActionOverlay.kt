package com.example.alo.presentation.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alo.domain.model.Message

@Composable
fun MessageActionOverlay(
    message: Message,
    isMine: Boolean,
    onDismiss: () -> Unit,
    onReply: () -> Unit,
    onCopy: () -> Unit,
    onReactionSelect: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)) // Làm tối nền
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss // Nhấn ra ngoài nền đen để đóng
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            // Căn lề tùy thuộc vào tin nhắn của mình hay của đối tác
            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
        ) {
            // 1. BONG BÓNG TIN NHẮN ĐANG CHỌN (Nổi bật lên)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isMine) Color(0xFFE5EFFF) else MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .widthIn(max = 260.dp)
            ) {
                Text(
                    text = message.encryptedContent,
                    color = if (isMine) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. THANH CẢM XÚC
            ReactionBar(onReactionSelected = onReactionSelect)

            Spacer(modifier = Modifier.height(16.dp))

            // 3. MENU TÙY CHỌN (COPY, REPLY)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.width(200.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onReply() }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Reply, contentDescription = "Reply")
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Trả lời", fontSize = 16.sp)
                    }

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCopy() }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Sao chép", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}