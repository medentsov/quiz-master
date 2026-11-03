package ru.viktorinavopros.quiz.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val JeopardyNavy = Color(0xFF06143A)
val JeopardyNavyDeep = Color(0xFF030B22)
val JeopardyBlue = Color(0xFF0B2A66)
val JeopardyBlueLight = Color(0xFF164A9E)
val JeopardyCell = Color(0xFF1D5BCB)
val JeopardyCellDark = Color(0xFF103C92)
val JeopardyHeader = Color(0xFF1B4FB8)
val JeopardyHeaderDark = Color(0xFF0C2C73)
val JeopardyGold = Color(0xFFFFC857)
val JeopardyGoldDark = Color(0xFFD99A18)
val JeopardyWhite = Color(0xFFF7F9FF)
val JeopardyMuted = Color(0xFFAFC3F0)
val JeopardyGreen = Color(0xFF35D07F)
val JeopardyGreenDark = Color(0xFF0C6B3A)
val JeopardyRed = Color(0xFFFF5A5F)
val JeopardyRedDark = Color(0xFF7A1C22)

private val JeopardyColorScheme = darkColorScheme(
    primary = JeopardyGold,
    onPrimary = Color(0xFF2A1A00),
    primaryContainer = Color(0xFF5C4300),
    onPrimaryContainer = Color(0xFFFFE0A3),
    secondary = Color(0xFF8FC0FF),
    onSecondary = Color(0xFF002B5C),
    secondaryContainer = JeopardyBlueLight,
    onSecondaryContainer = JeopardyWhite,
    tertiary = JeopardyGreen,
    onTertiary = Color(0xFF00391C),
    tertiaryContainer = JeopardyGreenDark,
    onTertiaryContainer = Color(0xFFB8F5D2),
    background = JeopardyNavy,
    onBackground = JeopardyWhite,
    surface = JeopardyBlue,
    onSurface = JeopardyWhite,
    surfaceVariant = Color(0xFF123570),
    onSurfaceVariant = JeopardyMuted,
    surfaceContainer = Color(0xFF0A2354),
    surfaceContainerHigh = Color(0xFF0E2C66),
    surfaceContainerHighest = Color(0xFF123A80),
    outline = Color(0xFF4F72BC),
    outlineVariant = Color(0xFF2A4A8E),
    error = JeopardyRed,
    onError = Color(0xFF3B0508),
    errorContainer = JeopardyRedDark,
    onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = JeopardyWhite,
    inverseOnSurface = JeopardyNavy,
    inversePrimary = Color(0xFF7A5900),
    scrim = Color(0xCC000000),
)

private val JeopardyTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 52.sp,
        lineHeight = 56.sp,
        letterSpacing = 3.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 40.sp,
        lineHeight = 44.sp,
        letterSpacing = 2.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 25.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.3.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
)

private val JeopardyShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun ViktorinaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = JeopardyColorScheme,
        typography = JeopardyTypography,
        shapes = JeopardyShapes,
        content = content,
    )
}
