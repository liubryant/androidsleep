package cn.cjym.timesleep.ui.wellness

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable fun HealthInsightScreen(back:()->Unit) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var status by remember {mutableIntStateOf(HealthConnectClient.getSdkStatus(context))}
    val permissions=remember {setOf(HealthPermission.getReadPermission(SleepSessionRecord::class))}
    var granted by remember {mutableStateOf(false)}
    var sessions by remember {mutableStateOf(emptyList<SleepSessionRecord>())}
    var loading by remember {mutableStateOf(false)}
    var error by remember {mutableStateOf<String?>(null)}
    fun refresh() {scope.launch {
        loading=true;error=null
        try {
            status=HealthConnectClient.getSdkStatus(context)
            if(status==HealthConnectClient.SDK_AVAILABLE) {
                val client=HealthConnectClient.getOrCreate(context)
                granted=client.permissionController.getGrantedPermissions().containsAll(permissions)
                if(granted) {
                    val records=mutableListOf<SleepSessionRecord>();var token:String?=null
                    do {
                        val page=client.readRecords(ReadRecordsRequest(SleepSessionRecord::class,timeRangeFilter=TimeRangeFilter.between(Instant.now().minus(Duration.ofDays(30)),Instant.now()),pageToken=token))
                        records.addAll(page.records);token=page.pageToken
                    } while(token!=null)
                    sessions=records.sortedByDescending{it.endTime}
                } else sessions=emptyList()
            }
        } catch(e:Exception){error="健康数据读取失败，请检查授权后重试。"} finally {loading=false}
    }}
    val launcher=rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) {refresh()}
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {val observer=LifecycleEventObserver{_,e->if(e==Lifecycle.Event.ON_RESUME) refresh()};lifecycle.addObserver(observer);onDispose{lifecycle.removeObserver(observer)}}
    LaunchedEffect(Unit){refresh()}
    Column {
        WellnessTopBar("睡眠洞察",back)
        LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
            item {SceneryCard("连接你的睡眠数据","健康连接 · 最近 30 天",14,Modifier.fillMaxWidth().height(180.dp)){refresh()}}
            item {NoteCard("授权后，仅在本机读取健康连接中的睡眠时间和阶段。不会上传这些健康记录；你可以在系统健康连接中撤销权限。")}
            item {
                when(status) {
                    HealthConnectClient.SDK_UNAVAILABLE -> NoteCard("此设备不支持健康连接。你仍可使用应用内睡眠监测和测评。")
                    HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> Button(onClick={runCatching {context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")))}}){Text("安装或更新健康连接")}
                    else -> Button(onClick={if(granted) refresh() else launcher.launch(permissions)},enabled=!loading){Text(if(loading) "读取中…" else if(granted) "刷新健康数据" else "授权读取睡眠数据")}
                }
            }
            error?.let{item{NoteCard(it)}}
            if(granted && sessions.isEmpty() && !loading) item {NoteCard("还没有睡眠记录。请先让手表或健康应用将睡眠数据写入健康连接，再回来刷新。")}
            items(sessions,key={it.metadata.id}) {session->
                val date=DateTimeFormatter.ofPattern("MM月dd日 HH:mm").withZone(ZoneId.systemDefault())
                Surface(shape=androidx.compose.foundation.shape.RoundedCornerShape(18.dp)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        Text("${date.format(session.startTime)} — ${date.format(session.endTime)}",style=MaterialTheme.typography.titleMedium)
                        Text("记录时长 ${durationText(Duration.between(session.startTime,session.endTime).toMinutes())}")
                        Text("数据来源：${session.metadata.dataOrigin.packageName}",style=MaterialTheme.typography.bodySmall)
                        if(session.stages.isEmpty()) Text("数据源未提供睡眠阶段",color=MaterialTheme.colorScheme.onSurfaceVariant)
                        else listOf(SleepSessionRecord.STAGE_TYPE_DEEP to "深睡",SleepSessionRecord.STAGE_TYPE_LIGHT to "浅睡",SleepSessionRecord.STAGE_TYPE_REM to "快速眼动",SleepSessionRecord.STAGE_TYPE_AWAKE to "清醒").forEach { (stage,label)->
                            val minutes=session.stages.filter{it.stage==stage}.sumOf{Duration.between(it.startTime,it.endTime).toMinutes()}
                            Row(Modifier.fillMaxWidth()){Text(label,Modifier.weight(1f));Text(durationText(minutes))}
                        }
                    }
                }
            }
        }
    }
}
private fun durationText(minutes:Long)="${minutes/60}小时 ${minutes%60}分"

@Composable fun DevicesScreen(back:()->Unit,navigate:(String)->Unit) {
    var selected by remember {mutableStateOf<String?>(null)}
    Column {
        WellnessTopBar("智能设备",back)
        LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            item{SceneryCard("让设备记录，帮你理解睡眠","通过健康连接读取已同步的数据",19,Modifier.fillMaxWidth().height(180.dp)){navigate("health")}}
            item{Button(onClick={navigate("health")},modifier=Modifier.fillMaxWidth()){Text("打开健康连接与睡眠洞察")}}
            items(listOf("华为运动健康","小米运动健康 / Zepp Life","Samsung Health","Fitbit / Pixel Watch","Garmin Connect","Oura / 智能戒指")) {name->OutlinedButton(onClick={selected=name},modifier=Modifier.fillMaxWidth()){Text(name)}}
            item{NoteCard("这里提供数据连接指南，不会直接配对蓝牙设备。是否支持健康连接，取决于设备厂商、应用版本及地区。")}
        }
    }
    selected?.let{name->AlertDialog(onDismissRequest={selected=null},title={Text(name)},text={Text("1. 在设备官方应用中完成配对，并同步睡眠记录。\n\n2. 查看该应用是否支持“健康连接 / Health Connect”，如支持，允许写入睡眠数据。\n\n3. 返回时光睡眠，授权读取并刷新。\n\n如果官方应用不支持健康连接，当前无法直接读取该设备数据。")},confirmButton={TextButton(onClick={selected=null;navigate("health")}){Text("查看健康数据")}},dismissButton={TextButton(onClick={selected=null}){Text("关闭")}})}
}
