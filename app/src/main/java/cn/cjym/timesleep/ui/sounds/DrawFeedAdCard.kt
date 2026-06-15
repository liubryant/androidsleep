package cn.cjym.timesleep.ui.sounds

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.cjym.timesleep.AppConstants

/**
 * 穿山甲 Draw Feed 信息流广告占位，[AppConstants.isAdDisabled] 为 true 时不渲染任何内容。
 */
@Composable
fun DrawFeedAdCard(modifier: Modifier = Modifier) {
    if (AppConstants.isAdDisabled) return

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.52f),
    ) {}
}
