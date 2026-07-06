/**
 * Author: liuzheng <bryant_liu24@126.com>
 */

package cn.cjym.timesleep.ui.sleep

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import cn.cjym.timesleep.data.model.SampleSleepSession
import cn.cjym.timesleep.data.model.SleepEventType
import cn.cjym.timesleep.data.model.SleepSession
import cn.cjym.timesleep.data.model.SleepTrendCalculator
import cn.cjym.timesleep.data.model.SleepTrendPoint
import cn.cjym.timesleep.data.model.SleepTrendRange
import cn.cjym.timesleep.service.SleepMonitorManager
import cn.cjym.timesleep.ui.shared.AssetCoverImage
import cn.cjym.timesleep.ui.shared.EmptyState
import cn.cjym.timesleep.ui.sleep.charts.SleepTrendChart
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * 睡眠 Tab 主界面，对应 iOS `SleepHomeView`：顶部"开始睡眠/睡眠报告"分页、
 * 监测状态面板、事件统计、趋势图与历史报告列表。
 */
@Composable
fun SleepHomeScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val isMonitoring by SleepMonitorManager.isMonitoring.collectAsState()
    val latestSession by SleepMonitorManager.latestSession.collectAsState()
    val sessions by SleepMonitorManager.sessions.collectAsState()
    val currentDecibel by SleepMonitorManager.currentDecibel.collectAsState()
    val permissionDenied by SleepMonitorManager.permissionDenied.collectAsState()
    val reportTooShort by SleepMonitorManager.reportTooShort.collectAsState()
    var trendRange by remember { mutableStateOf(SleepTrendRange.week) }
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            SleepMonitorManager.start(context)
        } else {
            SleepMonitorManager.start(context)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        SleepTopBar(
            currentPage = pagerState.currentPage,
            onPageSelected = { page -> scope.launch { pagerState.animateScrollToPage(page) } },
        )

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (page == 0) {
                    StatusPanel(
                        isMonitoring = isMonitoring,
                        currentDecibel = currentDecibel,
                        onToggle = {
                            if (isMonitoring) {
                                SleepMonitorManager.stop(context)
                            } else {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    SleepMonitorManager.start(context)
                                } else {
                                    microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                    )
                    SleepEventSummaryGrid(session = latestSession)
                }

                TrendPanel(sessions = sessions, range = trendRange, onRangeChange = { trendRange = it })

                if (latestSession != null) {
                    SleepReportCard(session = latestSession!!)
                } else {
                    SleepReportCard(session = SampleSleepSession.instance, isSample = true)
                }

                if (sessions.isNotEmpty()) {
                    RecentReports(
                        sessions = sessions,
                        latestSession = latestSession,
                        onSelect = { SleepMonitorManager.select(it) },
                    )
                }
            }
        }
    }

    if (permissionDenied) {
        AlertDialog(
            onDismissRequest = { SleepMonitorManager.dismissPermissionAlert() },
            confirmButton = {
                TextButton(onClick = { SleepMonitorManager.dismissPermissionAlert() }) {
                    Text("知道了")
                }
            },
            title = { Text("需要麦克风权限") },
            text = { Text("请在系统设置中允许麦克风访问，才能进行睡眠声音监测。") },
        )
    }

    if (reportTooShort) {
        AlertDialog(
            onDismissRequest = { SleepMonitorManager.dismissReportTooShortAlert() },
            confirmButton = {
                TextButton(onClick = { SleepMonitorManager.dismissReportTooShortAlert() }) {
                    Text("知道了")
                }
            },
            title = { Text("未生成睡眠报告") },
            text = { Text("本次睡眠时长不足 2 小时，不会生成睡眠报告。") },
        )
    }
}

