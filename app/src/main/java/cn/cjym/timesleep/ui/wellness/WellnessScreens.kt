package cn.cjym.timesleep.ui.wellness

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import cn.cjym.timesleep.TimeSleepApp
import cn.cjym.timesleep.data.wellness.*
import cn.cjym.timesleep.service.MembershipAccess
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable fun FeatureRow(features: List<WellnessFeature>, navigate: (String)->Unit) {
    LazyRow(Modifier.height(140.dp),contentPadding=PaddingValues(2.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
        items(features,key={it.id}) { feature -> WellnessHomeCard(feature) {navigate(feature.route)} }
    }
}

@Composable fun WellnessHubScreen(back: ()->Unit,navigate:(String)->Unit) {
    val repo=(LocalContext.current.applicationContext as TimeSleepApp).wellness
    val records by repo.records.collectAsState()
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        WellnessTopBar("身心探索",back)
        LazyColumn(contentPadding=PaddingValues(start=16.dp,end=16.dp,top=16.dp,bottom=46.dp),verticalArrangement=Arrangement.spacedBy(22.dp)) {
            item {CoverPanel(25,Modifier.fillMaxWidth().heightIn(min=150.dp),20) {
                Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(9.dp)) {
                    Text("给自己一点了解的时间",fontSize=20.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold,color=Color.White)
                    Text("从睡眠、情绪到性格节律，所有记录默认仅保存在 App 本地。",Modifier.widthIn(max=320.dp),fontSize=13.sp,lineHeight=20.sp,color=Color.White.copy(alpha=.82f))
                }
            }}
            listOf(Triple("睡眠健康","睡眠数据、状态评估与日常记录",sleepFeatures),Triple("情绪与性格","了解近期感受与稳定人格倾向",personalityFeatures),Triple("放松与设备","主动恢复并汇总更多健康数据",relaxFeatures)).forEach { (title,subtitle,features) ->
                item {Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {Text(title,fontSize=18.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold);Text(subtitle,fontSize=12.sp,color=Color(0xFF999999))}
                    features.chunked(2).forEach { pair -> Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        pair.forEach { feature -> WellnessGridCard(feature,Modifier.weight(1f)) {navigate(feature.route)} }
                        if(pair.size==1) Spacer(Modifier.weight(1f))
                    }}
                }}
            }
            item {CoverPanel(23,Modifier.fillMaxWidth().clickable {navigate("history")}) {
                Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                    Icon(Icons.Default.History,null,Modifier.size(48.dp).background(Color.Black.copy(alpha=.3f),RoundedCornerShape(14.dp)).padding(12.dp),tint=Color.White)
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {Text("测评记录",fontSize=15.sp,color=Color.White);Text(if(records.isEmpty()) "完成测评后，可在这里查看变化" else "已保存 ${records.size} 次测评结果",fontSize=12.sp,color=Color.White.copy(alpha=.8f))}
                    Icon(Icons.Default.ChevronRight,null,Modifier.size(16.dp),tint=Color.White)
                }
            }}
            item {Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {Icon(Icons.Default.Shield,null,Modifier.size(18.dp),tint=Color(0xFF34C759));Text("测评结果默认仅保存在当前设备，不会用于广告推荐，仅用于自我觉察，不代替专业诊断。",fontSize=11.sp,color=Color(0xFF999999))}}
        }
    }
}

