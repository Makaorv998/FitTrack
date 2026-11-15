package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

data class FitTrackExtendedColors(
    val isDark: Boolean,
    val backgroundSecondary: Color,
    val surfaceElevated: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val border: Color,
    val auroraPrimary: Color,
    val auroraSecondary: Color,
    val auroraBrush: Brush,
    val glassSurface: Color
)

val LocalFitTrackColors = staticCompositionLocalOf {
    FitTrackExtendedColors(
        isDark = true,
        backgroundSecondary = DarkBackgroundSecondary,
        surfaceElevated = DarkSurfaceElevated,
        success = DarkSuccess,
        warning = DarkWarning,
        danger = DarkError,
        border = DarkOutline,
        auroraPrimary = DarkPrimary,
        auroraSecondary = DarkSecondary,
        auroraBrush = Brush.linearGradient(listOf(DarkPrimary, DarkSecondary)),
        glassSurface = DarkSurfaceElevated.copy(alpha = 0.72f)
    )
}

private val FitTrackDarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2E1E59),
    onPrimaryContainer = Color(0xFFE9D5FF),
    secondary = DarkSecondary,
    onSecondary = Color(0xFF042F3D),
    secondaryContainer = Color(0xFF0C3644),
    onSecondaryContainer = Color(0xFFA5F3FC),
    tertiary = DarkSuccess,
    onTertiary = Color(0xFF052E16),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline,
    error = DarkError,
    onError = Color.White
)

private val FitTrackLightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF4C1D95),
    secondary = LightSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = Color(0xFF155E75),
    tertiary = LightSuccess,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightBackgroundSecondary,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutline,
    error = LightError,
    onError = Color.White
)

val LocalReducedMotionOverride = staticCompositionLocalOf { false }

val FitTrackShapes = Shapes(
    small = RoundedCornerShape(FitTrackRadius.sm),
    medium = RoundedCornerShape(FitTrackRadius.md),
    large = RoundedCornerShape(FitTrackRadius.lg),
    extraLarge = RoundedCornerShape(FitTrackRadius.xl)
)

@Composable
fun FitTrackTheme(
    themePreference: String = "dark",
    reducedMotionOverride: Boolean = false,
    content: @Composable () -> Unit
) {
    val useDark = when (themePreference.lowercase()) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (useDark) FitTrackDarkColorScheme else FitTrackLightColorScheme
    val extendedColors = if (useDark) {
        FitTrackExtendedColors(
            isDark = true,
            backgroundSecondary = DarkBackgroundSecondary,
            surfaceElevated = DarkSurfaceElevated,
            success = DarkSuccess,
            warning = DarkWarning,
            danger = DarkError,
            border = DarkOutline,
            auroraPrimary = DarkPrimary,
            auroraSecondary = DarkSecondary,
            auroraBrush = Brush.linearGradient(listOf(DarkPrimary, DarkSecondary)),
            glassSurface = DarkSurfaceElevated.copy(alpha = 0.72f)
        )
    } else {
        FitTrackExtendedColors(
            isDark = false,
            backgroundSecondary = LightBackgroundSecondary,
            surfaceElevated = LightSurfaceElevated,
            success = LightSuccess,
            warning = LightWarning,
            danger = LightError,
            border = LightOutline,
            auroraPrimary = LightPrimary,
            auroraSecondary = LightSecondary,
            auroraBrush = Brush.linearGradient(listOf(LightPrimary, LightSecondary)),
            glassSurface = LightSurface.copy(alpha = 0.88f)
        )
    }

    CompositionLocalProvider(
        LocalFitTrackColors provides extendedColors,
        LocalReducedMotionOverride provides reducedMotionOverride
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = FitTrackShapes,
            content = content
        )
    }
}
