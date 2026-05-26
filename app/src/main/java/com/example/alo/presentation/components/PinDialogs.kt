package com.example.alo.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alo.presentation.theme.ErrorColor

@Composable
fun PinSetupDialog(
    onDismissRequest: () -> Unit,
    onPinConfirmed: (String) -> Unit
) {
    var step by remember { mutableStateOf(1) }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(if (step == 1) "Thiết lập mã PIN" else "Xác nhận mã PIN", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text("Nhập mã PIN gồm 6 chữ số để khóa cuộc trò chuyện này.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = if (step == 1) pin else confirmPin,
                    onValueChange = { 
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            if (step == 1) pin = it else confirmPin = it
                            errorMessage = null
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    isError = errorMessage != null,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Mã PIN (6 số)") }
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = ErrorColor,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (step == 1) {
                        if (pin.length == 6) {
                            step = 2
                        } else {
                            errorMessage = "Mã PIN phải đủ 6 số"
                        }
                    } else {
                        if (pin == confirmPin) {
                            onPinConfirmed(pin)
                        } else {
                            errorMessage = "Mã PIN không khớp"
                            confirmPin = ""
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF))
            ) {
                Text(if (step == 1) "Tiếp tục" else "Xác nhận")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Hủy", color = Color.Gray)
            }
        }
    )
}

@Composable
fun PinAuthDialog(
    onDismissRequest: () -> Unit,
    onPinValidated: (String) -> Unit,
    externalError: String? = null
) {
    var pin by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    var errorMessage = externalError ?: localError

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text("Trò chuyện đã bị khóa", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text("Vui lòng nhập mã PIN để xem cuộc trò chuyện này.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { 
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            pin = it
                            localError = null
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    isError = errorMessage != null,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Mã PIN (6 số)") }
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = ErrorColor,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pin.length == 6) {
                        onPinValidated(pin)
                    } else {
                        errorMessage = "Mã PIN phải đủ 6 số"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF))
            ) {
                Text("Mở khóa")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Hủy", color = Color.Gray)
            }
        }
    )
}