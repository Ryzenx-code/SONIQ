package com.example.soniq.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.soniq.ui.theme.NeonCyan
import com.example.soniq.ui.theme.NeonElectricBlue
import com.example.soniq.ui.theme.NeonViolet
import com.example.soniq.ui.theme.OledPitchBlack
import com.example.soniq.ui.theme.SurfaceSlate
import com.example.soniq.ui.theme.SurfaceSlateHigh
import com.example.soniq.ui.theme.VibrantMagenta
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

enum class VisualizerMode(val label: String) {
    SPECTRUM("SPECTRUM"),
    OSCILLOSCOPE("WAVEFORM"),
    RADIAL("ORBITAL")
}

/**
 * High-performance real-time audio visualizer utilizing the Jetpack Compose Canvas API.
 * Dynamically scales its waveform, spectrum bars, and glow intensity based on media playback volume
 * and playback state.
 */
@Composable
fun RealtimeAudioVisualizer(
    volume: Float,
    isPlaying: Boolean,
    bandGains: List<Float> = emptyList(),
    modifier: Modifier = Modifier,
    initialMode: VisualizerMode = VisualizerMode.SPECTRUM,
    showModeSelector: Boolean = true
) {
    var mode by remember { mutableStateOf(initialMode) }

    // Multi-phase continuous animation loop: only run transition while isPlaying is true
    val phaseState: androidx.compose.runtime.State<Float>
    val fastPhaseState: androidx.compose.runtime.State<Float>

    if (isPlaying) {
        val infiniteTransition = rememberInfiniteTransition(label = "audio_visualizer_engine")
        phaseState = infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 2f * PI.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(2800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "phase"
        )
        fastPhaseState = infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 2f * PI.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "fast_phase"
        )
    } else {
        phaseState = remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
        fastPhaseState = remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }

    // Dynamic volume multiplier: scales all visual components directly by volume (0f..1f)
    val activeVolumeFactor = if (isPlaying) volume.coerceIn(0f, 1f) else (volume * 0.06f).coerceIn(0f, 0.06f)

    // Reusable cached paths
    val wavePath = remember { Path() }
    val underGlowPath = remember { Path() }
    val wave2Path = remember { Path() }

    // Cached brushes
    val spectrumBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                VibrantMagenta,
                NeonViolet,
                NeonElectricBlue,
                NeonCyan
            )
        )
    }
    val waveBrush = remember {
        Brush.horizontalGradient(
            colors = listOf(NeonCyan, NeonElectricBlue, NeonViolet, VibrantMagenta)
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(OledPitchBlack)
            .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(20.dp))
            .padding(12.dp)
            .testTag("realtime_audio_visualizer")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar: Mode Switcher & Telemetry
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live Status Badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isPlaying && activeVolumeFactor > 0.05f) NeonCyan
                                else if (isPlaying) Color(0xFFF59E0B)
                                else Color(0xFF64748B)
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPlaying) "DSP VISUALIZER • LIVE" else "DSP VISUALIZER • IDLE",
                        color = if (isPlaying) NeonCyan else Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.05.sp
                    )
                }

                // Volume and dB Telemetry
                val dbEquivalent = if (activeVolumeFactor > 0.001f) {
                    (20 * kotlin.math.log10(activeVolumeFactor)).toInt().coerceIn(-48, 0)
                } else -60

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VOL: ${(volume * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceSlateHigh)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "$dbEquivalent dB",
                            color = if (dbEquivalent > -6) VibrantMagenta else NeonElectricBlue,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Main Canvas Rendering Area: phase is read strictly inside the Canvas lambda!
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF030509))
                    .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(14.dp))
                    .padding(6.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("audio_visualizer_canvas")
                ) {
                    val w = size.width
                    val h = size.height
                    val currentPhase = phaseState.value
                    val currentFastPhase = fastPhaseState.value

                    when (mode) {
                        VisualizerMode.SPECTRUM -> {
                            drawSpectrumBars(
                                width = w,
                                height = h,
                                volumeFactor = activeVolumeFactor,
                                isPlaying = isPlaying,
                                phase = currentPhase,
                                fastPhase = currentFastPhase,
                                bandGains = bandGains,
                                barBrush = spectrumBrush
                            )
                        }
                        VisualizerMode.OSCILLOSCOPE -> {
                            drawOscilloscopeWave(
                                width = w,
                                height = h,
                                volumeFactor = activeVolumeFactor,
                                isPlaying = isPlaying,
                                phase = currentPhase,
                                fastPhase = currentFastPhase,
                                bandGains = bandGains,
                                wavePath = wavePath,
                                underGlowPath = underGlowPath,
                                wave2Path = wave2Path,
                                waveBrush = waveBrush
                            )
                        }
                        VisualizerMode.RADIAL -> {
                            drawRadialOrbital(
                                width = w,
                                height = h,
                                volumeFactor = activeVolumeFactor,
                                isPlaying = isPlaying,
                                phase = currentPhase,
                                fastPhase = currentFastPhase,
                                bandGains = bandGains
                            )
                        }
                    }
                }
            }

            // Optional Mode Selector Footer
            if (showModeSelector) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VisualizerMode.values().forEach { visualizerMode ->
                        val isSelected = mode == visualizerMode
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) NeonElectricBlue.copy(alpha = 0.2f) else SurfaceSlate)
                                .border(
                                    1.dp,
                                    if (isSelected) NeonElectricBlue else Color(0x1FFFFFFF),
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable { mode = visualizerMode }
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .testTag("visualizer_mode_${visualizerMode.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when (visualizerMode) {
                                        VisualizerMode.SPECTRUM -> Icons.Default.Equalizer
                                        VisualizerMode.OSCILLOSCOPE -> Icons.Default.Waves
                                        VisualizerMode.RADIAL -> Icons.Default.GraphicEq
                                    },
                                    contentDescription = null,
                                    tint = if (isSelected) NeonCyan else Color(0xFF94A3B8),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = visualizerMode.label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 32-Band Studio Spectrum Analyzer Canvas Renderer.
 * Bars scale dynamically in height, color, and peak bounce according to volume.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSpectrumBars(
    width: Float,
    height: Float,
    volumeFactor: Float,
    isPlaying: Boolean,
    phase: Float,
    fastPhase: Float,
    bandGains: List<Float>,
    barBrush: Brush
) {
    // Reference horizontal grid lines
    val gridLines = listOf(0.2f, 0.4f, 0.6f, 0.8f)
    gridLines.forEach { ratio ->
        drawLine(
            color = Color(0x14FFFFFF),
            start = Offset(0f, height * ratio),
            end = Offset(width, height * ratio),
            strokeWidth = 1f
        )
    }

    val barCount = 32
    val spacing = 3.dp.toPx()
    val totalSpacing = (barCount - 1) * spacing
    val barWidth = ((width - totalSpacing) / barCount).coerceAtLeast(2f)
    val baselineY = height * 0.90f
    val maxHeight = height * 0.82f

    val reflectionAlpha = (0.25f * volumeFactor).coerceIn(0f, 1f)
    val reflectionColor = NeonCyan.copy(alpha = reflectionAlpha)

    for (i in 0 until barCount) {
        val t = i.toFloat() / (barCount - 1)
        val x = i * (barWidth + spacing)

        // Interpolate corresponding EQ gain
        val eqGain = if (bandGains.isNotEmpty()) {
            val idx = (t * (bandGains.size - 1)).toInt().coerceIn(0, bandGains.size - 1)
            (bandGains[idx] / 12f).coerceIn(-1f, 1f)
        } else 0f

        // Real-time sinusoidal frequency excitation
        val wave1 = sin(phase + i * 0.35f) * 0.45f
        val wave2 = sin(fastPhase * 1.5f + i * 0.8f) * 0.25f
        val wave3 = cos(phase * 0.8f + i * 0.2f) * 0.20f
        val combinedWave = abs(wave1 + wave2 + wave3 + 0.15f)

        // Height strictly responds dynamically to volume
        val targetHeight = if (isPlaying) {
            val dynamicGainFactor = (1f + eqGain * 0.5f).coerceAtLeast(0.15f)
            (maxHeight * volumeFactor * combinedWave * dynamicGainFactor).coerceIn(4f, maxHeight)
        } else {
            // Idle ambient line
            4f + (volumeFactor * 8f)
        }

        val topY = baselineY - targetHeight

        // Render main spectrum bar
        drawRoundRect(
            brush = barBrush,
            topLeft = Offset(x, topY),
            size = Size(barWidth, targetHeight),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )

        // Floating dynamic peak cap (studio peak meter)
        val peakCapHeight = 2.5.dp.toPx()
        val peakCapY = (topY - 4.dp.toPx() - (sin(fastPhase + i) * 2.dp.toPx() * volumeFactor)).coerceAtLeast(2f)
        val peakColor = if (targetHeight > maxHeight * 0.75f) VibrantMagenta else NeonCyan

        drawRoundRect(
            color = peakColor.copy(alpha = if (isPlaying) 0.95f else 0.4f),
            topLeft = Offset(x, peakCapY),
            size = Size(barWidth, peakCapHeight),
            cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
        )

        // Subtle dark OLED floor reflection without re-allocating a brush
        val reflectionHeight = (targetHeight * 0.22f).coerceAtMost(height - baselineY)
        drawRoundRect(
            color = reflectionColor,
            topLeft = Offset(x, baselineY + 1.dp.toPx()),
            size = Size(barWidth, reflectionHeight),
            cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
        )
    }
}

/**
 * Fluid Neon Oscilloscope Waveform Canvas Renderer.
 * High-fidelity dual sine wave with volume-driven amplitude and glowing fill.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawOscilloscopeWave(
    width: Float,
    height: Float,
    volumeFactor: Float,
    isPlaying: Boolean,
    phase: Float,
    fastPhase: Float,
    bandGains: List<Float>,
    wavePath: Path,
    underGlowPath: Path,
    wave2Path: Path,
    waveBrush: Brush
) {
    val centerY = height * 0.5f
    val maxAmp = (height * 0.42f) * volumeFactor

    // Draw central unity axis
    drawLine(
        color = NeonElectricBlue.copy(alpha = 0.25f),
        start = Offset(0f, centerY),
        end = Offset(width, centerY),
        strokeWidth = 1.dp.toPx()
    )

    val steps = 80
    wavePath.reset()
    underGlowPath.reset()
    wave2Path.reset()

    underGlowPath.moveTo(0f, centerY)

    for (i in 0..steps) {
        val t = i.toFloat() / steps
        val x = t * width

        val eqBass = bandGains.getOrNull(0) ?: 0f
        val bassBoostMult = 1f + (eqBass / 24f)

        val yOffset = if (isPlaying && volumeFactor > 0.01f) {
            val s1 = sin(phase * 2f + t * 4f * PI.toFloat()) * maxAmp * 0.65f * bassBoostMult
            val s2 = sin(fastPhase * 3f + t * 8f * PI.toFloat()) * maxAmp * 0.25f
            val s3 = cos(phase * 1.2f + t * 2f * PI.toFloat()) * maxAmp * 0.15f
            s1 + s2 + s3
        } else {
            sin(phase + t * 2f * PI.toFloat()) * 4f // idle breathing ripple
        }

        val y = centerY + yOffset
        if (i == 0) {
            wavePath.moveTo(x, y)
        } else {
            wavePath.lineTo(x, y)
        }
        underGlowPath.lineTo(x, y)
    }

    underGlowPath.lineTo(width, centerY)
    underGlowPath.close()

    // Ambient fill under oscilloscope curve
    drawPath(
        path = underGlowPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                NeonViolet.copy(alpha = 0.35f * volumeFactor),
                NeonCyan.copy(alpha = 0.15f * volumeFactor),
                Color.Transparent
            ),
            startY = centerY - maxAmp,
            endY = centerY + maxAmp
        )
    )

    // Primary Neon Wave
    drawPath(
        path = wavePath,
        brush = waveBrush,
        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
    )

    // Secondary sub-harmonic wave (Stereo Right channel phase shift)
    if (isPlaying && volumeFactor > 0.05f) {
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val x = t * width
            val yOffset = sin(phase * 2.1f + t * 5f * PI.toFloat() + 0.6f) * maxAmp * 0.45f
            val y = centerY + yOffset
            if (i == 0) wave2Path.moveTo(x, y) else wave2Path.lineTo(x, y)
        }
        drawPath(
            path = wave2Path,
            color = VibrantMagenta.copy(alpha = 0.65f * volumeFactor),
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

/**
 * Radial 360-degree Orbital Halo Visualizer.
 * Radiates frequency rays from the center, pulsing dynamically with playback volume.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRadialOrbital(
    width: Float,
    height: Float,
    volumeFactor: Float,
    isPlaying: Boolean,
    phase: Float,
    fastPhase: Float,
    bandGains: List<Float>
) {
    val center = Offset(width * 0.5f, height * 0.5f)
    val baseRadius = (minOf(width, height) * 0.22f).coerceAtLeast(20f)
    val maxRayLength = (minOf(width, height) * 0.24f) * volumeFactor

    // Central pulsing orb
    val orbRadius = baseRadius + (sin(fastPhase) * 4.dp.toPx() * volumeFactor)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                NeonViolet.copy(alpha = 0.6f * volumeFactor),
                NeonElectricBlue.copy(alpha = 0.3f * volumeFactor),
                Color.Transparent
            ),
            center = center,
            radius = orbRadius * 1.5f
        ),
        radius = orbRadius * 1.5f,
        center = center
    )
    drawCircle(
        color = Color(0xFF0A0F1A),
        radius = orbRadius,
        center = center
    )
    drawCircle(
        color = NeonElectricBlue.copy(alpha = 0.8f),
        radius = orbRadius,
        center = center,
        style = Stroke(width = 1.5.dp.toPx())
    )

    // 48 Radial Frequency Rays
    val rayCount = 48
    for (i in 0 until rayCount) {
        val angle = (i.toFloat() / rayCount) * 2f * PI.toFloat() + (phase * 0.2f)
        val t = i.toFloat() / rayCount

        val wave = abs(sin(fastPhase + i * 0.4f) * 0.6f + cos(phase * 2f + i * 0.2f) * 0.4f)
        val rayLen = if (isPlaying) {
            (maxRayLength * wave).coerceIn(3.dp.toPx(), maxRayLength)
        } else {
            2.dp.toPx() + (volumeFactor * 4.dp.toPx())
        }

        val startX = center.x + cos(angle) * (orbRadius + 3.dp.toPx())
        val startY = center.y + sin(angle) * (orbRadius + 3.dp.toPx())

        val endX = center.x + cos(angle) * (orbRadius + 3.dp.toPx() + rayLen)
        val endY = center.y + sin(angle) * (orbRadius + 3.dp.toPx() + rayLen)

        val rayColor = if (i % 3 == 0) VibrantMagenta else if (i % 2 == 0) NeonCyan else NeonElectricBlue

        drawLine(
            color = rayColor.copy(alpha = if (isPlaying) 0.9f else 0.35f),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}
