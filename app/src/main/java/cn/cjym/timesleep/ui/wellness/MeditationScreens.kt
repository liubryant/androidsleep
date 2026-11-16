package cn.cjym.timesleep.ui.wellness

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import cn.cjym.timesleep.ui.shared.IOSSlider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import cn.cjym.timesleep.TimeSleepApp
import cn.cjym.timesleep.service.*
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable fun MeditationRow(tracks:List<MeditationTrack>,navigate:(String)->Unit) {
    var locked by remember {mutableStateOf<MeditationTrack?>(null)}
    BoxWithConstraints(Modifier.fillMaxWidth()) {
    // Two full covers and half of the third, including both visible gaps.
    val cardWidth = (maxWidth - 24.dp) / 2.5f
    val coverHeight = cardWidth * (4f / 3f)
    LazyRow(Modifier.heightIn(min=coverHeight + 35.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        items(tracks,key={it.id}) {track->Column(Modifier.width(cardWidth).clickable {if(track.requiresVIP && !MembershipAccess.active.value) locked=track else navigate("meditation/${track.id}")},verticalArrangement=Arrangement.spacedBy(11.dp)) {
            Box(Modifier.height(coverHeight).fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(18.dp))) {
                AsyncImage(coverPath(track.cover),null,Modifier.matchParentSize(),contentScale=ContentScale.Crop)
                Box(Modifier.matchParentSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent,Color.Black.copy(alpha=.18f)))))
                if(track.requiresVIP) VIPBadge(Modifier.align(Alignment.TopStart).padding(9.dp))
                Box(Modifier.align(Alignment.BottomEnd).padding(9.dp).size(30.dp).background(Color.Black.copy(alpha=.3f),CircleShape),contentAlignment=Alignment.Center) {Icon(Icons.Default.PlayArrow,null,Modifier.size(18.dp),tint=Color.White)}
            }
            Text(track.title,fontSize=15.sp,lineHeight=19.sp,color=Color.White,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }}
    }
    }
    locked?.let {track->VIPPrompt(track.title,{locked=null},{locked=null;navigate("membership")})}
}

