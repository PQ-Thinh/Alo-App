package com.example.alo.presentation.view.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ChatBottomBar(
    text: String,
    onCancelReply: () -> Unit,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachImage: () -> Unit = {},
    onAttachFile: () -> Unit = {}
) {
    var isAttachmentMenuExpanded by remember { mutableStateOf(false) }
    Surface(color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 8.dp) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp)
                    .imePadding()
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.Bottom
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { isAttachmentMenuExpanded = !isAttachmentMenuExpanded }) {
                        Icon(
                            imageVector = if (isAttachmentMenuExpanded) Icons.Default.Close else Icons.Default.AddCircle,
                            contentDescription = "Mở menu đính kèm",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = isAttachmentMenuExpanded,
                        enter = expandHorizontally() + fadeIn(),
                        exit = shrinkHorizontally() + fadeOut()
                    ) {
                        Row {
                            IconButton(onClick = onAttachImage) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = "Gửi hình ảnh",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = onAttachFile) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = "Gửi tệp",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Ô NHẬP TIN NHẮN
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp),
                    placeholder = { Text("Nhập tin nhắn...") },
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4,
                    trailingIcon = {
                        IconButton(onClick = { /* TODO: Mở Emoji Sticker Picker */ }) {
                            Icon(
                                imageVector = Icons.Default.SentimentSatisfied,
                                contentDescription = "Biểu tượng cảm xúc",
                                tint = Color.Gray
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                // NÚT GỬI (Góc dưới cùng bên phải)
                IconButton(
                    onClick = {
                        onSend()
                        onCancelReply()
                    },
                    enabled = text.trim().isNotEmpty(),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Gửi")
                }
            }
        }
}