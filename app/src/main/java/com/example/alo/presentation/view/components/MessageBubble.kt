package com.example.alo.presentation.view.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.alo.domain.model.Message
import com.example.alo.presentation.helper.MessageUiModel
import com.example.alo.presentation.view.utils.formatMessageTime

@Composable
fun MessageBubble(
    message: Message,
    repliedMessage: Message?,
    isMine: Boolean,
    partnerAvatar: String,
    partnerName: String,
    showAvatar: Boolean,
    showTime: Boolean,
    showDetails: Boolean,
    showRawEncryption: Boolean = false,
    onMessageClick: () -> Unit,
    onMessageLongClick: () -> Unit
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMine) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (showAvatar) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                if (showAvatar) {
                    if (partnerAvatar.isNotEmpty()) {
                        AsyncImage(
                            model = partnerAvatar,
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = partnerName.firstOrNull()?.uppercase() ?: "?",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
        ) {
            val hasReactions = message.reactions.isNotEmpty()

            Box(
                modifier = Modifier.padding(bottom = if (hasReactions) 14.dp else 0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isMine || !showAvatar) 16.dp else 4.dp,
                                bottomEnd = if (!isMine || !showAvatar) 16.dp else 4.dp
                            )
                        )
                        .background(if (isMine) Color(0xFFE5EFFF) else MaterialTheme.colorScheme.surfaceVariant)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { onMessageClick() },
                                onLongPress = { onMessageLongClick() }
                            )
                        }
                        .defaultMinSize(minWidth = if (hasReactions) 70.dp else 0.dp)
                        .padding(
                            start = if (message.messageType == "IMAGE") 4.dp else 14.dp,
                            end = if (message.messageType == "IMAGE") 4.dp else 14.dp,
                            top = if (message.messageType == "IMAGE") 4.dp else 10.dp,
                            bottom = if (message.messageType == "IMAGE") { if (hasReactions) 10.dp else 4.dp } else { if (hasReactions) 16.dp else 10.dp }
                        )
                        .widthIn(max = 240.dp)
                ) {
                    Column {
                        // 1. NẾU CÓ TRÍCH DẪN -> HIỂN THỊ KHỐI QUOTE
                        if (repliedMessage != null) {
                            val isRepliedMine = repliedMessage.senderId == message.senderId
                            val repliedSenderName = if (isRepliedMine) "Bạn" else partnerName

                            Row(
                                modifier = Modifier
                                    .padding(bottom = 8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.05f))
                                    .height(IntrinsicSize.Min)
                                    .wrapContentWidth()
                                    .padding(end = 12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .fillMaxHeight()
                                        .background(if (isRepliedMine) Color(0xFF6C63FF) else Color(0xFF4CAF50))
                                )

                                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                    Text(
                                        text = repliedSenderName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isRepliedMine) Color(0xFF6C63FF) else Color(0xFF4CAF50)
                                    )

                                    // Thay đổi chữ hiển thị dựa theo loại tin nhắn bị trích dẫn
                                    val previewText = when (repliedMessage.messageType) {
                                        "IMAGE" -> "[Hình ảnh]"
                                        "FILE" -> "[Tài liệu] ${repliedMessage.attachments.firstOrNull()?.fileName ?: ""}"
                                        else -> repliedMessage.encryptedContent
                                    }

                                    Text(
                                        text = previewText,
                                        fontSize = 12.sp,
                                        color = Color.DarkGray,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // 2. NỘI DUNG TIN NHẮN CHÍNH (TEXT, IMAGE, FILE)
                        if (!showRawEncryption && message.messageType == "IMAGE" && message.attachments.isNotEmpty()) {
                            // --- HIỂN THỊ HÌNH ẢNH ---
                            val imageUrl = message.attachments.first().fileUrl
                            Box(contentAlignment = Alignment.Center) {
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = "Hình ảnh đính kèm",
                                    modifier = Modifier
                                        .width(220.dp)
                                        .heightIn(min = 150.dp, max = 300.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                // Hiển thị vòng quay mờ nếu đang tải ảnh
                                if (MessageUiModel(message, true).isUploading) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp)
                                    }
                                }
                            }
                        } else if (!showRawEncryption && message.messageType == "FILE" && message.attachments.isNotEmpty()) {
                            // --- HIỂN THỊ FILE TÀI LIỆU ---
                            val attachment = message.attachments.first()
                            val ext = attachment.fileName?.substringAfterLast('.', "")?.lowercase()
                            val iconColor = when (ext) {
                                "pdf" -> Color(0xFFE53935)
                                "doc", "docx" -> Color(0xFF1E88E5)
                                "xls", "xlsx" -> Color(0xFF43A047)
                                "zip", "rar" -> Color(0xFFFFB300)
                                else -> Color(0xFF757575)
                            }

                            Row(
                                modifier = Modifier
                                    .widthIn(max = 240.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.05f))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(iconColor.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.InsertDriveFile, contentDescription = "Tài liệu", tint = iconColor)
                                }
                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    attachment.fileName?.let {
                                        Text(
                                            text = it,
                                            fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    attachment.fileSize?.let {
                                        Text(
                                            text = if (it > 1024 * 1024) "${attachment.fileSize / (1024 * 1024)} MB" else "${attachment.fileSize / 1024} KB",
                                            fontSize = 11.sp, color = Color.Gray
                                        )
                                    }
                                }

                                if (MessageUiModel(message, true).isUploading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp
                                    )
                                } else {
                                    // ĐÃ TẢI XONG: Hiển thị nút bấm để mở File qua Intent
                                    IconButton(
                                        onClick = {
                                            if (attachment.fileUrl.isNotEmpty()) {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(attachment.fileUrl))
                                                context.startActivity(intent)
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Mở File", tint = Color.Gray)
                                    }
                                }
                            }
                        } else {
                            // --- HIỂN THỊ TEXT ---
                            val displayContent = if (showRawEncryption) message.rawEncryptedContent else message.encryptedContent
                            Text(
                                text = displayContent,
                                color = if (showRawEncryption) Color(0xFFE91E63) else if (isMine) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = if (showRawEncryption) 10.sp else 15.sp,
                                fontFamily = if (showRawEncryption) FontFamily.Monospace else null
                            )
                        }
                    }
                }

                // --- THANH CẢM XÚC ---
                if (hasReactions) {
                    val reactionCounts = message.reactions
                        .groupBy { it.reactionIcon }
                        .mapValues { entry -> entry.value.sumOf { it.count } }

                    val sortedIcons = reactionCounts.entries.sortedByDescending { it.value }.map { it.key }
                    val displayIcons = sortedIcons.take(2)
                    val totalReactions = reactionCounts.values.sum()

                    Row(
                        modifier = Modifier
                            .align(if (isMine) Alignment.BottomStart else Alignment.BottomEnd)
                            .offset(x = if (isMine) 10.dp else (-10).dp, y = 12.dp)
                            .background(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                            .clickable { },
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        displayIcons.forEach { icon ->
                            Text(text = icon, fontSize = 12.sp, modifier = Modifier.offset(y = (-1).dp))
                        }

                        if (totalReactions > 1) {
                            Text(
                                text = totalReactions.toString(),
                                fontSize = 11.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 2.dp)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showDetails,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -10 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -10 })
            ) {
                Row(
                    modifier = Modifier.padding(top = 4.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatMessageTime(message.createdAt),
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    if (isMine) {
                        Spacer(modifier = Modifier.width(8.dp))
                        if (message.seenBy.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(Color.LightGray),
                                contentAlignment = Alignment.Center
                            ) {
                                if (partnerAvatar.isNotEmpty()) {
                                    AsyncImage(
                                        model = partnerAvatar,
                                        contentDescription = "Seen Avatar",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        text = partnerName.firstOrNull()?.uppercase() ?: "?",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        } else {
                            Text(text = "Đã nhận", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }

            if (showTime) {
                Text(
                    text = formatMessageTime(message.createdAt),
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
                )
            }
        }
    }
}