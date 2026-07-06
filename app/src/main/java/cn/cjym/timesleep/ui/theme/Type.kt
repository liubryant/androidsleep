/**
 * Author: liuzheng <bryant_liu24@126.com>
 */

package cn.cjym.timesleep.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import cn.cjym.timesleep.R

private val MiSans = FontFamily(
    Font(R.font.misans_normal, FontWeight.Normal),
    Font(R.font.misans_medium, FontWeight.Medium),
    Font(R.font.misans_demibold, FontWeight.SemiBold),
    Font(R.font.misans_demibold, FontWeight.Bold),
)

private val DefaultTypography = Typography()

val TimeSleepTypography = Typography(
    displayLarge = DefaultTypography.displayLarge.copy(fontFamily = MiSans),
    displayMedium = DefaultTypography.displayMedium.copy(fontFamily = MiSans),
    displaySmall = DefaultTypography.displaySmall.copy(fontFamily = MiSans),
    headlineLarge = DefaultTypography.headlineLarge.copy(fontFamily = MiSans),
    headlineMedium = DefaultTypography.headlineMedium.copy(fontFamily = MiSans),
    headlineSmall = DefaultTypography.headlineSmall.copy(fontFamily = MiSans),
    titleLarge = DefaultTypography.titleLarge.copy(fontFamily = MiSans),
    titleMedium = DefaultTypography.titleMedium.copy(fontFamily = MiSans),
    titleSmall = DefaultTypography.titleSmall.copy(fontFamily = MiSans),
    bodyLarge = DefaultTypography.bodyLarge.copy(fontFamily = MiSans),
    bodyMedium = DefaultTypography.bodyMedium.copy(fontFamily = MiSans),
    bodySmall = DefaultTypography.bodySmall.copy(fontFamily = MiSans),
    labelLarge = DefaultTypography.labelLarge.copy(fontFamily = MiSans),
    labelMedium = DefaultTypography.labelMedium.copy(fontFamily = MiSans),
    labelSmall = DefaultTypography.labelSmall.copy(fontFamily = MiSans),
)
