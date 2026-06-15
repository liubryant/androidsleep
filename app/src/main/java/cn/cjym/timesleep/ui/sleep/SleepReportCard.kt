package cn.cjym.timesleep.ui.sleep

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cn.cjym.timesleep.AppConstants
import cn.cjym.timesleep.data.model.SleepDistribution
import cn.cjym.timesleep.data.model.SleepEvent
import cn.cjym.timesleep.data.model.SleepEventType
import cn.cjym.timesleep.data.model.SleepSession
import cn.cjym.timesleep.data.model.SleepStage
import cn.cjym.timesleep.service.SleepSessionStore
import cn.cjym.timesleep.ui.sleep.charts.NoiseChart
import cn.cjym.timesleep.ui.sleep.charts.SleepDistributionPieChart
import cn.cjym.timesleep.ui.sleep.charts.SleepEventScatterChart
import cn.cjym.timesleep.ui.sleep.charts.chartColor
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 睡眠报告卡片，对应 iOS `SleepReportView`：评分、关键指标、录音回放、
 * 事件散点图、噪音曲线、睡眠分布饼图与事件列表。
 */
@Composable
fun SleepReportCard(session: SleepSession, modifier: Modifier = Modifier) {
    val distribution = session.sleepDistribution

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "睡眠报告",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${session.sleepScore}",
                style = MaterialTheme.typography.headlineMedium,
                color = scoreColor(session.sleepScore),
            )
            Text(
                text = "分",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricBox(title = "时长", value = durationText(session.duration), modifier = Modifier.weight(1f))
            MetricBox(title = "平均噪音", value = "${session.averageNoise.toInt()} dB", modifier = Modifier.weight(1f))
            MetricBox(title = "峰值", value = "${session.maxNoise.toInt()} dB", modifier = Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricBox(title = "睡眠效率", value = "${session.sleepEfficiency.roundToInt()}%", modifier = Modifier.weight(1f))
            MetricBox(title = "睡眠年龄", value = "${session.sleepAge} 岁", modifier = Modifier.weight(1f))
            MetricBox(
                title = "深睡时长",
                value = durationText(distribution.duration(SleepStage.deep)),
                modifier = Modifier.weight(1f),
            )
        }

        RecordingControl(session = session)

        EventSummaryGrid(session = session)

        if (session.events.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "声音事件", style = MaterialTheme.typography.titleSmall)
                SleepEventScatterChart(events = session.events)
            }
        }

        if (session.noiseSamples.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "噪音曲线", style = MaterialTheme.typography.titleSmall)
                NoiseChart(samples = session.noiseSamples)
            }
        }

        SleepDistributionSection(distribution = distribution)

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = "事件列表", style = MaterialTheme.typography.titleSmall)
            session.events.takeLast(20).reversed().forEach { event ->
                EventRow(event = event)
            }
        }
    }
}

@Composable
private fun RecordingControl(session: SleepSession) {
    val context = LocalContext.current
    var isPlaying by remember(session.id) { mutableStateOf(false) }
    var mediaPlayer by remember(session.id) { mutableStateOf<android.media.MediaPlayer?>(null) }

    val recordingFile = remember(session.id, session.audioFileName) {
        session.audioFileName?.let { name ->
            val file = SleepSessionStore.recordingFile(context, name)
            if (file.exists()) file else null
        }
    }

    DisposableEffect(session.id) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    val canPlay = recordingFile != null && AppConstants.isAdDisabled

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.GraphicEq,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(34.dp),
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(text = "睡眠录音", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = recordingAvailabilityText(recordingFile != null),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            IconButton(
                enabled = canPlay,
                onClick = {
                    val player = mediaPlayer
                    if (isPlaying && player != null) {
                        player.stop()
                        player.release()
                        mediaPlayer = null
                        isPlaying = false
                    } else if (recordingFile != null) {
                        val newPlayer = android.media.MediaPlayer()
                        runCatching {
                            newPlayer.setDataSource(recordingFile.absolutePath)
                            newPlayer.prepare()
                            newPlayer.setOnCompletionListener {
                                it.release()
                                mediaPlayer = null
                                isPlaying = false
                            }
                            newPlayer.start()
                            mediaPlayer = newPlayer
                            isPlaying = true
                        }
                    }
                },
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "停止" else "播放",
                )
            }
        }

        AnimatedVisibility(visible = isPlaying) {
            RecordingWaveform()
        }
    }
}

