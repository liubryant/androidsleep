package cn.cjym.timesleep.ui.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun IOSSlider(value:Float,onValueChange:(Float)->Unit,modifier:Modifier=Modifier,onValueChangeFinished:(()->Unit)?=null,valueRange:ClosedFloatingPointRange<Float> = 0f..1f,enabled:Boolean=true,tint:Color=Color.White) {
    val fraction=((value-valueRange.start)/(valueRange.endInclusive-valueRange.start).coerceAtLeast(.001f)).coerceIn(0f,1f)
    Slider(value=value.coerceIn(valueRange),onValueChange=onValueChange,onValueChangeFinished=onValueChangeFinished,valueRange=valueRange,enabled=enabled,modifier=modifier.height(30.dp),
        thumb={Box(Modifier.size(20.dp).clip(CircleShape).background(if(enabled) tint else tint.copy(alpha=.35f)))},
        track={Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(tint.copy(alpha=.2f))) {Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(if(enabled) tint else tint.copy(alpha=.35f)))}})
}
