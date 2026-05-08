package com.example.rhythmtrainer.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Purple700 = Color(0xFF6A1B9A)
val Purple500 = Color(0xFF9C27B0)
val Purple300 = Color(0xFFBA68C8)
val Purple100 = Color(0xFFE1BEE7)
val DeepPurple = Color(0xFF311B92)
val Amber500 = Color(0xFFFFC107)
val Amber700 = Color(0xFFFFA000)
val DarkSurface = Color(0xFF1E1E2E)
val DarkBackground = Color(0xFF121218)
val LightBackground = Color(0xFFF8F5FF)
val LightSurface = Color(0xFFFFFFFF)
val TextOnDark = Color(0xFFE8E8F0)
val TextOnLight = Color(0xFF1A1A2E)
val AccentGradientStart = Color(0xFF7C4DFF)
val AccentGradientEnd = Color(0xFF448AFF)

private val LightColorScheme = lightColorScheme(
    primary = Purple700,
    onPrimary = Color.White,
    primaryContainer = Purple100,
    onPrimaryContainer = DeepPurple,
    secondary = Amber500,
    onSecondary = Color.Black,
    secondaryContainer = Amber700,
    onSecondaryContainer = Color.White,
    background = LightBackground,
    onBackground = TextOnLight,
    surface = LightSurface,
    onSurface = TextOnLight,
    surfaceVariant = Color(0xFFEDE7F6),
    onSurfaceVariant = Color(0xFF49454E),
    error = Color(0xFFB00020),
    onError = Color.White,
)

private val DarkColorScheme = darkColorScheme(
    primary = Purple300,
    onPrimary = Color.Black,
    primaryContainer = Purple700,
    onPrimaryContainer = Purple100,
    secondary = Amber500,
    onSecondary = Color.Black,
    secondaryContainer = Amber700,
    onSecondaryContainer = Color.White,
    background = DarkBackground,
    onBackground = TextOnDark,
    surface = DarkSurface,
    onSurface = TextOnDark,
    surfaceVariant = Color(0xFF2D2D3F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    error = Color(0xFFCF6679),
    onError = Color.Black,
)

@Composable
fun RhythmTrainerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}

val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.25.sp
    ),
    labelLarge = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.sp
    ),
)