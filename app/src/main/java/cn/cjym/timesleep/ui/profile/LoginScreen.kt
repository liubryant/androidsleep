package cn.cjym.timesleep.ui.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.cjym.timesleep.R
import cn.cjym.timesleep.TimeSleepApp
import cn.cjym.timesleep.service.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class LoginMode { Code, Password }
private val LoginAccent = Color(0xFF0A84FF)

@Composable
fun LoginScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as TimeSleepApp
    val scope = rememberCoroutineScope()

    var mode by remember { mutableStateOf(LoginMode.Code) }
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSendingCode by remember { mutableStateOf(false) }
    var isLoggingIn by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var legalDocument by remember { mutableStateOf<String?>(null) }

    val isPhoneValid = phone.matches(Regex("^1[3-9]\\d{9}$"))
    val isCodeValid = code.length == 6 && code.all { it.isDigit() }
    val isPasswordValid = password.length >= 6
    val canSubmit = isPhoneValid &&
        (if (mode == LoginMode.Code) isCodeValid else isPasswordValid) &&
        !isLoggingIn

    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown--
        }
    }

    legalDocument?.let { type ->
        BackHandler { legalDocument = null }
        LegalWebViewScreen(type = type, onBack = { legalDocument = null })
        return
    }

    Scaffold(
        modifier = modifier.imePadding(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Box(Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 4.dp)) {
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart).size(44.dp)) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "返回", Modifier.size(24.dp))
                }
                Text("登录账号", Modifier.align(Alignment.Center), fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 500.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(top = 26.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Image(painterResource(R.drawable.launcher_icon), null, Modifier.size(68.dp).clip(RoundedCornerShape(15.dp)))
                    Text(if (mode == LoginMode.Code) "手机验证码快速登录" else "手机号密码登录",
                        fontSize = 15.sp, lineHeight = 21.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("登录为可选操作，不登录也可以正常使用声音播放与睡眠功能",
                        fontSize = 12.sp, lineHeight = 18.sp, textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    LoginField("手机号", phone, { phone = normalizeLoginPhone(it) }, "请输入手机号",
                        KeyboardType.Phone, prefix = {
                            Text("+86", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LoginAccent)
                            VerticalDivider(Modifier.height(20.dp))
                        })
                    if (mode == LoginMode.Code) {
                        LoginField("验证码", code, { code = it.filter(Char::isDigit).take(6) }, "请输入6位验证码",
                            KeyboardType.NumberPassword, suffix = {
                                VerticalDivider(Modifier.height(20.dp))
                                TextButton(
                                    enabled = isPhoneValid && !isSendingCode && !isLoggingIn && countdown <= 0,
                                    onClick = {
                            errorMessage = null
                            isSendingCode = true
                            scope.launch {
                                try {
                                    AuthRepository.requestVerificationCode(phone)
                                    countdown = 60
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: "验证码发送失败，请稍后重试"
                                }
                                isSendingCode = false
                            }

                                    },
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.width(78.dp).height(44.dp),
                                    colors = ButtonDefaults.textButtonColors(contentColor = LoginAccent,
                                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                                ) {
                                    if (isSendingCode) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                    else Text(if (countdown > 0) "${countdown}s" else "获取验证码",
                                        fontSize = 13.sp, maxLines = 1, fontWeight = FontWeight.SemiBold)
                                }
                            })
                    } else {
                        LoginField("密码", password, { password = it }, "请输入密码（至少6位）",
                            KeyboardType.Password, PasswordVisualTransformation())
                    }
                    errorMessage?.let { Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center) }
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
                            if (result.resolvedAccessToken.isBlank()) throw IllegalStateException("登录成功但未取得账号凭证，请稍后重试")
                            app.settings.login(result.phone, result.resolvedAccessToken)
                            onBack()
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "登录失败，请稍后重试"
                        }
                        isLoggingIn = false
                    }

                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (canSubmit || isLoggingIn) LoginAccent else LoginAccent.copy(alpha = .35f)),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White,
                            disabledContainerColor = Color.Transparent, disabledContentColor = Color.White),
                    ) {
                        if (isLoggingIn) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                        else Text("登录", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        if (mode == LoginMode.Code) "使用密码登录" else "使用验证码登录",
                        modifier = Modifier.clickable {
                            errorMessage = null
                            mode = if (mode == LoginMode.Code) LoginMode.Password else LoginMode.Code
                        },
                        fontSize = 13.sp, lineHeight = 18.sp,
                        fontWeight = FontWeight.SemiBold, color = LoginAccent,
                    )
                }
                Column(
                    Modifier.padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("登录即代表您已阅读并同意", fontSize = 12.sp, lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("《用户协议》", Modifier.clickable { legalDocument = "agreement" },
                            fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold, color = LoginAccent)
                        Text("《隐私政策》", Modifier.clickable { legalDocument = "privacy" },
                            fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold, color = LoginAccent)
                    }
                }
            }
        }
    }
}

private fun normalizeLoginPhone(input: String): String {
    val digits = input.filter(Char::isDigit)
    return when {
        digits.length == 13 && digits.startsWith("86") -> digits.drop(2)
        digits.length == 15 && digits.startsWith("0086") -> digits.drop(4)
        else -> digits.take(11)
    }
}

@Composable
private fun LoginField(
    title: String, value: String, onValueChange: (String) -> Unit, placeholder: String,
    keyboardType: KeyboardType,
    transformation: VisualTransformation = VisualTransformation.None,
    prefix: @Composable RowScope.() -> Unit = {},
    suffix: @Composable RowScope.() -> Unit = {},
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 50.dp).clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1C1C1E)).border(1.dp, Color.White.copy(alpha = .12f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            prefix()
            BasicTextField(value, onValueChange, Modifier.weight(1f).padding(vertical = 14.dp),
                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                visualTransformation = transformation,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 20.sp, color = Color.White),
                cursorBrush = Brush.verticalGradient(listOf(LoginAccent, LoginAccent)),
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) Text(placeholder, fontSize = 15.sp, lineHeight = 20.sp, maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        inner()
                    }
                })
            suffix()
        }
    }
}
