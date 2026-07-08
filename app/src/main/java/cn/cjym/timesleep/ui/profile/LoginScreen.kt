/**
 * Author: liuzheng <bryant_liu24@126.com>
 */

package cn.cjym.timesleep.ui.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import cn.cjym.timesleep.LegalLinks
import cn.cjym.timesleep.R
import cn.cjym.timesleep.TimeSleepApp
import cn.cjym.timesleep.service.AuthRepository
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class LoginMode { Code, Password }

/** 登录 / 注册页面，对应 iOS `LoginView`：手机号 + 验证码或密码登录。 */
@Composable
fun LoginScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as TimeSleepApp
    val scope = rememberCoroutineScope()

    var mode by remember { mutableStateOf(LoginMode.Password) }
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSendingCode by remember { mutableStateOf(false) }
    var isLoggingIn by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var agreementChecked by remember { mutableStateOf(false) }

    val isPhoneValid = phone.matches(Regex("^1[3-9]\\d{9}$"))
    val isCodeValid = code.length == 6 && code.all { it.isDigit() }
    val isPasswordValid = password.length >= 6
    val canSubmit = agreementChecked &&
        isPhoneValid &&
        (if (mode == LoginMode.Code) isCodeValid else isPasswordValid) &&
        !isLoggingIn

    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown--
        }
    }

    Scaffold(
        topBar = { ProfileTopBar(title = "登录", onBack = onBack) },
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
            AsyncImage(
                model = "android.resource://${context.packageName}/${R.mipmap.ic_launcher}",
                contentDescription = null,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(16.dp)),
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "登录时光睡眠", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "同步你的睡眠报告、收藏声音和监测设置",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            LoginTextField(
                value = phone,
                onValueChange = { value -> phone = value.filter { it.isDigit() }.take(11) },
                placeholder = { Text("请输入手机号") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
            )

            if (mode == LoginMode.Code) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    LoginTextField(
                        value = code,
                        onValueChange = { value -> code = value.filter { it.isDigit() }.take(6) },
                        placeholder = { Text("请输入验证码") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.weight(1f),
                    )

                    OutlinedButton(
                        enabled = isPhoneValid && !isSendingCode && countdown <= 0,
                        onClick = {
                            errorMessage = null
                            isSendingCode = true
                            scope.launch {
                                try {
                                    AuthRepository.requestVerificationCode(phone)
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
            } else {
                LoginTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("请输入密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            errorMessage?.let { message ->
                Text(text = message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { agreementChecked = !agreementChecked },
            ) {
                Icon(
                    imageVector = if (agreementChecked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                    contentDescription = if (agreementChecked) "已同意协议" else "未同意协议",
                    tint = if (agreementChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "我已阅读并同意",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
                Text(
                    text = "《用户协议》",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        openLegalUrl(context, LegalLinks.USER_AGREEMENT_URL)
                    },
                )
                Text(
                    text = "和",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "《隐私政策》",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        openLegalUrl(context, LegalLinks.PRIVACY_POLICY_URL)
                    },
                )
            }

            Button(
                enabled = canSubmit,
                onClick = {
                    errorMessage = null
                    isLoggingIn = true
                    scope.launch {
                        try {
                            val result = if (mode == LoginMode.Code) {
                                AuthRepository.loginByCode(phone, code)
                            } else {
                                AuthRepository.loginByPassword(phone, password)
                            }
                            app.settings.login(result.phone)
                            onBack()
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "登录失败，请稍后重试"
                        }
                        isLoggingIn = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isLoggingIn) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("登录")
                }
            }

            TextButton(onClick = {
                errorMessage = null
                mode = if (mode == LoginMode.Code) LoginMode.Password else LoginMode.Code
            }) {
                Text(if (mode == LoginMode.Code) "使用密码登录" else "使用验证码登录")
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

private fun openLegalUrl(context: android.content.Context, url: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

@Composable
private fun LoginTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        singleLine = true,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        shape = RoundedCornerShape(8.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
        ),
        modifier = modifier,
    )
}
