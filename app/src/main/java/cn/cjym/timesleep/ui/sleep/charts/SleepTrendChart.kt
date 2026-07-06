/**
 * Author: liuzheng <bryant_liu24@126.com>
 */

package cn.cjym.timesleep.ui.sleep.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cn.cjym.timesleep.data.model.SleepTrendPoint
import kotlin.math.max

/**
 * 睡眠趋势图：柱状图表示每日睡眠时长（小时），折线图表示每日评分 / 12，
 * 对应 iOS `Chart` 中的 `BarMark` + `LineMark` 组合。
 */
@Composable
fun SleepTrendChart(points: List<SleepTrendPoint>, modifier: Modifier = Modifier) {
    val barColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    val lineColor = Color(0xFF4CAF50)
    val strokeWidth = with(androidx.compose.ui.platform.LocalDensity.current) { 2.dp.toPx() }
    val dotRadius = with(androidx.compose.ui.platform.LocalDensity.current) { 3.dp.toPx() }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
    ) {
        if (points.isEmpty()) return@Canvas

        val maxDuration = max(points.maxOf { it.durationHours }, 1.0)
        val maxScore = max(points.maxOf { it.score / 12.0 }, 1.0)
        val maxY = max(maxDuration, maxScore)

        val slotWidth = size.width / points.size
        val barWidth = slotWidth * 0.5f

        points.forEachIndexed { index, point ->
            val centerX = slotWidth * index + slotWidth / 2
            val barHeight = (point.durationHours / maxY * size.height).toFloat()
            if (barHeight > 0) {
                drawRect(
                    color = barColor,
                    topLeft = Offset(centerX - barWidth / 2, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                )
            }
        }

        val linePoints = points.mapIndexed { index, point ->
            val centerX = slotWidth * index + slotWidth / 2
            val y = size.height - (point.score / 12.0 / maxY * size.height).toFloat()
            Offset(centerX, y)
        }

        for (i in 0 until linePoints.size - 1) {
            if (points[i].score <= 0 || points[i + 1].score <= 0) continue
            drawLine(
                color = lineColor,
                start = linePoints[i],
                end = linePoints[i + 1],
                strokeWidth = strokeWidth,
            )
        }

        points.forEachIndexed { index, point ->
            if (point.score > 0) {
                drawCircle(color = lineColor, radius = dotRadius, center = linePoints[index])
            }
        }
    }
}
