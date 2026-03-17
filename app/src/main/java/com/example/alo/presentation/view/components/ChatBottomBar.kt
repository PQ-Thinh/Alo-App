package com.example.alo.presentation.view.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alo.domain.model.Message
import com.example.alo.presentation.theme.AppBackgroundColor
import com.example.alo.presentation.theme.CardBackgroundColor
import com.example.alo.presentation.theme.TextPrimaryColor
import com.example.alo.presentation.theme.TextSecondaryColor

@Composable
fun ChatBottomBar(
    text: String,
    replyingToMessage: Message?,
    partnerName: String,
    currentUserId: String,
    onCancelReply: () -> Unit,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachImage: () -> Unit = {},
    onAttachFile: () -> Unit = {}
) {
    var isAttachmentMenuExpanded by remember { mutableStateOf(false) }
    val primaryColor = Color(0xFF6C63FF)
    val hasText = text.trim().isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppBackgroundColor) // Màu nền tổng thể xám nhạt để làm nổi đảo
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .imePadding()
            .navigationBarsPadding()
    ) {
        // --- ĐẢO REPLY (Nổi lên trên nếu có) ---
        AnimatedVisibility(visible = replyingToMessage != null) {
            if (replyingToMessage != null) {
                val isReplyingToMe = replyingToMessage.senderId == currentUserId
                val replyName = if (isReplyingToMe) "Bạn" else partnerName
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBackgroundColor) // Đảo trắng
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Reply,
                        contentDescription = null,
                        tint = primaryColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Đang trả lời $replyName",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = primaryColor
                        )
                        Text(
                            text = replyingToMessage.encryptedContent,
                            fontSize = 12.sp,
                            color = TextSecondaryColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onCancelReply, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Hủy", tint = TextSecondaryColor)
                    }
                }
            }
        }

        // --- HÀNG CHỨA 3 ĐẢO ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp) // Khoảng cách giữa các đảo
        ) {

            // --- ĐẢO 1: MENU ĐÍNH KÈM (TRÁI) ---
            Row(
                modifier = Modifier
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(CardBackgroundColor) // Đảo trắng
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { isAttachmentMenuExpanded = !isAttachmentMenuExpanded }) {
                    Icon(
                        imageVector = if (isAttachmentMenuExpanded) Icons.Default.Close else Icons.Default.AddCircle,
                        contentDescription = "Mở menu đính kèm",
                        tint = primaryColor,
                        modifier = Modifier.size(28.dp)
                    )
                }

                AnimatedVisibility(
                    visible = isAttachmentMenuExpanded,
                    enter = expandHorizontally() + fadeIn(),
                    exit = shrinkHorizontally() + fadeOut()
                ) {
                    Row(modifier = Modifier.padding(end = 4.dp)) {
                        IconButton(onClick = onAttachImage, modifier = Modifier.size(40.dp)) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Gửi hình ảnh",
                                tint = primaryColor
                            )
                        }
                        IconButton(onClick = onAttachFile, modifier = Modifier.size(40.dp)) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = "Gửi tệp",
                                tint = primaryColor
                            )
                        }
                    }
                }
            }

            // --- ĐẢO 2: Ô NHẬP TIN NHẮN (GIỮA) ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(CardBackgroundColor), // Đảo trắng
                contentAlignment = Alignment.Center
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Nhập tin nhắn...", color = TextSecondaryColor) },
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4,
                    trailingIcon = {
                        IconButton(onClick = { /* TODO: Mở Emoji Sticker Picker */ }) {
                            Icon(
                                imageVector = Icons.Default.SentimentSatisfied,
                                contentDescription = "Biểu tượng cảm xúc",
                                tint = TextSecondaryColor
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimaryColor,
                        unfocusedTextColor = TextPrimaryColor,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        disabledBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
            }

            // --- ĐẢO 3: NÚT GỬI (PHẢI) ---
            Box(
                modifier = Modifier
                    .size(52.dp) // Hình tròn đều
                    .shadow(elevation = 4.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(if (hasText) primaryColor else CardBackgroundColor) // Đổi màu nền nếu có text
                    .clickable(enabled = hasText) {
                        onSend()
                        onCancelReply()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Gửi",
                    tint = if (hasText) Color.White else TextSecondaryColor, // Icon trắng nếu có text, xám nếu rỗng
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}