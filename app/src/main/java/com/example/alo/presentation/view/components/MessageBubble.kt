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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.example.alo.presentation.theme.CardBackgroundColor
import com.example.alo.presentation.theme.TextPrimaryColor
import com.example.alo.presentation.theme.TextSecondaryColor
import com.example.alo.presentation.theme.primaryColor
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


    // Đảo ngược góc bo tròn tuỳ vào người gửi
    val bubbleShape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (isMine || !showAvatar) 16.dp else 4.dp,
        bottomEnd = if (!isMine || !showAvatar) 16.dp else 4.dp
    )

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
                    .background(if (showAvatar) Color(0xFFE8EAF6) else Color.Transparent),
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
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
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
            val isImageMessage = !showRawEncryption && message.messageType == "IMAGE" && message.attachments.isNotEmpty()

            Box(
                modifier = Modifier.padding(bottom = if (hasReactions) 14.dp else 0.dp)
            ) {
                // --- BUBBLE TIN NHẮN CHÍNH ---
                Box(
                    modifier = Modifier
                        // Bóng đổ nhẹ cho nổi bật, xoá shadow đối với Ảnh vì ảnh tự nổi
                        .shadow(elevation = if (isImageMessage) 0.dp else 2.dp, shape = bubbleShape)
                        .clip(bubbleShape)
                        // Nếu là ảnh: Nền trong suốt. Nếu Text/File: Mình -> Tím, Họ -> Trắng
                        .background(if (isImageMessage) Color.Transparent else if (isMine) primaryColor else CardBackgroundColor)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { onMessageClick() },
                                onLongPress = { onMessageLongClick() }
                            )
                        }
                        .defaultMinSize(minWidth = if (hasReactions) 70.dp else 0.dp)
                        .padding(
                            // Bỏ padding mặc định nếu chỉ hiển thị ảnh để ảnh tràn viền đẹp hơn
                            start = if (isImageMessage) 0.dp else 14.dp,
                            end = if (isImageMessage) 0.dp else 14.dp,
                            top = if (isImageMessage) 0.dp else 10.dp,
                            bottom = if (isImageMessage) { if (hasReactions) 10.dp else 0.dp } else { if (hasReactions) 16.dp else 10.dp }
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
                                    // Thêm padding cho phần Quote
                                    .padding(
                                        start = if (isImageMessage) 4.dp else 0.dp,
                                        end = if (isImageMessage) 4.dp else 0.dp,
                                        top = if (isImageMessage) 4.dp else 0.dp,
                                        bottom = 8.dp
                                    )
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isMine) Color.Black.copy(alpha = 0.15f) else Color(0xFFF0F0F0))
                                    .height(IntrinsicSize.Min)
                                    .wrapContentWidth()
                                    .padding(end = 12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .fillMaxHeight()
                                        // Vạch màu trích dẫn
                                        .background(if (isMine) Color.White.copy(alpha = 0.8f) else primaryColor)
                                )

                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                    Text(
                                        text = repliedSenderName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isMine) Color.White else primaryColor
                                    )

                                    val previewText = when (repliedMessage.messageType) {
                                        "IMAGE" -> "[Hình ảnh]"
                                        "FILE" -> "[Tài liệu] ${repliedMessage.attachments.firstOrNull()?.fileName ?: ""}"
                                        else -> repliedMessage.encryptedContent
                                    }

                                    Text(
                                        text = previewText,
                                        fontSize = 12.sp,
                                        color = if (isMine) Color.White.copy(alpha = 0.9f) else TextSecondaryColor,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // 2. NỘI DUNG TIN NHẮN CHÍNH (TEXT, IMAGE, FILE)
                        if (isImageMessage) {
                            // --- HIỂN THỊ HÌNH ẢNH ---
                            val imageUrl = message.attachments.first().fileUrl
                            Box(contentAlignment = Alignment.Center) {
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = "Hình ảnh đính kèm",
                                    modifier = Modifier
                                        .width(220.dp)
                                        .heightIn(min = 150.dp, max = 300.dp)
                                        // Bo ảnh theo shape của bong bóng chat
                                        .clip(bubbleShape)
                                        // Viền mỏng mờ để chặn ảnh hoà vào nền nếu ảnh màu trắng
                                        .border(0.5.dp, Color.Black.copy(alpha = 0.05f), bubbleShape),
                                    contentScale = ContentScale.Crop
                                )
                                if (MessageUiModel(message, false).isUploading) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .background(Color.Black.copy(alpha = 0.3f), bubbleShape),
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
                                    .background(if (isMine) Color.White.copy(alpha = 0.25f) else Color(0xFFF5F6FA))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background( iconColor.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.InsertDriveFile,
                                        contentDescription = "Tài liệu",
                                        tint = iconColor
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    attachment.fileName?.let {
                                        Text(
                                            text = it,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = if (isMine) Color.White else TextPrimaryColor
                                        )
                                    }
                                    attachment.fileSize?.let {
                                        Text(
                                            text = if (it > 1024 * 1024) "${attachment.fileSize / (1024 * 1024)} MB" else "${attachment.fileSize / 1024} KB",
                                            fontSize = 11.sp,
                                            color = if (isMine) Color.White.copy(alpha = 0.7f) else TextSecondaryColor
                                        )
                                    }
                                }

                                if (MessageUiModel(message, false).isUploading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = if (isMine) Color.White else primaryColor, strokeWidth = 2.dp
                                    )
                                } else {
                                    IconButton(
                                        onClick = {
                                            if (attachment.fileUrl.isNotEmpty()) {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(attachment.fileUrl))
                                                context.startActivity(intent)
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = "Mở File",
                                            tint = if (isMine) Color.White else TextSecondaryColor
                                        )
                                    }
                                }
                            }
                        } else {
                            // --- HIỂN THỊ TEXT CHÍNH ---
                            val displayContent = if (showRawEncryption) message.rawEncryptedContent else message.encryptedContent
                            Text(
                                text = displayContent,
                                color = if (showRawEncryption) Color(0xFFE91E63) else if (isMine) Color.White else TextPrimaryColor,
                                fontSize = if (showRawEncryption) 10.sp else 15.sp,
                                fontFamily = if (showRawEncryption) FontFamily.Monospace else null
                            )
                        }
                    }
                }

                // --- THANH CẢM XÚC (Reactions) ---
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
                            .shadow(elevation = 2.dp, shape = RoundedCornerShape(12.dp))
                            .background(color = CardBackgroundColor, shape = RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
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
                                color = TextSecondaryColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 2.dp)
                            )
                        }
                    }
                }
            }

            // --- THỜI GIAN VÀ TRẠNG THÁI (ĐÃ XEM) ---
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
                        color = TextSecondaryColor
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
                            Text(text = "Đã nhận", fontSize = 11.sp, color = TextSecondaryColor)
                        }
                    }
                }
            }

            // --- HIỂN THỊ THỜI GIAN BÊN NGOÀI KHỐI ---
            if (showTime) {
                Text(
                    text = formatMessageTime(message.createdAt),
                    fontSize = 11.sp,
                    color = TextSecondaryColor,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
                )
            }
        }
    }
}