@Composable fun AssessmentScreen(kind:String,back:()->Unit,navigate:(String)->Unit) {
    val repo = (LocalContext.current.applicationContext as TimeSleepApp).wellness
    val definition = remember(kind) { repo.catalog.definitions.first { it.id==kind } }
    val initial = remember(kind) { repo.draft(kind) }
    var answers by remember(kind) { mutableStateOf(initial.answers) }
    var index by rememberSaveable(kind) { mutableIntStateOf(initial.index.coerceIn(0,definition.questions.lastIndex)) }
    var result by remember(kind) { mutableStateOf<WellnessRecord?>(null) }
    var showGate by remember { mutableStateOf(false) }
    var showRestart by remember { mutableStateOf(false) }
    val records by repo.records.collectAsState()
    var showPrevious by rememberSaveable(kind) { mutableStateOf(kind=="mbti" && records.any { it.assessmentID==kind }) }
    val previous = records.firstOrNull { it.assessmentID==kind }
    val active by MembershipAccess.active.collectAsState()
    val cover = wellnessFeatures.first { it.id==kind }.cover
    val displayed = result ?: previous?.takeIf { showPrevious }
    fun finish() {
        if(kind=="mbti" && !active) { showGate=true; return }
        result=WellnessScoring.result(repo.catalog,definition,answers).also(repo::save)
    }
    BackHandler { back() }
    val feature=wellnessFeatures.first {it.id==kind}
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        WellnessTopBar(definition.title,back)
        if(displayed!=null) {
            LazyColumn(contentPadding=PaddingValues(18.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
                item {ResultBody(displayed)}
                item {OutlinedButton(onClick={showRestart=true}) {Text("重新测评")}}
                item {Text("结果仅供自我观察，不用于疾病诊断。感受持续或明显影响生活时，请联系有资质的专业人员。",fontSize=12.sp,color=Color.Gray)}
            }
        } else {
            val question=definition.questions[index]
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
                CoverPanel(cover,Modifier.fillMaxWidth().heightIn(min=136.dp)) {
                    Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                        Icon(feature.symbol,null,Modifier.size(58.dp).background(feature.accent,RoundedCornerShape(17.dp)).padding(15.dp),tint=Color.White)
                        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {Text(definition.introduction,fontSize=13.sp,lineHeight=19.sp,color=Color.White);Text(definition.periodText,fontSize=11.sp,color=Color.White.copy(alpha=.8f))}
                    }
                }
                Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth()) {Text("第 ${index+1} 题",Modifier.weight(1f),fontSize=12.sp,color=feature.accent);Text("${index+1} / ${definition.questions.size}",fontSize=12.sp,color=Color.Gray)}
                    LinearProgressIndicator(progress={(index+1f)/definition.questions.size},modifier=Modifier.fillMaxWidth().height(4.dp),color=feature.accent,trackColor=Color(0xFF2C2C2E))
                    Text(question.text,Modifier.padding(top=8.dp),fontSize=21.sp,lineHeight=29.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold,color=Color.White)
                }
                Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {question.choices.forEach {choice ->
                    val selected=answers[question.id]==choice.score
                    Surface(onClick={answers=answers+(question.id to choice.score);repo.saveDraft(kind,AssessmentDraft(index,answers))},shape=RoundedCornerShape(15.dp),color=if(selected) feature.accent.copy(alpha=.1f) else Color(0xFF1C1C1E),border=if(selected) androidx.compose.foundation.BorderStroke(1.dp,feature.accent.copy(alpha=.48f)) else null) {
                        Row(Modifier.fillMaxWidth().heightIn(min=54.dp).padding(horizontal=16.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            Text(choice.text,Modifier.weight(1f),fontSize=15.sp,color=Color.White,fontWeight=if(selected) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal)
                            Icon(if(selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,null,Modifier.size(22.dp),tint=if(selected) feature.accent else Color.Gray.copy(alpha=.45f))
                        }
                    }
                }}
                Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick={index--;repo.saveDraft(kind,AssessmentDraft(index,answers))},enabled=index>0,shape=RoundedCornerShape(10.dp)) {Text("上一题")}
                    Button(onClick={if(index==definition.questions.lastIndex) finish() else {index++;repo.saveDraft(kind,AssessmentDraft(index,answers))}},enabled=answers.containsKey(question.id),modifier=Modifier.weight(1f),shape=RoundedCornerShape(10.dp),colors=ButtonDefaults.buttonColors(containerColor=feature.accent)) {Text(if(index==definition.questions.lastIndex) "查看结果" else "下一题")}
                }
                Text("本测评用于自我觉察，不构成医疗诊断或治疗建议。",Modifier.fillMaxWidth(),fontSize=11.sp,color=Color(0xFF777777),textAlign=TextAlign.Center)
                Spacer(Modifier.height(28.dp))
            }
        }
    }
    if(showGate) AlertDialog(onDismissRequest={showGate=false},title={Text("解锁 MBTI 完整报告")},text={Text("答题已保存。完整人格报告属于会员权益。")},confirmButton={TextButton(onClick={showGate=false;navigate("membership")}) { Text("查看会员") }},dismissButton={TextButton(onClick={showGate=false}) { Text("稍后") }})
    if(showRestart) AlertDialog(onDismissRequest={showRestart=false},title={Text("重新开始测评？")},text={Text("已有历史记录会保留。")},confirmButton={TextButton(onClick={showRestart=false;showPrevious=false;result=null;answers=emptyMap();index=0;repo.clearDraft(kind)}) { Text("开始") }},dismissButton={TextButton(onClick={showRestart=false}) { Text("取消") }})
}

