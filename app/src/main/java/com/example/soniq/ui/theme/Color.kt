package com.example.soniq.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// OLED Sonic Flow Pitch-Black (#000000) Palette
// Pure true black foundations shut off OLED subpixels to achieve 0mA drain on dark regions.
val OledPitchBlack = Color(0xFF000000)
val CanvasDeep = Color(0xFF000000)
val CanvasSubtle = Color(0xFF000000)
val SurfaceSlate = Color(0xFF0A0D14)
val SurfaceSlateHigh = Color(0xFF121622)
val SurfaceGlass = Color(0xE6000000)
val SurfaceGlassBorder = Color(0x1AFFFFFF)

// Battery-optimized high-luminance accents:
// Electric cyan and blue provide maximum legibility per subpixel lumen with reduced drive current.
val NeonElectricBlue = Color(0xFF2979FF)
val PrimaryBlue = Color(0xFF0066FF)
val NeonViolet = Color(0xFFA855F7)
val VibrantMagenta = Color(0xFFEC4899)
val NeonCyan = Color(0xFF00E5FF)
val OledEcoGreen = Color(0xFF00E676)

val TextPureWhite = Color(0xFFFFFFFF)
val TextSilver = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

// Universal Media Light Palette
val LightBackground = Color(0xFFFAF8FF)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceLow = Color(0xFFF2F3FF)
val LightSurfaceContainer = Color(0xFFEAEDFF)
val LightSurfaceHigh = Color(0xFFE2E7FD)
val LightTextPrimary = Color(0xFF0D1322)
val LightTextSecondary = Color(0xFF1E293B)
val LightTextMuted = Color(0xFF64748B)

// Sonic Flow Signature Gradients
val SonicFlowGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF0066FF),
        Color(0xFFA855F7),
        Color(0xFFEC4899)
    )
)

val CyanToBlueGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF00E5FF),
        Color(0xFF2979FF)
    )
)

val VioletToMagentaGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFFA855F7),
        Color(0xFFEC4899)
    )
)
