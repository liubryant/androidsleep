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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import cn.cjym.timesleep.data.model.NoiseSample
import kotlin.math.max

/**
 * 噪音曲线图：折线 + 面积，对应 iOS `Chart` 中的 `LineMark` + `AreaMark`。
 */
@Composable
fun NoiseChart(samples: List<NoiseSample>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    val areaColor = lineColor.copy(alpha = 0.12f)
    val strokeWidth = with(LocalDensity.current) { 2.dp.toPx() }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp),
    ) {
        if (samples.size < 2) return@Canvas

        val minTime = samples.first().time
        val maxTime = samples.last().time
        val timeRange = max((maxTime - minTime).toDouble(), 1.0)
        val maxDb = max(samples.maxOf { it.decibel }, 10.0)

        val points = samples.map { sample ->
            val x = ((sample.time - minTime) / timeRange * size.width).toFloat()
            val y = (size.height * (1 - (sample.decibel / maxDb).coerceIn(0.0, 1.0))).toFloat()
            Offset(x, y)
        }

        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                lineTo(points[i].x, points[i].y)
            }
        }

        val areaPath = Path().apply {
            addPath(linePath)
            lineTo(points.last().x, size.height)
            lineTo(points.first().x, size.height)
            close()
        }

        drawPath(areaPath, color = areaColor)
        drawPath(linePath, color = lineColor, style = Stroke(width = strokeWidth))
    }
}