private fun recordingAvailabilityText(hasRecording: Boolean): String {
    if (!hasRecording) return "暂无可播放录音"
    return if (AppConstants.isAdDisabled) "可回放夜间声音" else "观看激励视频后可播放"
}

@Composable
private fun RecordingWaveform(modifier: Modifier = Modifier) {
    val barCount = 18
    val transition = rememberInfiniteTransition(label = "waveform")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1600, easing = LinearEasing)),
        label = "time",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(barCount) { index ->
            val phase = time * 2 * Math.PI * 5 + index * 0.55
            val value = (sin(phase) + 1) / 2
            val height = 8.dp + (value * 20).dp
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(height)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)),
            )
        }
    }
}

@Composable
private fun EventSummaryGrid(session: SleepSession) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SleepEventType.entries.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { type ->
                    EventMetricBox(type = type, count = session.eventCount(type), modifier = Modifier.weight(1f))
                }
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun EventMetricBox(type: SleepEventType, count: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(imageVector = type.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(text = "$count", style = MaterialTheme.typography.titleMedium)
        Text(
            text = type.title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SleepDistributionSection(distribution: SleepDistribution) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "睡眠分布", style = MaterialTheme.typography.titleSmall)

        if (distribution.totalDuration <= 0) {
            Text(
                text = "暂无足够数据生成睡眠分布。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SleepDistributionPieChart(distribution = distribution, modifier = Modifier.size(120.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SleepStage.entries.forEach { stage ->
                        StageLegendRow(distribution = distribution, stage = stage)
                    }
                }
            }

            Text(
                text = distributionAnalysisText(distribution),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StageLegendRow(distribution: SleepDistribution, stage: SleepStage) {
    val minutes = (distribution.duration(stage) / 60).toInt()
    val percent = distribution.percentage(stage).roundToInt()

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(stage.chartColor),
        )
        Text(text = stage.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(64.dp))
        Text(text = "$minutes 分钟", style = MaterialTheme.typography.bodyMedium)
        Text(
            text = "$percent%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun EventRow(event: SleepEvent) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Icon(
            imageVector = event.type.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(28.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = event.type.title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = formatTime(event.startTime),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "${(event.confidence * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MetricBox(title: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.titleMedium, maxLines = 1)
    }
}

private fun scoreColor(score: Int): Color = when {
    score >= 85 -> Color(0xFF4CAF50)
    score >= 70 -> Color(0xFF3F51B5)
    score >= 55 -> Color(0xFFFF9800)
    else -> Color(0xFFF44336)
}

private fun durationText(durationSeconds: Double): String {
    val minutes = (durationSeconds / 60).toInt()
    return if (minutes < 60) "$minutes 分钟" else "${minutes / 60} 小时 ${minutes % 60} 分"
}

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private fun formatTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(timeFormatter)

private fun distributionAnalysisText(distribution: SleepDistribution): String {
    val lines = mutableListOf<String>()

    val fallAsleep = distribution.fallAsleepTime
    val wake = distribution.wakeTime
    if (fallAsleep != null && wake != null) {
        lines.add("您在 ${formatTime(fallAsleep)} 开始入睡，${formatTime(wake)} 结束睡眠。")
    }

    val deepMinutes = (distribution.duration(SleepStage.deep) / 60).toInt()
    val deepNote = if (deepMinutes >= 90) "已达到健康参考值" else "低于参考值 90 分钟"
    lines.add("深度睡眠 $deepMinutes 分钟（参考值 >90 分钟，$deepNote）。")

    val lightMinutes = (distribution.duration(SleepStage.light) / 60).toInt()
    lines.add("浅睡眠 $lightMinutes 分钟。")

    val remMinutes = (distribution.duration(SleepStage.rem) / 60).toInt()
    lines.add("做梦 $remMinutes 分钟。")

    val awakeMinutes = (distribution.duration(SleepStage.awake) / 60).toInt()
    if (awakeMinutes > 0) {
        lines.add("睡眠中觉醒约 $awakeMinutes 分钟。")
    }

    return lines.joinToString("\n")
}
