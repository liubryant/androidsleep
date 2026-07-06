/**
 * Author: liuzheng <bryant_liu24@126.com>
 */

package cn.cjym.timesleep.ui.sleep.charts

import androidx.compose.ui.graphics.Color
import cn.cjym.timesleep.data.model.SleepEventType
import cn.cjym.timesleep.data.model.SleepStage

/** 睡眠阶段配色，与 iOS `SleepStage.color` 一致：深睡=indigo / 浅睡=cyan / 做梦=purple / 觉醒=orange。 */
val SleepStage.chartColor: Color
    get() = when (this) {
        SleepStage.deep -> Color(0xFF3F51B5)
        SleepStage.light -> Color(0xFF00BCD4)
        SleepStage.rem -> Color(0xFF9C27B0)
        SleepStage.awake -> Color(0xFFFF9800)
    }

private val eventColorPalette = listOf(
    Color(0xFF3F51B5), Color(0xFF00BCD4), Color(0xFF9C27B0), Color(0xFFFF9800),
    Color(0xFF4CAF50), Color(0xFFF44336), Color(0xFF795548), Color(0xFF607D8B), Color(0xFFE91E63),
)

/** 事件类型在散点图/图例中的配色。 */
val SleepEventType.chartColor: Color
    get() = eventColorPalette[ordinal % eventColorPalette.size]