@Composable fun MeditationHomeScreen(back:()->Unit,navigate:(String)->Unit) {
    val context=LocalContext.current
    val app=context.applicationContext as TimeSleepApp
    val groups by MeditationLibrary.groups.collectAsState()
    val error by MeditationLibrary.error.collectAsState()
    val descriptions by MeditationLibrary.groupDescriptions.collectAsState()
    val downloadProgress by MindfulnessPlayer.downloadProgress.collectAsState()
    var pendingTrack by remember { mutableStateOf<String?>(null) }
    var downloadMessage by remember { mutableStateOf<String?>(null) }
    val scenes by app.soundLibrary.scenes.collectAsState()
    var banner by rememberSaveable {mutableIntStateOf(0)}
    var locked by remember {mutableStateOf<String?>(null)}
    val scope=rememberCoroutineScope()
    fun openTrack(track: MeditationTrack) {
        if(track.requiresVIP && !MembershipAccess.active.value) {locked=track.title;return}
        if(pendingTrack!=null) {if(pendingTrack!=track.id) downloadMessage="当前音频正在下载，请稍候";return}
        if(MindfulnessPlayer.cached(track)) {navigate("meditation/${track.id}");return}
        pendingTrack=track.id
        scope.launch {
            try {
                MindfulnessPlayer.prepare(track)
                if(track.requiresVIP && !MembershipAccess.active.value) locked=track.title
                else navigate("meditation/${track.id}")
            } catch(e:kotlinx.coroutines.CancellationException) {throw e}
            catch(e:Exception) {downloadMessage="音频下载失败，请检查网络后重试"}
            finally {pendingTrack=null}
        }
    }
    LaunchedEffect(Unit) {MeditationLibrary.load(context);app.soundLibrary.load()}
    LaunchedEffect(Unit) {while(true) {delay(3000);banner=(banner+1)%3}}
    val titles=listOf("正念，是温柔地回到此刻","从一次自然呼吸开始","走神了，也完全没关系")
    val details=listOf("不评判此刻的感受，只是留意呼吸、身体与正在发生的一切。","找一个舒服的姿势，闭上眼睛，跟随声音，把注意力放在呼吸上。","发现思绪飘走时，不必责备自己，轻轻觉察，再慢慢回到当下。")
    val colors=listOf(listOf(Color(0xFF1A575C),Color(0xFF599482)),listOf(Color(0xFF40457A),Color(0xFF7D7AB3)),listOf(Color(0xFF6B4A33),Color(0xFFB08257)))
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        WellnessTopBar("自在正念",back)
        LazyColumn(contentPadding=PaddingValues(bottom=34.dp),verticalArrangement=Arrangement.spacedBy(28.dp)) {
            item {Column(Modifier.padding(horizontal=20.dp).padding(top=8.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)) {
                BoxWithConstraints(Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp)).background(androidx.compose.ui.graphics.Brush.linearGradient(colors[banner]))) {
                    val presenterWidth = (maxWidth * .28f).coerceAtMost(110.dp)
                    Column(
                        Modifier.fillMaxWidth().heightIn(min=140.dp)
                            .padding(start=18.dp,end=presenterWidth+12.dp,top=12.dp,bottom=12.dp),
                        verticalArrangement=Arrangement.spacedBy(8.dp,Alignment.CenterVertically),
                    ) {
                        Text(titles[banner],fontSize=16.sp,lineHeight=21.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.Bold,color=Color.White)
                        Text(details[banner],fontSize=12.sp,lineHeight=17.sp,color=Color.White.copy(alpha=.82f))
                    }
                    AsyncImage("file:///android_asset/mindfulness_presenter.png",null,Modifier.align(Alignment.BottomEnd).width(presenterWidth).height(140.dp),contentScale=ContentScale.Fit)
                }
                Row(horizontalArrangement=Arrangement.spacedBy(7.dp)) {repeat(3) {index->Box(Modifier.width(if(index==banner) 20.dp else 7.dp).height(7.dp).clip(CircleShape).background(Color.White.copy(alpha=if(index==banner) .72f else .16f)).clickable {banner=index})}}
            }}
            val displayGroups=listOf("正念冥想" to (MeditationLibrary.bundled+groups.filter {it.first.trim()=="正念冥想"}.flatMap {it.second}))+groups.filter {it.first.trim()!="正念冥想"}
            displayGroups.forEachIndexed {index,group->item {Column(verticalArrangement=Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal=20.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {Text(group.first,fontSize=18.sp,lineHeight=23.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.Bold);Text(descriptions[group.first.trim()] ?: "跟随声音，把注意力带回当下",fontSize=12.sp,lineHeight=17.sp,color=Color.Gray,maxLines=1)}
                    if(index==0) Surface(onClick={navigate("voices")},shape=CircleShape,color=Color(0xFFEFE7FA)) {Row(Modifier.padding(horizontal=8.dp,vertical=5.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {Icon(Icons.Default.People,null,Modifier.size(12.dp),tint=Color(0xFF5E4F87));Text("认识声音人物",fontSize=10.sp,lineHeight=14.sp,color=Color(0xFF5E4F87))}}
                }
                LazyRow(contentPadding=PaddingValues(horizontal=20.dp,vertical=2.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),modifier=Modifier.height(229.dp)) {
                    items(group.second,key={it.id}) {track->Column(Modifier.width(138.dp).clickable {openTrack(track)},verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.fillMaxWidth().height(184.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))) {
                            AsyncImage(coverPath(track.cover),null,Modifier.matchParentSize(),contentScale=ContentScale.Crop)
                            if(track.requiresVIP) VIPBadge(Modifier.align(Alignment.TopStart).padding(7.dp))
                            MeditationDownloadBadge(pendingTrack==track.id,downloadProgress,MindfulnessPlayer.cached(track),Modifier.align(Alignment.BottomEnd).padding(7.dp))
                        }
                        Column(verticalArrangement=Arrangement.spacedBy(3.dp)) {Text(track.title,fontSize=12.sp,lineHeight=16.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis);Text(track.subtitle,fontSize=10.sp,lineHeight=13.sp,color=Color.Gray,maxLines=1)}
                    }}
                }
            }}}
            if(error!=null) item {TextButton(onClick={scope.launch {MeditationLibrary.load(context)}}) {Text(error!!)}}
            item {Column(verticalArrangement=Arrangement.spacedBy(14.dp)) {
                Column(Modifier.padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {Text("大自然中入眠",fontSize=18.sp,lineHeight=23.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.Bold);Text("枕着山海风雨，慢慢沉入梦乡",fontSize=12.sp,lineHeight=17.sp,color=Color.Gray)}
                val nature=scenes.takeLast(20).filterNot {it.title.trim() in setOf("打字机","篮球场","洗碗机","图书馆","雨窗","沉睡小夜曲","睡吧睡吧","年")}
                LazyRow(contentPadding=PaddingValues(horizontal=20.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {items(nature.chunked(2)) {pair->Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {pair.forEach {scene->Column(Modifier.width(138.dp).height(183.dp).clickable {openTrack(MeditationLibrary.natureTrack(scene))},verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(138.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))) {AsyncImage("file:///android_asset/${scene.coverAssetPath}",null,Modifier.matchParentSize(),contentScale=ContentScale.Crop);if(scene.requiresVIP) VIPBadge(Modifier.align(Alignment.TopStart).padding(7.dp));MeditationDownloadBadge(pendingTrack=="nature-${scene.id}",downloadProgress,MindfulnessPlayer.cached(MeditationLibrary.natureTrack(scene)),Modifier.align(Alignment.BottomEnd).padding(7.dp))}
                    Text(scene.title,fontSize=12.sp,lineHeight=16.sp,maxLines=1);Text(scene.subtitle,fontSize=10.sp,lineHeight=13.sp,color=Color.Gray,maxLines=1)
                }}}}}
            }}
        }
    }
    downloadMessage?.let {message->AlertDialog(onDismissRequest={downloadMessage=null},title={Text("提示")},text={Text(message)},confirmButton={TextButton(onClick={downloadMessage=null}) {Text("知道了")}})}
    locked?.let {VIPPrompt(it,{locked=null},{locked=null;navigate("membership")})}
}

