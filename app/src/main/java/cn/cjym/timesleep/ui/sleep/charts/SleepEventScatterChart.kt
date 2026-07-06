/**
 * Author: liuzheng <bryant_liu24@126.com>
 */

package cn.cjym.timesleep.ui.sleep.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.cjym.timesleep.data.model.SleepEvent
import cn.cjym.timesleep.data.model.SleepEventType
import kotlin.math.max

/**
 * 事件散点图：横轴为时间，纵轴为事件类型（9 类），对应 iOS `Chart` 中的 `PointMark`。
 */
@Composable
fun SleepEventScatterChart(events: List<SleepEvent>, modifier: Modifier = Modifier) {
    val dotRadius = with(androidx.compose.ui.platform.LocalDensity.current) { 4.dp.toPx() }
    val types = SleepEventType.entries

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
        ) {
            if (events.isEmpty()) return@Canvas

            val minTime = events.minOf { it.startTime }
            val maxTime = events.maxOf { it.startTime }
            val timeRange = max((maxTime - minTime).toDouble(), 1.0)
            val rowHeight = size.height / types.size

            types.indices.forEach { index ->
                val y = rowHeight * index + rowHeight / 2
                drawLine(
                    color = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.15f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f,
                )
            }

            events.forEach { event ->
                val x = if (maxTime == minTime) {
                    size.width / 2
                } else {
                    ((event.startTime - minTime) / timeRange * size.width).toFloat()
                }
                val typeIndex = types.indexOf(event.type)
                val y = rowHeight * typeIndex + rowHeight / 2
                drawCircle(color = event.type.chartColor, radius = dotRadius, center = Offset(x, y))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        SleepEventLegend(types = types)
    }
}

@Composable
private fun SleepEventLegend(types: List<SleepEventType>) {
    val rows = types.chunked(3)
    Column {
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { type ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 2.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(type.chartColor),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = type.title,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
