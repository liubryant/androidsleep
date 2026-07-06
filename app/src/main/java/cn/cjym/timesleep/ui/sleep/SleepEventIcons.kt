/**
 * Author: liuzheng <bryant_liu24@126.com>
 */

package cn.cjym.timesleep.ui.sleep

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.PauseCircleFilled
import androidx.compose.material.icons.filled.Sick
import androidx.compose.ui.graphics.vector.ImageVector
import cn.cjym.timesleep.data.model.SleepEventType

/** 与 iOS `SleepEventType.symbolName` 对应的图标映射。 */
val SleepEventType.icon: ImageVector
    get() = when (this) {
        SleepEventType.snore -> Icons.Filled.Air
        SleepEventType.cough -> Icons.Filled.Sick
        SleepEventType.sleepTalk -> Icons.Filled.ChatBubble
        SleepEventType.bruxism -> Icons.Filled.Face
        SleepEventType.noise -> Icons.AutoMirrored.Filled.VolumeUp
        SleepEventType.heavyBreathing -> Icons.Filled.Air
        SleepEventType.nasalCongestion -> Icons.Filled.MedicalServices
        SleepEventType.fart -> Icons.Filled.Cloud
        SleepEventType.breathHolding -> Icons.Filled.PauseCircleFilled
    }
