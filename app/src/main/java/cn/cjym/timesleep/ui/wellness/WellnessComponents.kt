package cn.cjym.timesleep.ui.wellness

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.shape.CircleShape
import coil.compose.AsyncImage

fun coverPath(index: Int) = "file:///android_asset/SoundResources/Mindfulness/Covers/cover-%02d.jpg".format(index)

@Composable fun SceneryBackground(cover: Int, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.fillMaxSize().background(Color(0xFF111315))) {
        AsyncImage(coverPath(cover),null,Modifier.matchParentSize(),contentScale=ContentScale.Crop)
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Black.copy(.55f),Color.Black.copy(.82f)))))
        content()
    }
}

@Composable fun SceneryCard(title: String, subtitle: String, cover: Int, modifier: Modifier = Modifier, badge: String? = null, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(20.dp)).clickable(onClick=onClick).background(Color(0xFF202428))) {
        AsyncImage(coverPath(cover),null,Modifier.matchParentSize(),contentScale=ContentScale.Crop)
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent,Color.Black.copy(.78f)))))
        badge?.let { Surface(color=Color.Black.copy(.55f),shape=RoundedCornerShape(20.dp),modifier=Modifier.align(Alignment.TopEnd).padding(12.dp)) { Text(it,Modifier.padding(horizontal=9.dp,vertical=5.dp),style=MaterialTheme.typography.labelSmall,color=Color.White) } }
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Text(title,color=Color.White,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold,maxLines=2,overflow=TextOverflow.Ellipsis)
            Text(subtitle,color=Color.White.copy(.75f),style=MaterialTheme.typography.bodySmall,maxLines=2,overflow=TextOverflow.Ellipsis)
        }
    }
}

@Composable fun WellnessTopBar(title: String, onBack: () -> Unit, action: (@Composable () -> Unit)? = null) {
    Box(Modifier.fillMaxWidth().background(Color(0xEE1C1C1E)).padding(horizontal=18.dp,vertical=8.dp)) {
        Text(title,Modifier.align(Alignment.Center).padding(horizontal=48.dp),fontSize=17.sp,fontWeight=FontWeight.SemiBold,color=Color.White,maxLines=1,overflow=TextOverflow.Ellipsis)
        IconButton(onClick=onBack,modifier=Modifier.align(Alignment.CenterStart).size(42.dp).background(Color.White.copy(alpha=.08f),CircleShape)) {Icon(Icons.Default.ChevronLeft,"返回",Modifier.size(24.dp),tint=Color.White)}
        Box(Modifier.align(Alignment.CenterEnd)) {action?.invoke()}
    }
}

@Composable fun SectionHeading(title: String, subtitle: String = "", onMore: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().then(if(onMore!=null) Modifier.clickable(onClick=onMore) else Modifier),verticalAlignment=Alignment.CenterVertically) {
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
            if(subtitle.isNotEmpty()) Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if(onMore!=null) { Text("全部",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant); Icon(Icons.Default.ChevronRight,null,Modifier.size(18.dp)) }
    }
}

@Composable fun NoteCard(text: String) {
    Surface(color=MaterialTheme.colorScheme.surface.copy(.85f),shape=RoundedCornerShape(16.dp)) {
        Text(text,Modifier.fillMaxWidth().padding(18.dp),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

data class WellnessFeature(val id: String,val title: String,val subtitle: String,val cover: Int,val route: String)
val personalityFeatures = listOf(
    WellnessFeature("mbti","MBTI 人格","发现你的 16 型人格",2,"assessment/mbti"),
    WellnessFeature("mood","情绪状态","PHQ-2 快速筛查",5,"assessment/mood"),
    WellnessFeature("anxiety","焦虑状态","GAD-7 觉察近期担忧",9,"assessment/anxiety"),
    WellnessFeature("stress","压力自测","看看身心是否超负荷",10,"assessment/stress"),
    WellnessFeature("bigFive","大五性格","探索五种人格倾向",12,"assessment/bigFive"))
val sleepFeatures = listOf(
    WellnessFeature("health","睡眠洞察","读取健康连接睡眠阶段",14,"health"),
    WellnessFeature("sleepHealth","睡眠状态","了解近期睡眠困扰",15,"assessment/sleepHealth"),
    WellnessFeature("chronotype","作息类型","晨型、弹性型还是夜型",17,"assessment/chronotype"))
val relaxFeatures = listOf(WellnessFeature("breathing","呼吸练习","用一分钟让身心慢下来",18,"breathing"),WellnessFeature("devices","智能设备","手表与睡眠设备连接指南",19,"devices"))
val wellnessFeatures = personalityFeatures + sleepFeatures + relaxFeatures

@Composable fun DailyInspirationCard(inspiration: cn.cjym.timesleep.data.wellness.Inspiration, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).clickable(onClick=onClick)) {
        AsyncImage(coverPath(inspiration.cover),null,Modifier.matchParentSize(),contentScale=ContentScale.Crop,colorFilter=androidx.compose.ui.graphics.ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix().apply {setToSaturation(.64f)}))
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha=.82f),Color.Black.copy(alpha=.62f),Color.Black.copy(alpha=.20f)))))
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)) {Icon(Icons.Default.AutoAwesome,null,Modifier.size(16.dp),tint=Color.White);Text("今日灵感",fontSize=15.sp,fontWeight=FontWeight.SemiBold,color=Color.White)}
            Text(inspiration.quote,Modifier.widthIn(max=480.dp),fontSize=20.sp,lineHeight=29.sp,fontWeight=FontWeight.SemiBold,fontFamily=androidx.compose.ui.text.font.FontFamily.Serif,color=Color.White)
            Text("${inspiration.date.monthValue}月${inspiration.date.dayOfMonth}日  ·  ${inspiration.theme}",fontSize=12.sp,color=Color.White.copy(alpha=.74f))
        }
    }
}
