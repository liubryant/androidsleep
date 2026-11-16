package cn.cjym.timesleep.ui.sounds

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.cjym.timesleep.TimeSleepApp
import cn.cjym.timesleep.service.*
import cn.cjym.timesleep.ui.wellness.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime

internal fun homeGreeting(hour:Int):String = when(hour) {
    in 5..7->"早上好"; in 8..11->"上午好"; in 12..13->"中午好"
    in 14..17->"下午好"; in 18..20->"傍晚好"; in 21..23->"晚上好"; else->"夜深了"
} + "，时光睡眠"

@Composable fun SoundHomeScreen(modifier:Modifier=Modifier,navigate:(String)->Unit={}) {
    val app=LocalContext.current.applicationContext as TimeSleepApp
    val library=app.soundLibrary
    val scenes by library.scenes.collectAsState()
    val favorites by library.favorites.collectAsState(initial=emptySet())
    val active by MembershipAccess.active.collectAsState()
    val audio by AudioPlayerManager.nowPlaying.collectAsState()
    val story by StoryPlayerManager.nowPlaying.collectAsState()
    val meditation by MindfulnessPlayer.state.collectAsState()
    val scope=rememberCoroutineScope()
    val focus=LocalFocusManager.current
    val listState=rememberLazyListState()
    var search by rememberSaveable {mutableStateOf("")}
    var category by rememberSaveable {mutableStateOf("首页")}
    var lockedTitle by remember {mutableStateOf<String?>(null)}
    var menu by remember {mutableStateOf(false)}
    var hour by remember {mutableIntStateOf(LocalTime.now().hour)}
    LaunchedEffect(Unit) { library.load() }
    LaunchedEffect(Unit) { while(true) { hour=LocalTime.now().hour;delay(60_000) } }
    LaunchedEffect(search,category) { if(search.isNotBlank()) delay(250);listState.scrollToItem(0) }
    val filtered=remember(scenes,search,category,favorites) {
        if(search.isNotBlank()) scenes.filter {"${it.title} ${it.subtitle} ${it.category}".contains(search.trim(),true)}
        else if(category=="收藏") scenes.filter {it.id in favorites}
        else if(category=="首页") scenes.sortedByDescending {it.id in favorites}
        else scenes
    }
    val isHome=search.isBlank() && category=="首页"
    Column(modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally) {
        Row(Modifier.widthIn(max=1100.dp).fillMaxWidth().padding(start=16.dp,end=16.dp,top=3.dp,bottom=13.dp),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
            HomeWeatherBadge()
            BoxWithConstraints(Modifier.weight(1f)) {
                // Preserve the full iOS greeting on narrower Android displays.
                Text(homeGreeting(hour),fontSize=if(maxWidth<200.dp) 16.sp else 20.sp,fontWeight=FontWeight.Bold,color=Color.White,maxLines=1)
            }
            Box {
                IconButton(onClick={menu=true},modifier=Modifier.size(44.dp)) {Icon(Icons.Default.Tune,"声音筛选与定时",Modifier.size(23.dp),tint=Color.White)}
                DropdownMenu(menu,{menu=false}) {
                    DropdownMenuItem({Text("显示全部声音")},{category="全部";search="";menu=false})
                    DropdownMenuItem({Text("显示收藏")},{category="收藏";menu=false})
                    DropdownMenuItem({Text("返回首页")},{category="首页";search="";menu=false})
                    listOf(15,30,60).forEach { m->DropdownMenuItem({Text("$m 分钟后停止")},{AudioPlayerManager.setSleepTimer(m);menu=false}) }
                    DropdownMenuItem({Text("取消定时")},{AudioPlayerManager.cancelSleepTimer();menu=false})
                }
            }
        }
        Box(Modifier.widthIn(max=1100.dp).fillMaxWidth().padding(start=16.dp,end=16.dp,bottom=12.dp)) {
            BasicTextField(value=search,onValueChange={search=it},singleLine=true,
                textStyle=MaterialTheme.typography.bodyMedium.copy(fontSize=15.sp,color=Color.White),cursorBrush=SolidColor(Color.White),
                keyboardOptions=KeyboardOptions(imeAction=ImeAction.Search),keyboardActions=KeyboardActions(onSearch={focus.clearFocus();scope.launch {listState.animateScrollToItem(0)}}),
                modifier=Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF1C1C1E)),
                decorationBox={inner->Row(Modifier.fillMaxSize().padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Search,null,Modifier.size(20.dp),tint=Color(0xFFA6A6A6))
                    Box(Modifier.weight(1f)) {if(search.isEmpty()) Text("搜索雨声、森林、海浪",fontSize=15.sp,color=Color(0xFF8E8E93));inner()}
                    if(search.isNotEmpty()) IconButton(onClick={search=""},modifier=Modifier.size(24.dp)) {Icon(Icons.Default.Cancel,"清除搜索",Modifier.size(18.dp),tint=Color.Gray)}
                }})
        }
        LazyColumn(state=listState,modifier=Modifier.widthIn(max=1100.dp).fillMaxWidth().weight(1f),contentPadding=PaddingValues(start=16.dp,end=16.dp,bottom=if(audio!=null || story!=null || meditation!=null) 96.dp else 16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            if(isHome) {
                item("inspiration") {DailyInspirationCard(app.wellness.inspiration(app.wellness.today())) {navigate("inspiration")}}
                item("mindfulness") {Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    HomeSectionHeading("正念冥想","跟随声音，把注意力带回当下","更多") {navigate("meditations")}
                    MeditationRow(MeditationLibrary.bundled.take(6),navigate)
                }}
                item("wellness") {Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    HomeSectionHeading("情绪与性格","温柔觉察情绪、压力与内在倾向","全部") {navigate("wellness")}
                    FeatureRow(personalityFeatures,navigate)
                }}
                if(!active) item("membership") {Surface(onClick={navigate("membership")},shape=RoundedCornerShape(12.dp),color=Color(0xFF242424)) {
                    Row(Modifier.fillMaxWidth().heightIn(min=48.dp).padding(horizontal=14.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Diamond,null,Modifier.size(20.dp),tint=Color(0xFFB3A387))
                        Text("解锁更多助眠声音与故事",Modifier.weight(1f),fontSize=13.sp,color=Color(0xFFABABAB))
                        Text("开通会员",fontSize=13.sp,color=Color(0xFFB3A387));Icon(Icons.Default.ChevronRight,null,Modifier.size(12.dp),tint=Color(0xFFB3A387))
                    }
                }}
            }
            item("sound-heading") {
                Box(Modifier.padding(top=if(isHome) 8.dp else 0.dp)) {
                    HomeSectionHeading(if(search.isNotBlank()) "声音搜索结果" else "大自然入眠",if(search.isNotBlank()) "" else "枕着山海风雨，慢慢沉入梦乡")
                }
            }
            if(filtered.isEmpty()) item {NoteCard(if(category=="收藏") "还没有收藏的声音" else "没有匹配的声音，试试更短的关键词，比如「雨」或「海浪」。")}
            filtered.chunked(6).forEachIndexed { sectionIndex, section ->
                item("sounds-$sectionIndex") {Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    section.chunked(2).forEach {pair->Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                        pair.forEach {scene->SoundCard(scene,scene.id in favorites,{scope.launch {library.toggleFavorite(scene)}},Modifier.weight(1f),onLocked={lockedTitle=scene.title})}
                        if(pair.size==1) Spacer(Modifier.weight(1f))
                    }}
                }}
                if(section.size==6) item("ad-$sectionIndex") {DrawFeedAdCard()}
            }
        }
    }
    lockedTitle?.let {VIPPrompt(it,{lockedTitle=null},{lockedTitle=null;navigate("membership")})}

}