@Composable fun MeditationPlayerScreen(id:String,back:()->Unit,navigate:(String)->Unit) {
    val context=LocalContext.current
    val repo=(context.applicationContext as TimeSleepApp).wellness
    val state by MindfulnessPlayer.state.collectAsState()
    val favorites by repo.favorites.collectAsState()
    val progress by MindfulnessPlayer.downloadProgress.collectAsState()
    val active by MembershipAccess.active.collectAsState()
    val groups by MeditationLibrary.groups.collectAsState()
    val initial=remember(id,groups) {MeditationLibrary.find(id)}
    var timers by remember {mutableStateOf(false)}
    var seeking by remember {mutableStateOf(false)}
    var seek by remember {mutableFloatStateOf(0f)}
    var history by rememberSaveable {mutableStateOf(listOf<String>())}
    LaunchedEffect(id,initial) {if(initial!=null && (!initial.requiresVIP || active)) MindfulnessPlayer.play(initial)}
    if(initial==null) {Column {WellnessTopBar("正念冥想",back);NoteCard("内容尚未加载，请返回列表重试。")};return}
    if(initial.requiresVIP && !active) {
        SceneryBackground(initial.cover) {Column {WellnessTopBar(initial.title,back);Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(22.dp)){Text("这段练习属于会员内容",style=MaterialTheme.typography.headlineSmall);Text(initial.subtitle);Button(onClick={navigate("membership")}){Text("查看会员权益")};OutlinedButton(onClick={navigate("meditation/deep-sleep-healing")}){Text("先体验免费练习")}}}};return
    }
    val track=state?.track ?: initial
    val playback=state
    val transition=rememberInfiniteTransition(label="breathe")
    val scale by transition.animateFloat(.9f,1.06f,infiniteRepeatable(tween(4000,easing=FastOutSlowInEasing),RepeatMode.Reverse),label="artwork")
    fun switchTrack(previous:Boolean) {
        val candidates=(MeditationLibrary.bundled+groups.flatMap {it.second}).filter{(!it.requiresVIP||active)&&it.id!=track.id}
        val next=if(previous) history.lastOrNull()?.let(MeditationLibrary::find) else candidates.randomOrNull()
        if(next!=null){if(previous) history=history.dropLast(1) else history=history+track.id;MindfulnessPlayer.play(next,preserveTimer=true)}
    }
    SceneryBackground(track.cover,coverAsset=track.coverAsset) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().padding(horizontal=22.dp,vertical=8.dp)) {Text(track.title,Modifier.align(Alignment.Center),fontSize=17.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold,color=Color.White);IconButton(onClick=back,modifier=Modifier.align(Alignment.CenterStart).size(44.dp).background(Color.White.copy(alpha=.12f),CircleShape)) {Icon(Icons.Default.ChevronLeft,"返回",tint=Color.White)}}
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val viewportHeight = maxHeight
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min=viewportHeight).padding(start=24.dp,end=24.dp,top=16.dp,bottom=36.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.SpaceBetween) {
                Box(Modifier.size(184.dp),contentAlignment=Alignment.Center) {
                    Box(Modifier.size(167.dp).scale(if(playback?.playing==true) scale else .9f).border(1.dp,Color.White.copy(alpha=.08f),CircleShape),contentAlignment=Alignment.Center) {
                        Box(Modifier.size(140.dp).background(Color.White.copy(alpha=.06f),CircleShape).border(1.dp,Color.White.copy(alpha=.18f),CircleShape),contentAlignment=Alignment.Center) {
                            Box(Modifier.size(100.dp).background(Color.White.copy(alpha=.2f),CircleShape))
                        }
                    }
                    Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Spa,null,Modifier.size(36.dp),tint=Color.White)
                        Text(if(playback?.playing==true) "跟随声音\n回到当下" else "片刻暂停\n保持觉察",color=Color.White.copy(alpha=.74f),style=MaterialTheme.typography.bodySmall,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
                Column(
                    Modifier.padding(top=16.dp).fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(30.dp))
                        .background(Color.White.copy(alpha=.10f))
                        .border(1.dp,Color.White.copy(alpha=.16f),androidx.compose.foundation.shape.RoundedCornerShape(30.dp))
                        .padding(16.dp),
                    verticalArrangement=Arrangement.spacedBy(12.dp),
                ) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(7.dp)) {Text(track.title,fontSize=18.sp,lineHeight=24.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold,color=Color.White);Text(track.subtitle,fontSize=12.sp,lineHeight=17.sp,color=Color.White.copy(alpha=.62f))}
                    FilledIconButton(onClick={MindfulnessPlayer.toggle()},modifier=Modifier.size(50.dp),colors=IconButtonDefaults.filledIconButtonColors(containerColor=Color.White,contentColor=Color(0xFF21474D)),enabled=playback?.loading==false && playback.error==null){Icon(if(playback?.playing==true) Icons.Default.Pause else Icons.Default.PlayArrow,if(playback?.playing==true) "暂停" else "播放")}
                }
                Box(Modifier.fillMaxWidth().height(38.dp),contentAlignment=Alignment.Center) {
                    if(playback?.loading==true) {
                        Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                            LinearProgressIndicator(progress={progress?:0f},modifier=Modifier.fillMaxWidth())
                            Text(if(progress!=null) "下载中 ${(progress!!*100).toInt()}%" else "正在准备音频…",fontSize=11.sp,lineHeight=14.sp,color=Color.White.copy(alpha=.7f))
                        }
                    } else {
                IOSSlider(value=if(seeking) seek else playback?.position?:0f,onValueChange={seeking=true;seek=it},onValueChangeFinished={MindfulnessPlayer.seek(seek);seeking=false},valueRange=0f..(playback?.duration?:1f).coerceAtLeast(1f),enabled=playback?.loading==false && playback.error==null)
                    }
                }
                playback?.error?.let {Text(it,fontSize=12.sp,color=Color(0xFFFFC7B8));TextButton(onClick={MindfulnessPlayer.play(track)}){Text("重试")}}
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(timeText(if(seeking) seek else playback?.position?:0f),fontSize=12.sp,color=Color.White.copy(alpha=.58f));Text("-"+timeText(((playback?.duration?:0f)-(if(seeking) seek else playback?.position?:0f)).coerceAtLeast(0f)),fontSize=12.sp,color=Color.White.copy(alpha=.58f))}
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    MeditationAction(if(track.id in favorites) "已收藏" else "收藏",if(track.id in favorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder,Modifier.weight(1f),track.id in favorites) {repo.toggleFavorite(track.id)}
                    MeditationAction(playback?.remainingSeconds?.let {timeText(it.toFloat())} ?: "定时",Icons.Default.Timer,Modifier.weight(1f),playback?.remainingSeconds!=null) {timers=true}
                    MeditationAction("上一个",Icons.Default.SkipPrevious,Modifier.weight(1f),enabled=history.isNotEmpty()) {switchTrack(true)}
                    MeditationAction("下一个",Icons.Default.SkipNext,Modifier.weight(1f)) {switchTrack(false)}
                }
                }


            }
            }
        }
    }
    if(timers) AlertDialog(onDismissRequest={timers=false},title={Text("定时关闭")},text={Column {listOf(5,10,15,30).forEach {m->TextButton(onClick={MindfulnessPlayer.timer(m);timers=false}){Text("$m 分钟")}};TextButton(onClick={MindfulnessPlayer.timer(null);timers=false}){Text("取消定时")}}},confirmButton={TextButton(onClick={timers=false}){Text("关闭")}})
}
fun timeText(seconds:Float):String {val total=seconds.toInt().coerceAtLeast(0);return "%02d:%02d".format(total/60,total%60)}

