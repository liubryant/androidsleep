package cn.cjym.timesleep.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cn.cjym.timesleep.LegalLinks
import cn.cjym.timesleep.TimeSleepApp
import cn.cjym.timesleep.service.AppSDKManager
import kotlinx.coroutines.launch

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val app = context.applicationContext as TimeSleepApp
    val agreementAccepted by app.settings.agreementAccepted.collectAsState(initial = false)

    LaunchedEffect(agreementAccepted) {
        if (agreementAccepted) {
            AppSDKManager.startIfAllowed(agreementAccepted = true)
        }
    }

    if (agreementAccepted) {
        MainTabScreen()
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            PrivacyAgreementDialog()
        }
    }
}

@Composable
private fun PrivacyAgreementDialog() {
    val context = LocalContext.current
    val app = context.applicationContext as TimeSleepApp
    val scope = rememberCoroutineScope()

    var isChecked by remember { mutableStateOf(false) }
    var showMustCheckAlert by remember { mutableStateOf(false) }
    var showDisagreeAlert by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "个人信息保护",
                    style = MaterialTheme.typography.titleMedium,
                )

                Column(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .heightIn(min = 160.dp, max = 280.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        text = "欢迎使用时光睡眠。我们非常重视您的个人信息与隐私保护。请您在使用前仔细阅读以下协议，了解我们如何为您提供睡眠监测、声音播放、健康数据同步等服务。",
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    Row(modifier = Modifier.padding(top = 12.dp)) {
                        Text(
                            text = "《用户协议》",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { openUrl(context, LegalLinks.USER_AGREEMENT_URL) },
                        )
                        Text(text = " 和 ")
                        Text(
                            text = "《隐私政策》",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { openUrl(context, LegalLinks.PRIVACY_POLICY_URL) },
                        )
                    }

                    Text(
                        text = "在您点击\"同意并继续\"前，我们不会主动请求麦克风、健康数据等敏感权限，也不会开始睡眠声音监测。",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Text(
                        text = "您可以点击上方蓝色协议名称查看完整内容。若您不同意相关条款，将无法继续使用本应用。",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .clickable { isChecked = !isChecked },
                ) {
                    Icon(
                        imageVector = if (isChecked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                        contentDescription = null,
                        tint = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "我已阅读并同意《用户协议》和《隐私政策》",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }

                Button(
                    onClick = {
                        if (!isChecked) {
                            showMustCheckAlert = true
                        } else {
                            scope.launch { app.settings.acceptAgreement() }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                ) {
                    Text("同意并继续")
                }

                TextButton(
                    onClick = { showDisagreeAlert = true },
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text("不同意")
                }
            }
        }
    }

    if (showMustCheckAlert) {
        AlertDialog(
            onDismissRequest = { showMustCheckAlert = false },
            confirmButton = {
                TextButton(onClick = { showMustCheckAlert = false }) { Text("知道了") }
            },
            title = { Text("提示") },
            text = { Text("请先阅读并勾选同意《用户协议》和《隐私政策》。") },
        )
    }

    if (showDisagreeAlert) {
        AlertDialog(
            onDismissRequest = { showDisagreeAlert = false },
            title = { Text("提示") },
            text = { Text("您需要同意《用户协议》和《隐私政策》后才能使用时光睡眠。") },
            confirmButton = {
                TextButton(onClick = {
                    showDisagreeAlert = false
                    openUrl(context, LegalLinks.USER_AGREEMENT_URL)
                }) { Text("查看协议") }
            },
            dismissButton = {
                TextButton(onClick = {
                    (context as? Activity)?.finishAffinity()
                }) { Text("退出App") }
            },
        )
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}
