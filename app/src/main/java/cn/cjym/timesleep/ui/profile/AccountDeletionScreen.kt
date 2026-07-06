/**
 * Author: liuzheng <bryant_liu24@126.com>
 */

package cn.cjym.timesleep.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cn.cjym.timesleep.TimeSleepApp
import cn.cjym.timesleep.service.AuthRepository
import cn.cjym.timesleep.service.CacheManager
import cn.cjym.timesleep.service.SleepMonitorManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 注销账号页面，对应 iOS `AccountDeletionView`：验证码确认后永久删除账号与本机数据。 */
@Composable
fun AccountDeletionScreen(onBack: () -> Unit, onDeleted: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as TimeSleepApp
    val scope = rememberCoroutineScope()
    val phoneNumber by app.settings.phoneNumber.collectAsState(initial = "")

    var code by remember { mutableStateOf("") }
    var isSendingCode by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showConfirm by remember { mutableStateOf(false) }
    var showSucceeded by remember { mutableStateOf(false) }

    val isCodeValid = code.length == 6 && code.all { it.isDigit() }

    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown--
        }
    }

    fun deleteAccount() {
        errorMessage = null
        isDeleting = true
        scope.launch {
            try {
                AuthRepository.deleteAccount(phoneNumber, code)
                SleepMonitorManager.clearAllData(context)
                CacheManager.clearCache(context)
                app.settings.clearAccountData()
                showSucceeded = true
            } catch (e: Exception) {
                errorMessage = e.message ?: "注销失败，请稍后重试"
            }
            isDeleting = false
        }
    }

    Scaffold(
        topBar = { ProfileTopBar(title = "账号与安全", onBack = onBack) },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(44.dp),
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "注销账号", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "验证手机号 $phoneNumber 后将永久注销账号",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(text = "注销账号前请确认：", style = MaterialTheme.typography.labelLarge)
                Text(text = "• 账号信息、睡眠记录及设置将被永久删除，无法恢复", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "• 本机保存的监测数据也会一并清除", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "• 注销后该手机号可重新注册新账号", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { value -> code = value.filter { it.isDigit() }.take(6) },
                    label = { Text("验证码") },
                    placeholder = { Text("请输入验证码") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.weight(1f),
                )

                OutlinedButton(
                    enabled = !isSendingCode && countdown <= 0,
                    onClick = {
                        errorMessage = null
                        isSendingCode = true
                        scope.launch {
                            try {
                                AuthRepository.requestVerificationCode(phoneNumber)
                                countdown = 120
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "验证码发送失败，请稍后重试"
                            }
                            isSendingCode = false
                        }
                    },
                    modifier = Modifier.width(96.dp),
                ) {
                    if (isSendingCode) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else if (countdown > 0) {
                        Text("${countdown}s")
                    } else {
                        Text("获取验证码")
                    }
                }
            }

            errorMessage?.let { message ->
                Text(text = message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                enabled = isCodeValid && !isDeleting,
                onClick = { showConfirm = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("注销账号")
                }
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("确认注销账号？") },
            text = { Text("注销后账号及数据将被永久删除，且无法恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    deleteAccount()
                }) { Text("确认注销", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("取消") }
            },
        )
    }

    if (showSucceeded) {
        AlertDialog(
            onDismissRequest = { showSucceeded = false; onDeleted() },
            title = { Text("删除成功") },
            text = { Text("账号已注销") },
            confirmButton = {
                TextButton(onClick = { showSucceeded = false; onDeleted() }) { Text("好") }
            },
        )
    }
}
