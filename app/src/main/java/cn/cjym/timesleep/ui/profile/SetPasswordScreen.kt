/**
 * Author: liuzheng <bryant_liu24@126.com>
 */

package cn.cjym.timesleep.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import cn.cjym.timesleep.TimeSleepApp
import cn.cjym.timesleep.service.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 修改登录密码页面，对应 iOS `SetPasswordView`：验证码 + 新密码。 */
@Composable
fun SetPasswordScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as TimeSleepApp
    val scope = rememberCoroutineScope()
    val phoneNumber by app.settings.phoneNumber.collectAsState(initial = "")

    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSendingCode by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val isCodeValid = code.length == 6 && code.all { it.isDigit() }
    val isPasswordValid = password.length >= 6

    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown--
        }
    }

    Scaffold(
        topBar = { ProfileTopBar(title = "修改密码", onBack = onBack) },
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
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "修改登录密码", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "验证手机号 $phoneNumber 后修改密码，之后可使用密码登录",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                        successMessage = null
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

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("新密码") },
                placeholder = { Text("请输入新密码（至少6位）") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )

            errorMessage?.let { message ->
                Text(text = message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            successMessage?.let { message ->
                Text(text = message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }

            Button(
                enabled = isCodeValid && isPasswordValid && !isSubmitting,
                onClick = {
                    errorMessage = null
                    successMessage = null
                    isSubmitting = true
                    scope.launch {
                        try {
                            AuthRepository.setPassword(phoneNumber, code, password)
                            successMessage = "密码设置成功"
                            code = ""
                            password = ""
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "设置失败，请稍后重试"
                        }
                        isSubmitting = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("确认设置")
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
