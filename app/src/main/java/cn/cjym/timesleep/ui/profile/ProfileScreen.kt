/**
 * Author: liuzheng <bryant_liu24@126.com>
 */

package cn.cjym.timesleep.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import cn.cjym.timesleep.ui.shared.IOSSlider
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cn.cjym.timesleep.TimeSleepApp
import cn.cjym.timesleep.service.CacheManager
import cn.cjym.timesleep.service.SleepMonitorManager
import cn.cjym.timesleep.ui.shared.AssetCoverImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * "我的" Tab，对应 iOS `ProfileView`：账号、权限、睡眠监测设置、缓存数据与关于。
 * 内部维护一个嵌套 [NavHost] 以支持登录 / 改密 / 协议页面的子导航。
 */
@Composable
fun ProfileScreen(modifier: Modifier = Modifier, onExplore: (String) -> Unit = {}) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home", modifier = modifier) {
        composable("home") {
            ProfileHomeScreen(
                onExplore = onExplore,
                onLogin = { onExplore("login") },
                onSetPassword = { navController.navigate("set_password") },
                onDeleteAccount = { navController.navigate("delete_account") },
                onLegal = { type -> navController.navigate("legal/$type") },
            )
        }
        composable("login") {
            LoginScreen(onBack = { navController.popBackStack() })
        }
        composable("set_password") {
            SetPasswordScreen(onBack = { navController.popBackStack() })
        }
        composable("delete_account") {
            AccountDeletionScreen(
                onBack = { navController.popBackStack() },
                onDeleted = { navController.popBackStack("home", inclusive = false) },
            )
        }
        composable("legal/{type}") { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: "agreement"
            LegalWebViewScreen(type = type, onBack = { navController.popBackStack() })
        }
    }
}

@Composable
private fun ProfileHomeScreen(
    onExplore: (String) -> Unit,
    onLogin: () -> Unit,
    onSetPassword: () -> Unit,
    onDeleteAccount: () -> Unit,
    onLegal: (String) -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as TimeSleepApp
    val scope = rememberCoroutineScope()
    val selectedBlue = Color(0xFF0A84FF)
    val toggleColors = SwitchDefaults.colors(
        checkedTrackColor = selectedBlue,
        checkedBorderColor = selectedBlue,
        checkedThumbColor = Color.White,
        uncheckedThumbColor = Color.White,
    )

    val isLoggedIn by app.settings.isLoggedIn.collectAsState(initial = false)
    val phoneNumber by app.settings.phoneNumber.collectAsState(initial = "")
    val saveAudioClips by app.settings.saveAudioClips.collectAsState(initial = true)
    val sensitivity by app.settings.sensitivity.collectAsState(initial = 0.65)

    var reminderEnabled by remember { mutableStateOf(cn.cjym.timesleep.service.StoryReminder.enabled(context)) }
    val notificationPermission = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { granted ->
        reminderEnabled=granted
        cn.cjym.timesleep.service.StoryReminder.setEnabled(context,granted)
    }
    var cacheSizeText by remember { mutableStateOf("") }
    var showClearAlert by remember { mutableStateOf(false) }

    fun refreshCacheSize() {
        scope.launch(Dispatchers.IO) {
            cacheSizeText = CacheManager.formattedSize(CacheManager.cacheSize(context))
        }
    }

    LaunchedEffect(Unit) {
        refreshCacheSize()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ProfileHeader(
            isLoggedIn = isLoggedIn,
            phoneNumber = phoneNumber,
            onLogin = onLogin,
            onLogout = { scope.launch { app.settings.logout() } },
            onSetPassword = onSetPassword,
        )

        MembershipCard { onExplore("membership") }
        ProfileSection(title = "权限") {
            InfoRow(icon = Icons.Filled.Mic, label = "麦克风", value = "使用时申请")
        }

        ProfileSection(title = "睡眠监测") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "保存识别片段", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Switch(
                    checked = saveAudioClips,
                    onCheckedChange = { value -> scope.launch { app.settings.setSaveAudioClips(value) } },
                    colors = toggleColors,
                )
            }

            Column {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "识别灵敏度", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(
                        text = "${(sensitivity * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IOSSlider(
                    value = sensitivity.toFloat(),
                    onValueChange = { value -> scope.launch { app.settings.setSensitivity(value.toDouble()) } },
                    valueRange = 0.3f..0.95f,
                    tint = selectedBlue,
                )
            }
        }

        ProfileSection(title = "提醒") {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {Text("故事提醒");Text("每天 21:00、22:00 精选故事",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                Switch(checked=reminderEnabled,colors=toggleColors,onCheckedChange={ enabled ->
                    if(enabled && android.os.Build.VERSION.SDK_INT>=33 && androidx.core.content.ContextCompat.checkSelfPermission(context,android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED) notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    else {reminderEnabled=enabled;cn.cjym.timesleep.service.StoryReminder.setEnabled(context,enabled)}
                })
            }
        }
        ProfileSection(title = "数据") {
            InfoRow(icon = Icons.Filled.Storage, label = "数据大小", value = cacheSizeText)
            HorizontalDivider()
            ActionRow(
                icon = Icons.Filled.Delete,
                label = "清除数据",
                onClick = { showClearAlert = true },
                labelColor = Color(0xFFFF453A),
            )
        }

        if (isLoggedIn) {
            ProfileSection(title = "账号与安全") {
                LinkRow(label = "注销账号", onClick = onDeleteAccount, labelColor = MaterialTheme.colorScheme.error)
            }
        }

        ProfileSection(title = "关于") {
            LinkRow(label = "用户协议", onClick = { onLegal("agreement") })
            HorizontalDivider()
            LinkRow(label = "隐私政策", onClick = { onLegal("privacy") })
            HorizontalDivider()
            InfoRow(icon = null, label = "版本", value = cn.cjym.timesleep.BuildConfig.VERSION_NAME)
        }
    }

    if (showClearAlert) {
        AlertDialog(
            onDismissRequest = { showClearAlert = false },
            title = { Text("清除数据？") },
            text = { Text("将删除所有睡眠监测保存的录音声音数据，以及临时缓存文件，不会删除内置声音资源。此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    showClearAlert = false
                    CacheManager.clearCache(context)
                    SleepMonitorManager.clearAllData(context)
                    refreshCacheSize()
                }) { Text("清除") }
            },
            dismissButton = {
                TextButton(onClick = { showClearAlert = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun ProfileHeader(
    isLoggedIn: Boolean,
    phoneNumber: String,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    onSetPassword: () -> Unit,
) {
    var accountMenu by remember { mutableStateOf(false) }
    val background = Brush.horizontalGradient(listOf(Color(0xFF241B18),Color(0xFF3B2920),Color(0xFF5A402B)))
    Row(
        Modifier.fillMaxWidth().heightIn(min = 100.dp)
            .clip(RoundedCornerShape(24.dp)).background(background)
            .clickable { if (isLoggedIn) accountMenu = true else onLogin() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(48.dp).clip(androidx.compose.foundation.shape.CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF7657F5), Color(0xFFC65FD0)))),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Default.AccountCircle, null, Modifier.size(26.dp), tint = Color.White) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (isLoggedIn) (if (phoneNumber.length >= 7) phoneNumber.take(3) + "****" + phoneNumber.takeLast(4) else phoneNumber) else "登录时光睡眠",
                fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
            )
            Text(
                if (isLoggedIn) "账号已登录" else "登录后同步睡眠数据与会员权益",
                fontSize = 11.sp, lineHeight = 16.sp, color = Color(0xFFE3CBB2),
            )
        }
        Text(
            if (isLoggedIn) "账号" else "登录",
            Modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFF0A84FF))
                .padding(horizontal = 12.dp, vertical = 7.dp),
            fontSize = 12.sp, lineHeight = 16.sp, maxLines = 1, color = Color.White,
        )
    }
    if (accountMenu && isLoggedIn) AccountManagementDialog(
        phoneNumber = phoneNumber,
        onDismiss = { accountMenu = false },
        onLogout = { accountMenu = false; onLogout() },
    )
}

