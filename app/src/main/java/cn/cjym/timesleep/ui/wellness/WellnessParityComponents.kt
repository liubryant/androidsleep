package cn.cjym.timesleep.ui.wellness

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import cn.cjym.timesleep.R

val WellnessFeature.badge: String get() = when(id) {
    "mbti"->"36 道题";"mood"->"约 1 分钟";"anxiety"->"7 道题";"stress","sleepHealth"->"8 道题"
    "bigFive"->"20 道题";"health"->"健康数据";"chronotype"->"节律探索";"breathing"->"正念时长";else->"多平台"
}
val WellnessFeature.symbol: ImageVector get() = when(id) {
    "mbti"->Icons.Default.Groups;"mood"->Icons.Default.Favorite;"anxiety"->Icons.Default.Air;"stress"->Icons.Default.Speed
    "bigFive"->Icons.Default.AccountCircle;"health"->Icons.Default.MonitorHeart;"sleepHealth"->Icons.Default.Bedtime
    "chronotype"->Icons.Default.WbTwilight;"breathing"->Icons.Default.Spa;else->Icons.Default.Watch
}
val WellnessFeature.accent: Color get() = when(id) {
    "mbti","bigFive"->Color(0xFFAF52DE);"mood"->Color(0xFFFF2D55);"anxiety"->Color(0xFF32ADE6)
    "stress"->Color(0xFFFF9500);"sleepHealth","devices"->Color(0xFF5856D6);"chronotype"->Color(0xFFFFCC00)
    "breathing"->Color(0xFF00C7BE);else->Color(0xFF007AFF)
}

@Composable fun HomeSectionHeading(title:String,subtitle:String="",more:String="",onMore:(()->Unit)?=null) {
    Row(Modifier.fillMaxWidth().then(if(onMore!=null) Modifier.clickable(onClick=onMore) else Modifier),verticalAlignment=Alignment.Top) {
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
            Text(title,fontSize=15.sp,lineHeight=19.sp,fontWeight=FontWeight.SemiBold,color=Color.White)
            if(subtitle.isNotEmpty()) Text(subtitle,fontSize=11.sp,lineHeight=15.sp,color=Color(0xFFA6A6A6))
        }
        if(onMore!=null) Row(Modifier.padding(top=5.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
            Text(more,fontSize=12.sp,color=Color(0xFFA6A6A6));Icon(Icons.Default.ChevronRight,null,Modifier.size(12.dp),tint=Color(0xFFA6A6A6))
        }
    }
}

@Composable fun CoverPanel(cover:Int,modifier:Modifier=Modifier,radius:Int=18,content:@Composable BoxScope.()->Unit) {
    Box(modifier.clip(RoundedCornerShape(radius.dp)).background(Color(0xFF1C1C1E))) {
        AsyncImage(coverPath(cover),null,Modifier.matchParentSize(),contentScale=ContentScale.Crop)
        Box(Modifier.matchParentSize().background(Brush.linearGradient(listOf(Color.Black.copy(alpha=.46f),Color.Black.copy(alpha=.77f)))))
        content()
    }
}

@Composable fun WellnessHomeCard(feature:WellnessFeature,onClick:()->Unit) {
    CoverPanel(feature.cover,Modifier.width(220.dp).height(132.dp).clickable(onClick=onClick)) {
        Column(Modifier.fillMaxSize().padding(15.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Box(Modifier.height(23.dp).clip(CircleShape).background(Color.Black.copy(alpha=.32f)).padding(horizontal=9.dp),contentAlignment=Alignment.Center) {Text(feature.badge,fontSize=10.sp,fontWeight=FontWeight.Medium,color=Color.White)}
                Spacer(Modifier.weight(1f))
                if (feature.id == "mbti") {
                    Icon(painterResource(R.drawable.ic_mbti_people_wave), null, Modifier.size(27.dp), tint = Color.White.copy(alpha = .9f))
                } else {
                    Icon(feature.symbol, null, Modifier.size(27.dp), tint = Color.White.copy(alpha = .9f))
                }
            }
            Spacer(Modifier.weight(1f))
            Text(feature.title,fontSize=17.sp,lineHeight=21.sp,fontWeight=FontWeight.SemiBold,color=Color.White)
            Spacer(Modifier.height(4.dp))
            Text(feature.subtitle,fontSize=11.sp,lineHeight=15.sp,color=Color.White.copy(alpha=.74f),maxLines=1,overflow=TextOverflow.Ellipsis)
        }
    }
}

@Composable fun WellnessGridCard(feature:WellnessFeature,modifier:Modifier=Modifier,onClick:()->Unit) {
    CoverPanel(feature.cover,modifier.heightIn(min=145.dp).clickable(onClick=onClick)) {
        Column(Modifier.fillMaxWidth().padding(15.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha=.3f)),contentAlignment=Alignment.Center) {Icon(feature.symbol,null,Modifier.size(21.dp),tint=Color.White)}
                Spacer(Modifier.weight(1f));Icon(Icons.Default.ChevronRight,null,Modifier.size(16.dp),tint=Color.White.copy(alpha=.72f))
            }
            Text(feature.title,fontSize=15.sp,lineHeight=19.sp,fontWeight=FontWeight.SemiBold,color=Color.White)
            Text(feature.subtitle,fontSize=11.sp,lineHeight=15.sp,color=Color.White.copy(alpha=.8f),maxLines=2)
        }
    }
}

@Composable fun VIPBadge(modifier:Modifier=Modifier) {
    Row(modifier.height(23.dp).clip(CircleShape).background(Color.Black.copy(alpha=.65f)).padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(3.dp)) {
        // Crown silhouette used by the iOS badge, rather than a membership diamond.
        androidx.compose.foundation.Canvas(Modifier.size(10.dp)) {
            val p=androidx.compose.ui.graphics.Path().apply {moveTo(0f,size.height*.25f);lineTo(size.width*.25f,size.height*.5f);lineTo(size.width*.5f,0f);lineTo(size.width*.75f,size.height*.5f);lineTo(size.width,size.height*.25f);lineTo(size.width*.85f,size.height);lineTo(size.width*.15f,size.height);close()};drawPath(p,Color.White)
        }
        Text("VIP",fontSize=9.sp,fontWeight=FontWeight.Bold,color=Color.White)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun VIPPrompt(title:String,onDismiss:()->Unit,onUpgrade:()->Unit) {
    ModalBottomSheet(onDismissRequest=onDismiss,containerColor=Color(0xFF1C1C1E)) {
        Column(Modifier.fillMaxWidth().padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(18.dp)) {
            Icon(Icons.Default.Diamond,null,Modifier.size(42.dp),tint=Color(0xFFE7C48D))
            Text("解锁会员专享内容",fontSize=22.sp,fontWeight=FontWeight.SemiBold,color=Color.White)
            Text("「$title」属于会员权益",fontSize=15.sp,color=Color(0xFFAAAAAA))
            Button(onClick=onUpgrade,modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFFE7C48D),contentColor=Color(0xFF422D1B))) {Text("开通会员")}
            TextButton(onClick=onDismiss) {Text("暂不开通",color=Color(0xFFAAAAAA))}
        }
    }
}
