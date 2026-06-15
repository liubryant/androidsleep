package cn.cjym.timesleep.ui.sleep.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import cn.cjym.timesleep.data.model.SleepDistribution
import cn.cjym.timesleep.data.model.SleepStage
import kotlin.math.min

/**
 * 睡眠分布饼图（环形图），对应 iOS `SleepDistributionPieChart` + `PieSliceShape`。
 */
@Composable
fun SleepDistributionPieChart(distribution: SleepDistribution, modifier: Modifier = Modifier) {
    val holeColor = MaterialTheme.colorScheme.surface

    Canvas(modifier = modifier.aspectRatio(1f)) {
        val total = SleepStage.entries.sumOf { distribution.duration(it) }
        if (total <= 0) return@Canvas

        val diameter = min(size.width, size.height)
        val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
        val arcSize = Size(diameter, diameter)

        var startAngle = -90f
        SleepStage.entries.forEach { stage ->
            val duration = distribution.duration(stage)
            if (duration <= 0) return@forEach
            val sweep = (duration / total * 360).toFloat()
            drawArc(
                color = stage.chartColor,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = true,
                topLeft = topLeft,
                size = arcSize,
            )
            startAngle += sweep
        }

        drawCircle(
            color = holeColor,
            radius = diameter * 0.28f,
            center = Offset(size.width / 2, size.height / 2),
        )
    }
}