@Composable fun ResultBody(record: WellnessRecord) {
    val feature=wellnessFeatures.first {it.id==record.assessmentID}
    Column(verticalArrangement=Arrangement.spacedBy(18.dp)) {
        CoverPanel(feature.cover,Modifier.fillMaxWidth(),22) {
            Column(Modifier.fillMaxWidth().padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(13.dp)) {
                Box(Modifier.size(138.dp),contentAlignment=Alignment.Center) {
                    CircularProgressIndicator(progress={record.score.toFloat()/record.maximumScore.coerceAtLeast(1)},modifier=Modifier.fillMaxSize(),color=feature.accent,trackColor=Color.White.copy(alpha=.14f),strokeWidth=11.dp)
                    Column(horizontalAlignment=Alignment.CenterHorizontally) {Text(if(record.assessmentID=="mbti") record.level.substringBefore(" ·") else record.score.toString(),fontSize=if(record.assessmentID=="mbti") 27.sp else 32.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.Bold,color=Color.White);Text(if(record.assessmentID=="mbti") "人格类型" else if(record.metrics.isEmpty()) "/ ${record.maximumScore}" else "综合画像",fontSize=12.sp,color=Color.White.copy(alpha=.8f))}
                }
                Text(if(record.assessmentID=="mbti") record.level.substringAfter("·").trim() else record.level,fontSize=22.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold,color=Color.White)
                Text(record.summary,fontSize=14.sp,lineHeight=22.sp,textAlign=TextAlign.Center,color=Color.White.copy(alpha=.8f))
            }
        }
        if(record.metrics.isNotEmpty()) Surface(shape=RoundedCornerShape(18.dp),color=Color(0xFF1C1C1E)) {Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("维度画像",fontSize=17.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold)
            record.metrics.forEach {metric->Row(Modifier.fillMaxWidth()) {Text(metric.title,Modifier.weight(1f),fontSize=14.sp);Text("${metric.value}%",fontSize=12.sp,color=Color.Gray)};LinearProgressIndicator(progress={metric.value/100f},modifier=Modifier.fillMaxWidth(),color=feature.accent)}
        }}
        Surface(shape=RoundedCornerShape(18.dp),color=Color(0xFF1C1C1E)) {Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {Icon(Icons.Default.Lightbulb,null,tint=Color(0xFFFF9500));Text(if(record.assessmentID=="mbti") "人格解读" else "给你的建议",fontSize=17.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold,color=Color(0xFFFF9500))}
            record.suggestions.forEachIndexed {i,suggestion->Row(horizontalArrangement=Arrangement.spacedBy(11.dp)) {Box(Modifier.size(22.dp).background(feature.accent,CircleShape),contentAlignment=Alignment.Center) {Text("${i+1}",fontSize=12.sp,color=Color.White)};Text(suggestion,fontSize=14.sp,lineHeight=21.sp,color=Color.White)}}
        }}
    }
}

