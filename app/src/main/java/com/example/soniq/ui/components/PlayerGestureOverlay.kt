package com.example.soniq.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.soniq.ui.theme.NeonCyan
import com.example.soniq.ui.theme.NeonElectricBlue
import com.example.soniq.ui.theme.NeonViolet
import com.example.soniq.ui.theme.OledPitchBlack
import com.example.soniq.ui.theme.VibrantMagenta
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Touch gesture listener container for media playback control.
 * Features:
 * - Swipe Left: Skip to Next Track
 * - Swipe Right: Skip to Previous Track
 * - Vertical Drag: Real-time dynamic volume adjustment (drag up = increase, drag down = decrease)
 * - Sleek OLED Heads-Up Display (HUD) giving visual feedback during interactions
 */
@Composable
fun PlayerGestureContainer(
    currentVolume: Float,
    onSetVolume: (Float) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier,
    enableGestures: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var showVolumeHud by remember { mutableStateOf(false) }
    var skipFeedbackText by remember { mutableStateOf<String?>(null) }
    var isNextTrack by remember { mutableStateOf(true) }

    var hudDismissJob by remember { mutableStateOf<Job?>(null) }

    var accumulatedDragX by remember { mutableFloatStateOf(0f) }
    var accumulatedDragY by remember { mutableFloatStateOf(0f) }
    var detectedGestureAxis by remember { mutableStateOf<GestureAxis?>(null) }

    fun scheduleHudDismiss() {
        hudDismissJob?.cancel()
        hudDismissJob = scope.launch {
            delay(1300L)
            showVolumeHud = false
            skipFeedbackText = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (enableGestures) {
                    Modifier.pointerInput(currentVolume) {
                        detectDragGestures(
                            onDragStart = {
                                accumulatedDragX = 0f
                                accumulatedDragY = 0f
                                detectedGestureAxis = null
                            },
                            onDragEnd = {
                                if (detectedGestureAxis == GestureAxis.HORIZONTAL) {
                                    val swipeThreshold = 80f
                                    if (accumulatedDragX < -swipeThreshold) {
                                        // Swipe Left -> Next Track
                                        onNext()
                                        skipFeedbackText = "NEXT TRACK"
                                        isNextTrack = true
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    } else if (accumulatedDragX > swipeThreshold) {
                                        // Swipe Right -> Previous Track
                                        onPrevious()
                                        skipFeedbackText = "PREVIOUS TRACK"
                                        isNextTrack = false
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                }
                                scheduleHudDismiss()
                                detectedGestureAxis = null
                            },
                            onDragCancel = {
                                scheduleHudDismiss()
                                detectedGestureAxis = null
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                accumulatedDragX += dragAmount.x
                                accumulatedDragY += dragAmount.y

                                if (detectedGestureAxis == null) {
                                    if (abs(accumulatedDragY) > 18f && abs(accumulatedDragY) > abs(accumulatedDragX)) {
                                        detectedGestureAxis = GestureAxis.VERTICAL
                                    } else if (abs(accumulatedDragX) > 18f && abs(accumulatedDragX) > abs(accumulatedDragY)) {
                                        detectedGestureAxis = GestureAxis.HORIZONTAL
                                    }
                                }

                                if (detectedGestureAxis == GestureAxis.VERTICAL) {
                                    // Vertical drag: invert Y delta (dragging UP increases volume)
                                    val delta = -dragAmount.y / 320f
                                    val newVol = (currentVolume + delta).coerceIn(0f, 1f)
                                    onSetVolume(newVol)
                                    showVolumeHud = true
                                    skipFeedbackText = null
                                    scheduleHudDismiss()
                                }
                            }
                        )
                    }
                } else Modifier
            )
            .testTag("player_gesture_container")
    ) {
        // Main embedded screen content
        content()

        // 1. Sleek OLED Volume Gesture HUD
        AnimatedVisibility(
            visible = showVolumeHud,
            enter = fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.85f),
            exit = fadeOut(tween(250)) + scaleOut(tween(250), targetScale = 0.85f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(OledPitchBlack.copy(alpha = 0.94f))
                    .border(1.5.dp, NeonElectricBlue.copy(alpha = 0.8f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .testTag("volume_gesture_hud"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val volumeIcon = when {
                        currentVolume <= 0.001f -> Icons.Default.VolumeOff
                        currentVolume < 0.25f -> Icons.Default.VolumeMute
                        currentVolume < 0.70f -> Icons.Default.VolumeDown
                        else -> Icons.Default.VolumeUp
                    }

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(NeonElectricBlue.copy(alpha = 0.35f), Color.Transparent)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = volumeIcon,
                            contentDescription = "Volume Level",
                            tint = NeonCyan,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "VOLUME ${(currentVolume * 100).toInt()}%",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 0.05.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Custom Gradient Level Bar
                    Box(
                        modifier = Modifier
                            .width(130.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(9999.dp))
                            .background(Color(0xFF1E293B))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(currentVolume)
                                .height(6.dp)
                                .clip(RoundedCornerShape(9999.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(NeonElectricBlue, NeonCyan, VibrantMagenta)
                                    )
                                )
                        )
                    }
                }
            }
        }

        // 2. Track Skip Gesture HUD (Swipe Left / Swipe Right)
        AnimatedVisibility(
            visible = skipFeedbackText != null,
            enter = fadeIn(tween(150)) + scaleIn(tween(150), initialScale = 0.88f),
            exit = fadeOut(tween(250)) + scaleOut(tween(250), targetScale = 0.88f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .background(OledPitchBlack.copy(alpha = 0.94f))
                    .border(1.5.dp, if (isNextTrack) NeonCyan else NeonViolet, RoundedCornerShape(22.dp))
                    .padding(horizontal = 22.dp, vertical = 16.dp)
                    .testTag("track_skip_gesture_hud"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (isNextTrack) Icons.Default.SkipNext else Icons.Default.SkipPrevious,
                        contentDescription = null,
                        tint = if (isNextTrack) NeonCyan else NeonViolet,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = skipFeedbackText ?: "",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 0.04.sp
                    )
                }
            }
        }
    }
}

private enum class GestureAxis {
    HORIZONTAL,
    VERTICAL
}
