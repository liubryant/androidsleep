package cn.cjym.timesleep.ui.sounds

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.app.ActivityCompat
import cn.cjym.timesleep.service.HomeWeather
import kotlinx.coroutines.launch

@Composable fun HomeWeatherBadge() {
    val context=LocalContext.current
    val weather=remember { HomeWeather(context) }
    val current by weather.current.collectAsState()
    val error by weather.error.collectAsState()
    var granted by remember { mutableStateOf(weather.hasPermission()) }
    var showSettings by remember { mutableStateOf(false) }
    val scope=rememberCoroutineScope()
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    val permission = Manifest.permission.ACCESS_COARSE_LOCATION
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        granted = allowed
        if (allowed) scope.launch { weather.refresh() }
    }
    fun requestLocation() {
        weather.markAsked()
        launcher.launch(permission)
    }
    DisposableEffect(lifecycle) {
        val observer=LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                granted = weather.hasPermission()
                scope.launch { weather.refresh() }
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) {
        if (!weather.hasPermission() && !weather.wasAsked()) requestLocation()
    }
    val label = current?.temperatureText ?: "--°"
    Row(
        Modifier.heightIn(min=44.dp)
            .clickable {
                if (weather.hasPermission()) {
                    scope.launch { weather.refresh() }
                } else {
                    val activity = context as? Activity
                    if (weather.wasAsked() && activity != null &&
                        !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
                    ) showSettings = true
                    else requestLocation()
                }
            }.semantics {
                contentDescription = current?.let { "${it.description}，${it.temperatureText}摄氏度" }
                    ?: if (granted) error ?: "暂无天气数据" else "开启天气，需要大致位置权限"
            },
        verticalAlignment=Alignment.CenterVertically,
    ) {
        Row(Modifier.height(32.dp).clip(CircleShape).background(Color(0xFF1C1C1E)).padding(horizontal=9.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
            HomeWeatherIcon(current?.code,current?.isDay != 0,Modifier.size(16.dp))
            Text(label,fontSize=14.sp,fontWeight=FontWeight.SemiBold,color=Color.White)
        }
    }
    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("开启天气定位") },
            text = { Text("天气需要读取大致位置。请在系统应用设置中允许位置权限。") },
            confirmButton = {
                TextButton(onClick = {
                    showSettings = false
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)))
                }) { Text("去设置") }
            },
            dismissButton = { TextButton(onClick = { showSettings = false }) { Text("取消") } },
        )
    }
}
