package cn.cjym.timesleep.ui.wellness

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import cn.cjym.timesleep.TimeSleepApp
import cn.cjym.timesleep.data.wellness.Inspiration
import cn.cjym.timesleep.service.MeditationLibrary
import cn.cjym.timesleep.service.MeditationTrack
import cn.cjym.timesleep.service.MembershipAccess
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

private fun fullDate(date:LocalDate)="${date.year}年${date.monthValue}月${date.dayOfMonth}日"

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun InspirationScreen(back:()->Unit,navigate:(String)->Unit) {
    val context=LocalContext.current
    val repo=(context.applicationContext as TimeSleepApp).wellness
    val scope=rememberCoroutineScope()
    var day by rememberSaveable { mutableIntStateOf(repo.today()) }
    var archive by rememberSaveable { mutableStateOf(false) }
    var favoritesOnly by rememberSaveable {mutableStateOf(false)}
    var sharing by remember {mutableStateOf(false)}
    var shareError by remember {mutableStateOf(false)}
    var locked by remember {mutableStateOf<MeditationTrack?>(null)}
    val favorites by repo.favorites.collectAsState()
    val entry=repo.inspiration(day)
    val favorite="daily-$day" in favorites
    fun share() {if(!sharing) scope.launch {
        sharing=true
        try {
            val file=withContext(Dispatchers.IO) {renderInspirationImage(context,entry)}
            val uri=FileProvider.getUriForFile(context,"${context.packageName}.inspiration.files",file)
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),"分享图片"))
        } catch(e:Exception) {shareError=true} finally {sharing=false}
    }}
    Box(Modifier.fillMaxSize().background(Color(0xFF05101C))) {
        AsyncImage(coverPath(entry.cover),null,Modifier.matchParentSize(),contentScale=ContentScale.Crop,colorFilter=ColorFilter.colorMatrix(ColorMatrix().apply {setToSaturation(.64f)}))
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(0f to Color.Black.copy(alpha=.48f),.28f to Color.Black.copy(alpha=.13f),.78f to Color(0xE0081429),1f to Color(0xFF050D1C))))
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().background(Color(0x7A404040)).padding(horizontal=18.dp,vertical=8.dp)) {
                Text("今日灵感",Modifier.align(Alignment.Center),fontSize=17.sp,fontWeight=FontWeight.SemiBold,color=Color.White)
                IconButton(onClick=back,modifier=Modifier.align(Alignment.CenterStart).size(44.dp).background(Color.White.copy(alpha=.13f),CircleShape)) {Icon(Icons.Default.ChevronLeft,"返回",tint=Color.White)}
                Row(Modifier.align(Alignment.CenterEnd),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick={archive=true},modifier=Modifier.size(44.dp).background(Color.White.copy(alpha=.13f),CircleShape)) {Icon(Icons.Default.CalendarMonth,"日历与收藏",Modifier.size(20.dp),tint=Color.White)}
                    IconButton(onClick={share()},enabled=!sharing,modifier=Modifier.size(44.dp).background(Color.White.copy(alpha=.13f),CircleShape)) {Icon(Icons.Default.IosShare,"分享图片",Modifier.size(20.dp),tint=Color.White)}
                }
            }
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.BottomCenter) {
                val viewportHeight=maxHeight
                Column(Modifier.widthIn(max=560.dp).fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min=viewportHeight).padding(start=24.dp,end=24.dp,bottom=30.dp),verticalArrangement=Arrangement.Bottom) {
                    Spacer(Modifier.height(minOf(viewportHeight*.2f,160.dp)))
                    Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                            Text(entry.date.dayOfMonth.toString(),fontSize=60.sp,lineHeight=72.sp,fontFamily=FontFamily.Serif,fontWeight=FontWeight.ExtraLight,color=Color.White)
                            Column(verticalArrangement=Arrangement.spacedBy(5.dp)) {Text(fullDate(entry.date),fontSize=12.sp,color=Color.White.copy(alpha=.86f));Text(entry.theme,fontSize=12.sp,fontWeight=FontWeight.Medium,color=Color.White.copy(alpha=.86f))}
                        }
                        Box(Modifier.width(26.dp).height(2.dp).background(Color(0xFFE8C28F),CircleShape))
                        Text(entry.quote,fontSize=28.sp,lineHeight=42.sp,fontFamily=FontFamily.Serif,fontWeight=FontWeight.Medium,color=Color.White)
                        Text("—  时光睡眠",fontSize=13.sp,letterSpacing=2.sp,fontWeight=FontWeight.Medium,color=Color.White.copy(alpha=.83f))
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(Modifier.clip(CircleShape).background(Color.White.copy(alpha=.12f)).border(1.dp,Color.White.copy(alpha=.18f),CircleShape).clickable {repo.toggleFavorite("daily-$day")}.padding(horizontal=14.dp,vertical=11.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        Icon(if(favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,null,Modifier.size(18.dp),tint=if(favorite) Color(0xFFFFC7BA) else Color.White)
                        Text(if(favorite) "已收藏" else "收藏",fontSize=13.sp,fontWeight=FontWeight.SemiBold,color=if(favorite) Color(0xFFFFC7BA) else Color.White)
                    }
                    val practice=when((day+54)%60) {0->"calming-sleep";3->"walking-meditation";8->"emotional-release";17->"daytime-energy";else->null}?.let(MeditationLibrary::find)
                    if(practice!=null) {
                        Spacer(Modifier.height(20.dp))
                        Row(Modifier.clip(CircleShape).background(Color.White.copy(alpha=.12f)).border(1.dp,Color.White.copy(alpha=.18f),CircleShape).clickable {if(practice.requiresVIP && !MembershipAccess.active.value) locked=practice else navigate("meditation/${practice.id}")}.padding(horizontal=14.dp,vertical=11.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.PlayCircleOutline,null,Modifier.size(18.dp),tint=Color.White);Text("听一段练习 · ${practice.title}",fontSize=13.sp,color=Color.White)
                        }
                    }
                }
            }
        }
    }
    if(archive) {
        val first=LocalDate.of(2026,11,4)
        val today=first.plusDays(repo.today().toLong())
        val picker=rememberDatePickerState(initialSelectedDateMillis=entry.date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),yearRange=first.year..today.year,selectableDates=object:SelectableDates {
            override fun isSelectableDate(utcTimeMillis:Long):Boolean {val date=Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate();return !date.isBefore(first) && !date.isAfter(today)}
        })
        ModalBottomSheet(onDismissRequest={archive=false},containerColor=Color(0xFF1C1C1E),sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
            Column(Modifier.fillMaxWidth().heightIn(min=440.dp,max=700.dp).padding(horizontal=16.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                Box(Modifier.fillMaxWidth()) {Text("灵感手记",Modifier.align(Alignment.Center),fontSize=17.sp,fontWeight=FontWeight.SemiBold);TextButton(onClick={archive=false},modifier=Modifier.align(Alignment.CenterEnd)) {Text("完成")}}
                SingleChoiceSegmentedButtonRow {SegmentedButton(selected=!favoritesOnly,onClick={favoritesOnly=false},shape=SegmentedButtonDefaults.itemShape(0,2)) {Text("往期日历")};SegmentedButton(selected=favoritesOnly,onClick={favoritesOnly=true},shape=SegmentedButtonDefaults.itemShape(1,2)) {Text("我的收藏")}}
                if(favoritesOnly) {
                    val days=favorites.mapNotNull {it.takeIf {it.startsWith("daily-")}?.removePrefix("daily-")?.toIntOrNull()}.sortedDescending()
                    if(days.isEmpty()) Column(Modifier.fillMaxWidth().padding(vertical=80.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)) {Icon(Icons.Default.FavoriteBorder,null);Text("收藏的灵感会留在这里",fontSize=15.sp,color=Color.Gray)}
                    else LazyColumn {items(days) {d->val old=repo.inspiration(d);Column(Modifier.fillMaxWidth().clickable {day=d;archive=false}.padding(vertical=14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {Text(fullDate(old.date),fontSize=12.sp,color=Color.Gray);Text(old.quote,fontSize=15.sp)};HorizontalDivider()}}
                } else Column(Modifier.verticalScroll(rememberScrollState()),horizontalAlignment=Alignment.CenterHorizontally) {
                    DatePicker(picker,title=null,headline=null,showModeToggle=false)
                    Button(onClick={picker.selectedDateMillis?.let {day=ChronoUnit.DAYS.between(first,Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()).toInt();archive=false}}) {Text("查看这一天")}
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
    locked?.let {VIPPrompt(it.title,{locked=null},{locked=null;navigate("membership")})}
    if(shareError) AlertDialog(onDismissRequest={shareError=false},title={Text("暂时无法生成分享图片")},confirmButton={TextButton(onClick={shareError=false}) {Text("好")}})
}

private fun renderInspirationImage(context:Context,entry:Inspiration):java.io.File {
    val bitmap=android.graphics.Bitmap.createBitmap(1080,1620,android.graphics.Bitmap.Config.ARGB_8888)
    val canvas=android.graphics.Canvas(bitmap)
    canvas.scale(3f,3f)
    val background=context.assets.open("SoundResources/Mindfulness/Covers/cover-%02d.jpg".format(entry.cover)).use(android.graphics.BitmapFactory::decodeStream)
    val paint=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    val scale=maxOf(360f/background.width,540f/background.height)
    val w=background.width*scale;val h=background.height*scale
    paint.colorFilter=android.graphics.ColorMatrixColorFilter(android.graphics.ColorMatrix().apply {setSaturation(.64f)})
    canvas.drawBitmap(background,null,android.graphics.RectF((360-w)/2,(540-h)/2,(360+w)/2,(540+h)/2),paint)
    background.recycle();paint.colorFilter=null
    paint.shader=android.graphics.LinearGradient(0f,0f,0f,540f,intArrayOf(0x7A000000,0x21000000,0xE0081429.toInt(),0xFF050D1C.toInt()),floatArrayOf(0f,.28f,.78f,1f),android.graphics.Shader.TileMode.CLAMP)
    canvas.drawRect(0f,0f,360f,540f,paint);paint.shader=null
    val textPaint=android.text.TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {color=android.graphics.Color.WHITE;textSize=28f;typeface=android.graphics.Typeface.create("serif",android.graphics.Typeface.NORMAL)}
    val quote=android.text.StaticLayout.Builder.obtain(entry.quote,0,entry.quote.length,textPaint,296).setLineSpacing(8f,1f).setIncludePad(false).build()
    val quoteY=540f-32f-35f-quote.height
    paint.color=android.graphics.Color.WHITE;paint.textSize=17f;paint.typeface=android.graphics.Typeface.DEFAULT_BOLD
    canvas.drawText("✧ 今日灵感",32f,50f,paint);paint.textSize=10f;paint.typeface=android.graphics.Typeface.MONOSPACE;canvas.drawText("DAILY NOTES",248f,50f,paint)
    paint.typeface=android.graphics.Typeface.create("serif",android.graphics.Typeface.NORMAL);paint.textSize=60f
    canvas.drawText(entry.date.dayOfMonth.toString(),32f,quoteY-38f,paint)
    paint.textSize=12f;paint.typeface=android.graphics.Typeface.DEFAULT
    canvas.drawText(fullDate(entry.date),116f,quoteY-64f,paint);canvas.drawText(entry.theme,116f,quoteY-44f,paint)
    paint.color=0xFFE8C28F.toInt();canvas.drawRoundRect(32f,quoteY-18f,58f,quoteY-16f,1f,1f,paint)
    canvas.save();canvas.translate(32f,quoteY);quote.draw(canvas);canvas.restore()
    paint.color=0xFFD4D4D4.toInt();paint.textSize=13f;canvas.drawText("—  时光睡眠",32f,508f,paint)
    val dir=java.io.File(context.cacheDir,"inspiration-share").apply {mkdirs()}
    val file=java.io.File(dir,"daily-${entry.day}.png")
    file.outputStream().use {bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
    bitmap.recycle();return file
}
