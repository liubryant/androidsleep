package cn.cjym.timesleep.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cn.cjym.timesleep.TimeSleepApp
import cn.cjym.timesleep.service.CacheManager
import cn.cjym.timesleep.service.HealthConnectManager
import cn.cjym.timesleep.service.SleepMonitorManager
import cn.cjym.timesleep.ui.shared.AssetCoverImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * "我的" Tab，对应 iOS `ProfileView`：账号、权限、睡眠监测设置、缓存数据与关于。
 * 内部维护一个嵌套 [NavHost] 以支持登录 / 改密 / 协议页面的子导航。
 */
@Composable
fun ProfileScreen(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home", modifier = modifier) {
        composable("home") {
            ProfileHomeScreen(
                onLogin = { navController.navigate("login") },
                onSetPassword = { navController.navigate("set_password") },
                onLegal = { type -> navController.navigate("legal/$type") },
            )
        }
        composable("login") {
            LoginScreen(onBack = { navController.popBackStack() })
        }
        composable("set_password") {
            SetPasswordScreen(onBack = { navController.popBackStack() })
        }
        composable("legal/{type}") { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: "agreement"
            LegalWebViewScreen(type = type, onBack = { navController.popBackStack() })
        }
    }
}

@Composable
private fun ProfileHomeScreen(
    onLogin: () -> Unit,
    onSetPassword: () -> Unit,
    onLegal: (String) -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as TimeSleepApp
    val scope = rememberCoroutineScope()

    val isLoggedIn by app.settings.isLoggedIn.collectAsState(initial = false)
    val phoneNumber by app.settings.phoneNumber.collectAsState(initial = "")
    val saveAudioClips by app.settings.saveAudioClips.collectAsState(initial = true)
    val sensitivity by app.settings.sensitivity.collectAsState(initial = 0.65)

    val isHealthAuthorized by HealthConnectManager.isAuthorized.collectAsState()
    val healthStatusText by HealthConnectManager.statusText.collectAsState()

    var cacheSizeText by remember { mutableStateOf("") }
    var showClearAlert by remember { mutableStateOf(false) }

    fun refreshCacheSize() {
        scope.launch(Dispatchers.IO) {
            cacheSizeText = CacheManager.formattedSize(CacheManager.cacheSize(context))
        }
    }

    LaunchedEffect(Unit) {
        HealthConnectManager.refreshAuthorization(context)
        refreshCacheSize()
    }

    val healthPermissionLauncher = rememberLauncherForActivityResult(
        contract = HealthConnectManager.requestPermissionsContract(),
    ) { granted -> HealthConnectManager.onPermissionResult(granted) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        ProfileHeader(
            isLoggedIn = isLoggedIn,
            phoneNumber = phoneNumber,
            onLogin = onLogin,
            onLogout = { scope.launch { app.settings.logout() } },
            onSetPassword = onSetPassword,
        )

        ProfileSection(title = "权限") {
            InfoRow(icon = Icons.Filled.Mic, label = "麦克风", value = "使用时申请")
            InfoRow(
                icon = if (isHealthAuthorized) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                label = "Health Connect",
                value = healthStatusText,
                valueColor = if (isHealthAuthorized) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = { healthPermissionLauncher.launch(HealthConnectManager.permissionsToRequest()) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("授权 Health Connect")
            }
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
                Slider(
                    value = sensitivity.toFloat(),
                    onValueChange = { value -> scope.launch { app.settings.setSensitivity(value.toDouble()) } },
                    valueRange = 0.3f..0.95f,
                )
            }
        }

        ProfileSection(title = "数据") {
            InfoRow(icon = Icons.Filled.Storage, label = "数据大小", value = cacheSizeText)
            OutlinedButton(
                onClick = { showClearAlert = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(imageVector = Icons.Filled.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("清除数据")
            }
        }

        ProfileSection(title = "关于") {
            LinkRow(label = "用户协议", onClick = { onLegal("agreement") })
            HorizontalDivider()
            LinkRow(label = "隐私政策", onClick = { onLegal("privacy") })
            HorizontalDivider()
            InfoRow(icon = null, label = "版本", value = "1.0.0")
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .clip(RoundedCornerShape(8.dp)),
    ) {
        AssetCoverImage(assetPath = "SoundResources/010_热带雨林/cover.jpg", modifier = Modifier.fillMaxSize())

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.05f), Color.Black.copy(alpha = 0.58f)),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(50.dp),
                )

                Column {
                    Text(
                        text = if (isLoggedIn) phoneNumber else "未登录",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                    Text(
                        text = if (isLoggedIn) "睡眠目标：每天 8 小时" else "登录后同步睡眠记录和设置",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.86f),
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (isLoggedIn) {
                    HeaderActionButton(text = "退出登录", onClick = onLogout)
                    HeaderActionButton(text = "修改密码", onClick = onSetPassword)
                } else {
                    HeaderActionButton(text = "登录 / 注册", onClick = onLogin)
                }
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
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .padding(14.dp),
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
private fun LinkRow(label: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