@Composable fun VoiceCharactersScreen(back:()->Unit) {
    var web by remember {mutableStateOf<WebView?>(null)}
    DisposableEffect(Unit) {onDispose{web?.stopLoading();web?.destroy()}}
    Column(Modifier.fillMaxSize()) {
        AndroidView(factory={ctx->WebView(ctx).apply {web=this;setBackgroundColor(android.graphics.Color.BLACK);settings.javaScriptEnabled=true;settings.allowFileAccess=true;settings.allowContentAccess=false;webViewClient=object:WebViewClient(){override fun shouldOverrideUrlLoading(view:WebView?,request:android.webkit.WebResourceRequest?):Boolean { if(request?.url?.scheme=="timesleep") back(); return request?.url?.scheme !in listOf("file","about") }};loadUrl("file:///android_asset/VoiceCharacters/index.html")}},modifier=Modifier.fillMaxSize())
    }
}


@Composable private fun MeditationAction(title:String,icon:androidx.compose.ui.graphics.vector.ImageVector,modifier:Modifier=Modifier,active:Boolean=false,enabled:Boolean=true,onClick:()->Unit) {
    val tint=if(active) Color(0xFFBDFFE8) else Color.White.copy(alpha=if(enabled) .84f else .35f)
    Column(modifier.heightIn(min=54.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(17.dp))
        .background(Color.White.copy(alpha=if(active) .14f else .07f)).clickable(enabled=enabled,onClick=onClick).padding(horizontal=2.dp,vertical=7.dp),
        horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(7.dp,Alignment.CenterVertically)) {
        Icon(icon,title,Modifier.size(19.dp),tint=tint)
        Text(title,fontSize=10.sp,lineHeight=13.sp,maxLines=1,color=tint)
    }
}

@Composable private fun MeditationDownloadBadge(loading:Boolean,progress:Float?,cached:Boolean,modifier:Modifier=Modifier) {
    Box(modifier.size(if(loading) 34.dp else 23.dp).background(Color.Black.copy(alpha=.3f),CircleShape),contentAlignment=Alignment.Center) {
        if(loading) {
            if(progress==null) CircularProgressIndicator(Modifier.size(26.dp),color=Color.White,strokeWidth=2.dp)
            else {
                CircularProgressIndicator(progress={progress.coerceIn(0f,1f)},modifier=Modifier.size(26.dp),color=Color.White,strokeWidth=2.dp)
                Text("${(progress*100).toInt()}",fontSize=7.sp,color=Color.White)
            }
        } else Icon(if(cached) Icons.Default.PlayArrow else Icons.Default.ArrowDownward,null,Modifier.size(13.dp),tint=Color.White)
    }
}