@Composable private fun MembershipCard(onClick: () -> Unit) {
    val gold = Color(0xFFEECC9A)
    Column(
        Modifier.fillMaxWidth().heightIn(min = 116.dp).clip(RoundedCornerShape(24.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF241B18), Color(0xFF3B2920), Color(0xFF5A402B))))
            .clickable(onClick = onClick).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(48.dp).clip(androidx.compose.foundation.shape.CircleShape).background(gold),
                contentAlignment = Alignment.Center,
            ) { Text("✦", fontSize = 22.sp, color = Color(0xFF4B2A12)) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("时光睡眠会员", fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFFFF2D2))
                Text("开通会员，解锁全部正念内容", fontSize = 11.sp, lineHeight = 16.sp, color = gold)
            }
            Text(
                "查看权益", Modifier.clip(RoundedCornerShape(16.dp)).background(gold)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                fontSize = 12.sp, lineHeight = 16.sp, maxLines = 1, color = Color(0xFF4F2D16),
            )
        }
        HorizontalDivider(color = Color.White.copy(alpha = .13f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("✦ 全部正念冥想", "✦ 自然声音畅听", "✦ 专享内容更新").forEach {
                Text(it, Modifier.weight(1f), fontSize = 10.sp, lineHeight = 15.sp, color = gold)
            }
        }
    }
}

@Composable
private fun HeaderActionButton(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = Color.White,
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun ProfileSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(
            text = title,
            fontSize = 13.sp,
            color = Color(0xFF999999),
            modifier = Modifier.padding(start = 4.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector?, label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(text = label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = valueColor)
    }
}

@Composable
private fun ActionRow(icon: ImageVector, label: String, onClick: () -> Unit, labelColor: Color = Color.Unspecified) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = labelColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = labelColor, modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LinkRow(label: String, onClick: () -> Unit, labelColor: Color = Color.Unspecified) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = labelColor, modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