@Composable
private fun SleepTopBar(
    currentPage: Int,
    onPageSelected: (Int) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf("开始睡眠" to 0, "睡眠报告" to 1).forEach { (title, page) ->
                val selected = currentPage == page
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onPageSelected(page) },
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(9.dp))
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusPanel(isMonitoring: Boolean, currentDecibel: Double, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(14.dp)),
    ) {
        AssetCoverImage(assetPath = "SoundResources/011_春雨/cover.jpg", modifier = Modifier.fillMaxSize())

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.14f), Color.Black.copy(alpha = 0.58f)),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isMonitoring) "正在监测" else "今晚睡眠",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isMonitoring) {
                            "环境音量 ${currentDecibel.toInt()} dB"
                        } else {
                            "开始后会在本地识别打鼾、咳嗽、梦话、磨牙和噪音"
                        },
                        color = Color.White.copy(alpha = 0.86f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Icon(
                    imageVector = if (isMonitoring) Icons.Filled.FiberManualRecord else Icons.Filled.Bedtime,
                    contentDescription = null,
                    tint = if (isMonitoring) Color(0xFFF44336) else Color.White,
                    modifier = Modifier.size(44.dp),
                )
            }

            Button(
                onClick = onToggle,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isMonitoring) Color(0xFFF44336) else MaterialTheme.colorScheme.primary,
                ),
            ) {
                Icon(
                    imageVector = if (isMonitoring) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = if (isMonitoring) "结束睡眠" else "开始睡眠", color = Color.White)
            }
        }
    }
}

@Composable
private fun SleepEventSummaryGrid(session: SleepSession?) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SleepEventType.entries.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { type ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                    ) {
                        Icon(imageVector = type.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(text = "${session?.eventCount(type) ?: 0}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(text = type.title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TrendPanel(sessions: List<SleepSession>, range: SleepTrendRange, onRangeChange: (SleepTrendRange) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "睡眠趋势",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            TrendRangePicker(range = range, onRangeChange = onRangeChange)
        }

        if (sessions.isEmpty()) {
            EmptyState(
                title = "暂无趋势",
                message = "完成一次睡眠监测后，这里会显示周/月趋势。",
                icon = Icons.AutoMirrored.Filled.ShowChart,
            )
        } else {
            val points = remember(sessions, range) { SleepTrendCalculator.points(sessions, range) }
            SleepTrendChart(points = points, modifier = Modifier.fillMaxWidth())

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TrendMetricBox(title = "平均评分", value = averageScoreText(points), modifier = Modifier.weight(1f))
                TrendMetricBox(title = "打鼾", value = "${points.sumOf { it.snoreCount }} 次", modifier = Modifier.weight(1f))
                TrendMetricBox(title = "磨牙", value = "${points.sumOf { it.bruxismCount }} 次", modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TrendRangePicker(range: SleepTrendRange, onRangeChange: (SleepTrendRange) -> Unit) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SleepTrendRange.entries.forEach { value ->
            val selected = range == value
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(48.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onRangeChange(value) },
            ) {
                Text(
                    text = value.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TrendMetricBox(title: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.titleSmall, maxLines = 1)
    }
}

private fun averageScoreText(points: List<SleepTrendPoint>): String {
    val scored = points.filter { it.score > 0 }
    if (scored.isEmpty()) return "--"
    val average = scored.sumOf { it.score } / scored.size
    return "${average.roundToInt()} 分"
}

@Composable
private fun RecentReports(sessions: List<SleepSession>, latestSession: SleepSession?, onSelect: (SleepSession) -> Unit) {
    val recent = sessions.take(10)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "历史报告", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        recent.forEachIndexed { index, session ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(session) }
                    .padding(vertical = 10.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(28.dp),
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = formatReportDate(session.startTime), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(
                        text = "${durationText(session.duration)} · ${session.events.size} 个事件 · 平均 ${session.averageNoise.toInt()} dB",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (latestSession?.id == session.id) {
                    Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }

            if (index < recent.size - 1) {
                HorizontalDivider()
            }
        }
    }
}

private fun durationText(durationSeconds: Double): String {
    val minutes = (durationSeconds / 60).toInt()
    return if (minutes < 60) "$minutes 分钟" else "${minutes / 60} 小时 ${minutes % 60} 分"
}

private val reportDateFormatter = DateTimeFormatter.ofPattern("M月d日 HH:mm")

private fun formatReportDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(reportDateFormatter)
