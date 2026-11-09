package com.example.soniq.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = NeonElectricBlue,
    onPrimary = TextPureWhite,
    primaryContainer = PrimaryBlue,
    onPrimaryContainer = TextPureWhite,
    secondary = NeonViolet,
    onSecondary = TextPureWhite,
    secondaryContainer = Color(0xFF4A0080),
    tertiary = VibrantMagenta,
    onTertiary = TextPureWhite,
    background = OledPitchBlack, // Pure #000000 for OLED pixel shutoff
    onBackground = TextPureWhite,
    surface = OledPitchBlack,    // Pure #000000
    onSurface = TextPureWhite,
    surfaceVariant = SurfaceSlate,
    onSurfaceVariant = TextSilver,
    surfaceContainerLowest = OledPitchBlack,
    surfaceContainerLow = Color(0xFF05070A),
    surfaceContainer = SurfaceSlate,
    surfaceContainerHigh = SurfaceSlateHigh,
    surfaceContainerHighest = Color(0xFF181C28),
    outline = TextMuted,
    outlineVariant = SurfaceGlassBorder
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = TextPureWhite,
    primaryContainer = Color(0xFF0050CB),
    onPrimaryContainer = TextPureWhite,
    secondary = Color(0xFF7D2DCE),
    onSecondary = TextPureWhite,
    secondaryContainer = Color(0xFF974CE9),
    tertiary = Color(0xFFB10057),
    onTertiary = TextPureWhite,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceContainer,
    onSurfaceVariant = LightTextSecondary,
    surfaceContainer = LightSurfaceLow,
    surfaceContainerHigh = LightSurfaceHigh,
    outline = LightTextMuted,
    outlineVariant = Color(0x1F0D1322)
)

@Composable
fun SoniqTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                // Configure status bar and navigation bar with pitch-black on OLED
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
