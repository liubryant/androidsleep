package cn.cjym.timesleep.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Indigo,
    onPrimary = LightSurface,
    secondary = IndigoLight,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSecondarySurface,
)

private val DarkColors = darkColorScheme(
    primary = IndigoLight,
    onPrimary = DarkBackground,
    secondary = IndigoDark,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSecondarySurface,
)

@Composable
fun TimeSleepTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = TimeSleepTypography,
        content = content,
    )
}
