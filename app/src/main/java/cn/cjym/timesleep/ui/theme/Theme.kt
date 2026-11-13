/**
 * Author: liuzheng <bryant_liu24@126.com>
 */

package cn.cjym.timesleep.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Indigo,
    onPrimary = DarkBackground,
    secondary = IndigoLight,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSecondarySurface,
)

private val DarkColors = darkColorScheme(
    primary = IndigoLight,
    onPrimary = DarkBackground,
    secondary = IndigoDark,
    onBackground = androidx.compose.ui.graphics.Color.White,
    onSurface = androidx.compose.ui.graphics.Color.White,
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFA6A6A6),
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSecondarySurface,
)

@Composable
fun TimeSleepTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = TimeSleepTypography,
        content = content,
    )
}
