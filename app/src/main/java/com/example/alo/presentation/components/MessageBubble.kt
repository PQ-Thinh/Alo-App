package com.example.alo.presentation.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import coil3.compose.AsyncImage
import com.example.alo.domain.model.Message
import com.example.alo.presentation.helper.MessageUiModel
import com.example.alo.presentation.theme.AppBackgroundColor
import com.example.alo.presentation.theme.CardBackgroundColor
import com.example.alo.presentation.theme.TextPrimaryColor
import com.example.alo.presentation.theme.TextSecondaryColor
import com.example.alo.presentation.theme.primaryColor
import com.example.alo.presentation.utils.formatMessageTime

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
    isGroup: Boolean = false,
    senderName: String? = null,
    showSenderName: Boolean = true,
    memberAvatar: String? = null,
    isHighlighted: Boolean = false,
    repliedSenderName: String? = null,
    onMessageClick: () -> Unit,
    onMessageLongClick: () -> Unit,
    onSwipeToReply: () -> Unit = {},
    onReplyClick: (String) -> Unit = {},
    onAvatarClick: (String) -> Unit = {},
    onImageClick: (String) -> Unit = {}
) {
    val context = LocalContext.current


    // Modern rounded corners for grouping
    val bubbleShape = RoundedCornerShape(
        topStart = if (!isMine && !showSenderName) 4.dp else 20.dp,
        topEnd = if (isMine && !showSenderName) 4.dp else 20.dp,
        bottomStart = if (!isMine && !showAvatar) 4.dp else if (!isMine) 4.dp else 20.dp,
        bottomEnd = if (isMine && !showAvatar) 4.dp else if (isMine) 4.dp else 20.dp
    )

    var offsetX by remember { mutableFloatStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (offsetX > 150f || offsetX < -150f) {
                            onSwipeToReply()
                        }
                        offsetX = 0f
                    },
                    onDragCancel = {
                        offsetX = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        // Only allow left drag for my messages, right drag for others
                        val isDragValid = if (isMine) dragAmount < 0 else dragAmount > 0
                        if (isDragValid || offsetX != 0f) {
                            val newOffset = offsetX + dragAmount
                            offsetX = if (isMine) newOffset.coerceIn(-200f, 0f) else newOffset.coerceIn(0f, 200f)
                        }
                    }
                )
            },
        contentAlignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        // Reply Icon Background
        if (animatedOffsetX > 50f && !isMine) {
            Icon(
                Icons.Default.Reply,
                contentDescription = "Reply",
                tint = primaryColor,
                modifier = Modifier
                    .padding(start = 16.dp)
                    .size(24.dp)
                    .alpha((animatedOffsetX / 150f).coerceIn(0f, 1f))
            )
        } else if (animatedOffsetX < -50f && isMine) {
            Icon(
                Icons.Default.Reply,
                contentDescription = "Reply",
                tint = primaryColor,
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(24.dp)
                    .alpha((-animatedOffsetX / 150f).coerceIn(0f, 1f))
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
                .offset { IntOffset(animatedOffsetX.roundToInt(), 0) },
            horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom
        ) {
        if (!isMine) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (showAvatar) primaryColor.copy(alpha = 0.05f) else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                if (showAvatar) {
                    val avatarToDisplay = if (isGroup) memberAvatar else partnerAvatar
                    if (!avatarToDisplay.isNullOrEmpty()) {
                        AsyncImage(
                            model = avatarToDisplay,
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(enabled = showAvatar) {
                                    message.senderId?.let { onAvatarClick(it) }
                                },
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        val nameToDisplay = if (isGroup) (senderName ?: "?") else partnerName
                        Text(
                            text = nameToDisplay.firstOrNull()?.uppercase() ?: "?",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = primaryColor,
                            modifier = Modifier.clickable(enabled = showAvatar) {
                                message.senderId?.let { onAvatarClick(it) }
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
        }

        Column(
            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
        ) {
            val hasReactions = message.reactions.isNotEmpty()
            val isImageMessage = !showRawEncryption && message.messageType == "IMAGE" && message.attachments.isNotEmpty()

            val animatedBorderColor by animateColorAsState(
                targetValue = if (isHighlighted) primaryColor else Color.Transparent,
                animationSpec = tween(durationMillis = 600)
            )

            Box(
                modifier = Modifier.padding(bottom = if (hasReactions) 16.dp else 0.dp)
            ) {
                // --- BUBBLE TIN NHẮN CHÍNH ---
                Box(
                    modifier = Modifier
                        .shadow(
                            elevation = if (isImageMessage) 0.dp else 4.dp, 
                            shape = bubbleShape,
                            spotColor = if (isMine) primaryColor.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.1f)
                        )
                        .clip(bubbleShape)
                        .border(2.dp, animatedBorderColor, bubbleShape)
                        .background(
                            if (isImageMessage) Color.Transparent 
                            else if (isMine) primaryColor 
                            else Color.White
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { onMessageClick() },
                                onLongPress = { onMessageLongClick() }
                            )
                        }
                        .defaultMinSize(minWidth = if (hasReactions) 80.dp else 0.dp)
                        .padding(
                            start = if (isImageMessage) 0.dp else 16.dp,
                            end = if (isImageMessage) 0.dp else 16.dp,
                            top = if (isImageMessage) 0.dp else 12.dp,
                            bottom = if (isImageMessage) { if (hasReactions) 12.dp else 0.dp } else { if (hasReactions) 18.dp else 12.dp }
                        )
                        .widthIn(max = 260.dp)
                ) {
                    Column {
                        // 0. HIỂN THỊ TÊN NGƯỜI GỬI (NẾU LÀ NHÓM VÀ KHÔNG PHẢI MÌNH)
                        if (isGroup && !isMine && senderName != null && showSenderName) {
                            Text(
                                text = senderName,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                color = TextSecondaryColor.copy(alpha = 0.8f),
                                modifier = Modifier
                                    .padding(bottom = 6.dp)
                                    .clickable { message.senderId?.let { onAvatarClick(it) } }
                            )
                        }
                        // 1. NẾU CÓ TRÍCH DẪN -> HIỂN THỊ KHỐI QUOTE
                        if (repliedMessage != null) {
                            val finalRepliedSenderName = repliedSenderName ?: if (repliedMessage.senderId == message.senderId) "Bạn" else partnerName

                            Row(
                                modifier = Modifier
                                    .padding(
                                        start = if (isImageMessage) 6.dp else 0.dp,
                                        end = if (isImageMessage) 6.dp else 0.dp,
                                        top = if (isImageMessage) 6.dp else 0.dp,
                                        bottom = 10.dp
                                    )
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isMine) Color.White.copy(alpha = 0.2f) else primaryColor.copy(alpha = 0.08f))
                                    .clickable { onReplyClick(repliedMessage.id) }
                                    .height(IntrinsicSize.Min)
                                    .wrapContentWidth()
                                    .padding(end = 12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .fillMaxHeight()
                                        .background(if (isMine) Color.White else primaryColor)
                                )

                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                    Text(
                                        text = finalRepliedSenderName,
                                        fontWeight = FontWeight.ExtraBold,
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
                        if (message.messageType == "UPLOADING") {
                            Box(
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(120.dp)
                                    .background(if (isMine) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.05f), bubbleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = if (isMine) Color.White else primaryColor, strokeWidth = 3.dp)
                            }
                        } else if (isImageMessage) {
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
                                        .border(0.5.dp, Color.Black.copy(alpha = 0.05f), bubbleShape)
                                        .clickable { onImageClick(imageUrl) },
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
                                            Icons.Default.ArrowCircleDown,
                                            contentDescription = "Mở File",
                                            tint = if (isMine) Color.White else TextSecondaryColor
                                        )
                                    }
                                }
                            }
                        } else if (!showRawEncryption && message.messageType.startsWith("CALL_")) {
                            // --- HIỂN THỊ TRẠNG THÁI CUỘC GỌI TỪ BẢNG VIDEO_CALLS ---
                            val isMissedOrCancelled = message.messageType in listOf("CALL_MISSED", "CALL_CANCELLED", "CALL_REJECTED")
                            val direction = message.callDirection ?: "outgoing"
                            val isIncoming = direction == "incoming"

                            val callIcon = when (message.messageType) {
                                "CALL_MISSED" -> Icons.Default.CallMissed
                                "CALL_REJECTED" -> Icons.Default.CallEnd
                                "CALL_CANCELLED" -> Icons.Default.CallEnd
                                else -> if (isIncoming) Icons.Default.CallReceived else Icons.Default.CallMade
                            }

                            val iconTint = when {
                                isMissedOrCancelled -> Color(0xFFE53935)
                                isIncoming -> if (isMine) Color.White else Color(0xFF43A047)
                                else -> if (isMine) Color.White else primaryColor
                            }

                            val formattedDuration = message.callDurationSec?.let { totalSec ->
                                String.format("%d:%02d", totalSec / 60, totalSec % 60)
                            }

                            val callText = when (message.messageType) {
                                "CALL_MISSED" -> if (isMine) "Cuộc gọi không trả lời" else "Cuộc gọi nhỡ"
                                "CALL_CANCELLED" -> if (isMine) "Đã hủy cuộc gọi" else "Đối phương đã hủy"
                                "CALL_REJECTED" -> if (isMine) "Bạn đã từ chối" else "Cuộc gọi bị từ chối"
                                "CALL_ENDED" -> {
                                    val prefix = if (isIncoming) "Cuộc gọi đến" else "Cuộc gọi đi"
                                    "$prefix\n⏱ ${formattedDuration ?: "--:--"}"
                                }
                                else -> message.encryptedContent
                            }

                            Row(
                                modifier = Modifier
                                    .widthIn(max = 240.dp)
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (isMine) Color.White.copy(alpha = 0.2f) else iconTint.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = callIcon,
                                        contentDescription = "Call Status",
                                        tint = iconTint,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = callText,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        lineHeight = 18.sp,
                                        color = if (isMine) Color.White else TextPrimaryColor,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        } else {
                            // --- HIỂN THỊ TEXT CHÍNH ---
                            val displayContent = if (showRawEncryption) message.rawEncryptedContent else message.encryptedContent
                            val colorRaw = if (isMine) Color.White else TextPrimaryColor
                            Text(
                                text = displayContent,
                                color = if (showRawEncryption && isMine) Color.White else colorRaw,
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
                    modifier = Modifier.padding(top = 6.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatMessageTime(message.createdAt),
                        fontSize = 11.sp,
                        color = TextSecondaryColor,
                        fontWeight = FontWeight.Medium
                    )
                    if (isMine) {
                        Spacer(modifier = Modifier.width(8.dp))
                        if (message.seenBy.isNotEmpty()) {
                            Surface(
                                modifier = Modifier
                                    .size(16.dp),
                                shape = CircleShape,
                                border = BorderStroke(1.dp, Color.White),
                                shadowElevation = 2.dp
                            ) {
                                if (partnerAvatar.isNotEmpty()) {
                                    AsyncImage(
                                        model = partnerAvatar,
                                        contentDescription = "Seen Avatar",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(modifier = Modifier.fillMaxSize().background(primaryColor.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = partnerName.firstOrNull()?.uppercase() ?: "?",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = primaryColor
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(text = "Đã nhận", fontSize = 11.sp, color = TextSecondaryColor, fontWeight = FontWeight.Bold)
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
}