@Composable fun WellnessHistoryScreen(back:()->Unit,navigate:(String)->Unit) {
    val repo=(LocalContext.current.applicationContext as TimeSleepApp).wellness
    val records by repo.records.collectAsState()
    var confirm by remember { mutableStateOf(false) }
    Column {
        WellnessTopBar("测评历史",back) { IconButton(onClick={confirm=true},enabled=records.isNotEmpty()) {Icon(Icons.Default.DeleteOutline,"清空历史")} }
        LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            if(records.isEmpty()) item { NoteCard("还没有测评记录。从一次简短的自我觉察开始吧。") }
            items(records,key={it.id}) { record ->
                Surface(onClick={navigate("record/${record.id}")},shape=RoundedCornerShape(18.dp)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text(record.title,style=MaterialTheme.typography.titleMedium);Text(record.level,color=MaterialTheme.colorScheme.primary)
                        Text(SimpleDateFormat("yyyy年MM月dd日 HH:mm",Locale.CHINA).format(Date(record.date)),style=MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
    if(confirm) AlertDialog(onDismissRequest={confirm=false},title={Text("清空所有测评记录？")},text={Text("删除后无法恢复，答题草稿不受影响。")},confirmButton={TextButton(onClick={repo.clearHistory();confirm=false}){Text("清空")}},dismissButton={TextButton(onClick={confirm=false}){Text("取消")}})
}

@Composable fun WellnessRecordScreen(id:String,back:()->Unit) {
    val repo=(LocalContext.current.applicationContext as TimeSleepApp).wellness
    val records by repo.records.collectAsState()
    val record=records.firstOrNull {it.id==id}
    var confirm by remember {mutableStateOf(false)}
    Column {
        WellnessTopBar(record?.title ?: "测评记录",back) { IconButton(onClick={confirm=true}){Icon(Icons.Default.DeleteOutline,"删除记录")} }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(22.dp)) { if(record!=null) ResultBody(record) else Text("记录已删除") }
    }
    if(confirm) AlertDialog(onDismissRequest={confirm=false},title={Text("删除这条记录？")},confirmButton={TextButton(onClick={repo.deleteRecord(id);confirm=false;back()}){Text("删除")}},dismissButton={TextButton(onClick={confirm=false}){Text("取消")}})
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun BreathingScreen(back:()->Unit) {
    val context=LocalContext.current
    val prefs=remember {context.getSharedPreferences("breathing",0)}
    var duration by rememberSaveable {mutableIntStateOf(60)}
    var remaining by rememberSaveable {mutableIntStateOf(60)}
    var running by rememberSaveable {mutableStateOf(false)}
    var step by rememberSaveable {mutableIntStateOf(0)}
    var completed by rememberSaveable {mutableStateOf(false)}
    val owner=LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer=LifecycleEventObserver {_,event->if(event==Lifecycle.Event.ON_STOP) running=false}
        owner.lifecycle.addObserver(observer); onDispose {owner.lifecycle.removeObserver(observer)}
    }
    LaunchedEffect(running) {
        while(running && remaining>0) {delay(1000);remaining--;step=(step+1)%10
            if(remaining==0) {running=false;completed=true;prefs.edit().putLong("totalSeconds",prefs.getLong("totalSeconds",0)+duration).apply()}
        }
    }
    val scale by animateFloatAsState(if(step<6) 1.12f else .82f,tween(if(running) 3800 else 400),label="breathing")
    Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF0B1B1C),Color(0xFF0B111A))))) {
        Column(horizontalAlignment=Alignment.CenterHorizontally) {
            WellnessTopBar("呼吸练习",back)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(24.dp)) {
                SingleChoiceSegmentedButtonRow {listOf(1,3,5).forEachIndexed {index,m->SegmentedButton(selected=duration==m*60,onClick={duration=m*60;remaining=duration;step=0;running=false;completed=false},shape=SegmentedButtonDefaults.itemShape(index,3)) {Text("$m 分钟")}}}
                Spacer(Modifier.height(18.dp))
                Box(Modifier.size(230.dp).scale(scale).background(Color(0xFF588C88).copy(.4f),CircleShape),contentAlignment=Alignment.Center) {
                    Column(horizontalAlignment=Alignment.CenterHorizontally) {Text(if(completed) "练习完成" else if(!running) "准备好了吗" else if(step<4) "慢慢吸气" else if(step<6) "轻轻停留" else "缓缓呼气",style=MaterialTheme.typography.headlineSmall);Text("%02d:%02d".format(remaining/60,remaining%60),Modifier.padding(top=12.dp))}
                }
                Spacer(Modifier.height(18.dp))
                Text(if(step<4) "让空气自然进入身体，不需要刻意用力" else if(step<6) "放松肩膀，感受这一刻的安静" else "比吸气更慢一点，把紧绷轻轻放下",fontSize=14.sp,color=Color.White.copy(.7f))
                Button(onClick={if(remaining==0){remaining=duration;step=0;completed=false};running=!running},modifier=Modifier.fillMaxWidth()){Text(if(running) "暂停练习" else if(remaining<duration && remaining>0) "继续练习" else "开始练习")}
                if(completed) NoteCard("本次 ${duration/60} 分钟练习已完成，练习时长已保存在本机。")
                Text("保持自然舒适的呼吸，感到不适时请停止练习。",style=MaterialTheme.typography.bodySmall,textAlign=TextAlign.Center)
            }
        }
    }
}
