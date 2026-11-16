package cn.cjym.timesleep.ui.sounds

import androidx.compose.foundation.background
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
    val scope=rememberCoroutineScope()
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
    Row(
        Modifier.heightIn(min=44.dp).semantics {
            contentDescription=current?.let {"${it.description}，${it.temperatureText}摄氏度"} ?: "暂无天气数据"
        },
        verticalAlignment=Alignment.CenterVertically,
    ) {
        Row(Modifier.height(32.dp).clip(CircleShape).background(Color(0xFF1C1C1E)).padding(horizontal=9.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
            Icon(icon,null,Modifier.size(16.dp),tint=if((current?.code?:0)<=2) Color(0xFFF5A824) else Color(0xFF4C91DD))
            Text(current?.temperatureText ?: "--°",fontSize=14.sp,fontWeight=FontWeight.SemiBold,color=Color.White)
        }
    }
}
