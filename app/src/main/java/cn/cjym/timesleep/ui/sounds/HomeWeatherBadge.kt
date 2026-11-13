package cn.cjym.timesleep.ui.sounds

import android.Manifest
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
import cn.cjym.timesleep.service.HomeWeather
import kotlinx.coroutines.launch

@Composable fun HomeWeatherBadge() {
    val context=LocalContext.current
    val weather=remember { HomeWeather(context) }
    val current by weather.current.collectAsState()
    val error by weather.error.collectAsState()
    val loading by weather.loading.collectAsState()
    val scope=rememberCoroutineScope()
    var menu by remember { mutableStateOf(false) }
    val launcher=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if(granted) scope.launch {weather.refresh()} }
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer=LifecycleEventObserver { _, event -> if(event==Lifecycle.Event.ON_RESUME) scope.launch {weather.refresh()} }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) { weather.refresh() }
    val icon=when(current?.code) {
        0 -> if(current?.isDay==1) Icons.Default.WbSunny else Icons.Default.NightsStay
        1,2 -> Icons.Default.WbCloudy
        45,48 -> Icons.Default.BlurOn
        71,73,75,77,85,86 -> Icons.Default.AcUnit
        95,96,99 -> Icons.Default.Thunderstorm
        in 51..82 -> Icons.Default.WaterDrop
        else -> Icons.Default.Cloud
    }
    Box {
        Row(Modifier.heightIn(min=44.dp).semantics { contentDescription=current?.let {"${it.description}，${it.temperatureText}摄氏度"} ?: "开启本地天气定位" }.clickable {
            if(!weather.hasPermission() && !weather.wasAsked()) {weather.markAsked();launcher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)} else menu=true
        },verticalAlignment=Alignment.CenterVertically) {
            Row(Modifier.height(32.dp).clip(CircleShape).background(Color(0xFF1C1C1E)).padding(horizontal=9.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                Icon(icon,null,Modifier.size(16.dp),tint=if((current?.code?:0)<=2) Color(0xFFF5A824) else Color(0xFF4C91DD))
                Text(current?.temperatureText ?: "--°",fontSize=14.sp,fontWeight=FontWeight.SemiBold,color=Color.White)
            }
        }
        DropdownMenu(menu,{menu=false}) {
            if(weather.hasPermission()) DropdownMenuItem({Text(if(loading) "正在更新…" else "刷新天气")},{menu=false;scope.launch {weather.refresh()}},enabled=!loading)
            else DropdownMenuItem({Text("在设置中开启定位")},{menu=false;context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:${context.packageName}")))})
            error?.let { DropdownMenuItem({Text(it)},{},enabled=false) }
            DropdownMenuItem({Text("天气数据来自 Open-Meteo")},{menu=false;context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://open-meteo.com/")))})
        }
    }
}
