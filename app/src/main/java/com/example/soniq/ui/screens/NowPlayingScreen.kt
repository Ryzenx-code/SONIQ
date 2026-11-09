package com.example.soniq.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.soniq.audio.AudioEngineState
import com.example.soniq.data.RepeatMode
import com.example.soniq.ui.theme.NeonCyan
import com.example.soniq.ui.theme.NeonElectricBlue
import com.example.soniq.ui.theme.NeonViolet
import com.example.soniq.ui.theme.SonicFlowGradient
import com.example.soniq.ui.theme.SurfaceSlate
import com.example.soniq.ui.theme.SurfaceSlateHigh
import com.example.soniq.ui.theme.VibrantMagenta

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import com.example.soniq.data.EqualizerPreset
import com.example.soniq.data.SampleMediaRepository
import com.example.soniq.ui.components.MultiBandEqualizer
import com.example.soniq.ui.components.PlayerGestureContainer
import com.example.soniq.ui.components.RealtimeAudioVisualizer
import com.example.soniq.ui.components.VisualizerMode
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.TouchApp
import com.example.soniq.ui.theme.OledPitchBlack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    state: AudioEngineState,
    onCollapse: () -> Unit,
    onTogglePlay: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onSetSpeed: (Float) -> Unit,
    onSetSleepTimer: (Int?) -> Unit,
    onOpenStudio: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenLyrics: () -> Unit,
    onSetBandGain: (Int, Float) -> Unit = { _, _ -> },
    onToggleDsp: () -> Unit = {},
    onResetUnity: () -> Unit = {},
    onSelectPreset: (EqualizerPreset) -> Unit = {},
    onSetVolume: (Float) -> Unit = {},
    modifier: Modifier = Modifier,
    positionMsProvider: () -> Long = { 0L }
) {
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var showEqSheet by remember { mutableStateOf(false) }
    var showStudioVisualizer by remember { mutableStateOf(false) }

    val track = state.currentTrack
    val isFavorite = state.queue.find { it.id == track.id }?.isFavorite ?: track.isFavorite

    PlayerGestureContainer(
        currentVolume = state.volume,
        onSetVolume = onSetVolume,
        onNext = onNext,
        onPrevious = onPrevious,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(OledPitchBlack)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .testTag("now_playing_screen"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
        // 1. Top Context Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onCollapse,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0x1AFFFFFF))
                    .testTag("collapse_now_playing_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Collapse",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "PLAYING FROM ALBUM",
                    color = NeonCyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.05.sp
                )
                Text(
                    text = track.album,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = {},
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0x1AFFFFFF))
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Gesture Guidance Bar and Visualizer Mode Switch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(9999.dp))
                    .background(Color(0x14FFFFFF))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Swipe ↔ Track • ↕ Volume",
                    color = Color(0xFF94A3B8),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(9999.dp))
                    .background(if (showStudioVisualizer) NeonElectricBlue.copy(alpha = 0.25f) else SurfaceSlate)
                    .border(
                        1.dp,
                        if (showStudioVisualizer) NeonElectricBlue else Color(0x22FFFFFF),
                        RoundedCornerShape(9999.dp)
                    )
                    .clickable { showStudioVisualizer = !showStudioVisualizer }
                    .padding(horizontal = 8.dp, vertical = 3.dp)
                    .testTag("toggle_visualizer_mode_btn"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (showStudioVisualizer) Icons.Default.GraphicEq else Icons.Default.Equalizer,
                        contentDescription = null,
                        tint = if (showStudioVisualizer) NeonCyan else Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (showStudioVisualizer) "VINYL ART" else "CANVAS VISUALIZER",
                        color = if (showStudioVisualizer) NeonCyan else Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 2. Dynamic Hero: Real-time Audio Visualizer OR Vinyl Album Art
        if (showStudioVisualizer) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .height(240.dp)
                    .testTag("hero_studio_visualizer_box"),
                contentAlignment = Alignment.Center
            ) {
                RealtimeAudioVisualizer(
                    volume = state.volume,
                    isPlaying = state.isPlaying,
                    bandGains = state.bandGains,
                    modifier = Modifier.fillMaxSize(),
                    initialMode = VisualizerMode.SPECTRUM,
                    showModeSelector = true
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        brush = Brush.radialGradient(
                            listOf(
                                NeonViolet.copy(alpha = 0.7f),
                                Color(0xFF131926),
                                Color(0xFF07090E)
                            )
                        )
                    )
                    .border(1.5.dp, NeonElectricBlue.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                    .clickable { showStudioVisualizer = true }
                    .testTag("hero_vinyl_box"),
                contentAlignment = Alignment.Center
            ) {
                // Real-time canvas radial aura underneath vinyl responding dynamically to volume
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width * 0.5f, size.height * 0.5f)
                    val volFactor = if (state.isPlaying) state.volume else 0.05f
                    val pulseRadius = (size.width * 0.36f) + (volFactor * 22.dp.toPx())
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                NeonCyan.copy(alpha = 0.40f * volFactor),
                                NeonElectricBlue.copy(alpha = 0.25f * volFactor),
                                Color.Transparent
                            ),
                            center = center,
                            radius = pulseRadius * 1.3f
                        ),
                        radius = pulseRadius * 1.3f,
                        center = center
                    )
                }

                // Inner vinyl grooves & visualizer aura
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.68f)
                        .clip(CircleShape)
                        .background(Color(0xFF0B0F17))
                        .border(2.dp, Color(0x22FFFFFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(0.55f)
                            .clip(CircleShape)
                            .background(brush = SonicFlowGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Floating prompt on vinyl
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(9999.dp))
                        .background(OledPitchBlack.copy(alpha = 0.82f))
                        .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(9999.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "TAP FOR CANVAS VISUALIZER",
                        color = NeonCyan,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.04.sp
                    )
                }
            }
        }

        // 3. Track Metadata & Favorite Toggle
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        fontSize = 14.sp,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { onToggleFavorite(track.id) },
                    modifier = Modifier.testTag("favorite_btn")
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) VibrantMagenta else Color(0xFF94A3B8),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Codec & Audio Telemetry Badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9999.dp))
                        .background(NeonElectricBlue.copy(alpha = 0.15f))
                        .border(1.dp, NeonElectricBlue.copy(alpha = 0.4f), RoundedCornerShape(9999.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = track.codec.label,
                        color = NeonElectricBlue,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9999.dp))
                        .background(SurfaceSlateHigh)
                        .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(9999.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = track.codec.sampleRate,
                        color = Color(0xFF94A3B8),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9999.dp))
                        .background(VibrantMagenta.copy(alpha = 0.15f))
                        .border(1.dp, VibrantMagenta.copy(alpha = 0.4f), RoundedCornerShape(9999.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "DIRECT DAC",
                        color = VibrantMagenta,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 4. Interactive Scrubber & Waveform Seeker (Isolated recomposition)
        NowPlayingProgressBar(
            durationMs = state.durationMs,
            positionMsProvider = positionMsProvider,
            onSeekTo = onSeekTo
        )

        // 5. Main Transport Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle
            IconButton(
                onClick = onToggleShuffle,
                modifier = Modifier.testTag("shuffle_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (state.isShuffle) NeonCyan else Color(0xFF94A3B8),
                    modifier = Modifier.size(22.dp)
                )
            }

            // Previous
            IconButton(
                onClick = onPrevious,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0x14FFFFFF))
                    .testTag("prev_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Hero 64dp Play/Pause
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(brush = SonicFlowGradient)
                    .border(1.5.dp, NeonCyan.copy(alpha = 0.5f), CircleShape)
                    .clickable { onTogglePlay() }
                    .testTag("hero_play_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }

            // Next
            IconButton(
                onClick = onNext,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0x14FFFFFF))
                    .testTag("next_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Repeat
            IconButton(
                onClick = onCycleRepeat,
                modifier = Modifier.testTag("repeat_btn")
            ) {
                Icon(
                    imageVector = if (state.repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                    contentDescription = "Repeat",
                    tint = if (state.repeatMode != RepeatMode.OFF) NeonViolet else Color(0xFF94A3B8),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // 6. Secondary Quick Controls Bar: EQ, Lyrics, Queue, Sleep, Speed
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceSlate)
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { showEqSheet = true },
                modifier = Modifier.testTag("quick_eq_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Audio Studio EQ",
                    tint = NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onOpenLyrics,
                modifier = Modifier.testTag("quick_lyrics_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Lyrics,
                    contentDescription = "Lyrics",
                    tint = if (track.lyrics.isNotEmpty()) NeonViolet else Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onOpenQueue,
                modifier = Modifier.testTag("quick_queue_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.QueueMusic,
                    contentDescription = "Queue",
                    tint = NeonElectricBlue,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = { showSleepDialog = true },
                modifier = Modifier.testTag("quick_sleep_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Bedtime,
                    contentDescription = "Sleep Timer",
                    tint = if (state.sleepTimerMinutesLeft != null) VibrantMagenta else Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = { showSpeedDialog = true },
                modifier = Modifier.testTag("quick_speed_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = "Playback Speed",
                    tint = if (state.playbackSpeed != 1.0f) NeonCyan else Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
    }

    // Playback Speed Dialog
    if (showSpeedDialog) {
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("Playback Speed", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (state.playbackSpeed == speed) NeonElectricBlue.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable {
                                    onSetSpeed(speed)
                                    showSpeedDialog = false
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "${speed}x", color = Color.White, fontWeight = FontWeight.Bold)
                            if (state.playbackSpeed == speed) {
                                Text(text = "Active", color = NeonCyan, fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) {
                    Text("Close", color = NeonElectricBlue)
                }
            },
            containerColor = SurfaceSlate
        )
    }

    // Sleep Timer Dialog
    if (showSleepDialog) {
        AlertDialog(
            onDismissRequest = { showSleepDialog = false },
            title = { Text("Sleep Timer Night Mode", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        Pair(null, "Off"),
                        Pair(15, "15 minutes"),
                        Pair(30, "30 minutes"),
                        Pair(45, "45 minutes"),
                        Pair(60, "1 hour")
                    ).forEach { (mins, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (state.sleepTimerMinutesLeft == mins) VibrantMagenta.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable {
                                    onSetSleepTimer(mins)
                                    showSleepDialog = false
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = label, color = Color.White, fontWeight = FontWeight.Bold)
                            if (state.sleepTimerMinutesLeft == mins) {
                                Text(text = "Active", color = VibrantMagenta, fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepDialog = false }) {
                    Text("Cancel", color = NeonElectricBlue)
                }
            },
            containerColor = SurfaceSlate
        )
    }

    // In-Player Multi-Band Equalizer Modal Sheet
    if (showEqSheet) {
        ModalBottomSheet(
            onDismissRequest = { showEqSheet = false },
            containerColor = OledPitchBlack,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .width(42.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                MultiBandEqualizer(
                    isEnabled = state.isDspEnabled,
                    onToggleEnabled = onToggleDsp,
                    bandGains = state.bandGains,
                    onBandGainChange = onSetBandGain,
                    onResetToUnity = onResetUnity,
                    currentPresetName = state.currentPreset.name,
                    presets = SampleMediaRepository.defaultPresets,
                    onSelectPreset = onSelectPreset
                )
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
fun NowPlayingProgressBar(
    durationMs: Long,
    positionMsProvider: () -> Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val curPos = positionMsProvider()
    val progress = if (durationMs > 0) (curPos.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val curMins = curPos / 60000
    val curSecs = (curPos % 60000) / 1000
    val durMins = durationMs / 60000
    val durSecs = (durationMs % 60000) / 1000

    Column(modifier = modifier.fillMaxWidth()) {
        Slider(
            value = progress,
            onValueChange = { newProgress ->
                val newPos = (newProgress * durationMs).toLong()
                onSeekTo(newPos)
            },
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = NeonElectricBlue,
                inactiveTrackColor = SurfaceSlateHigh
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("now_playing_seeker")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = String.format("%d:%02d", curMins, curSecs),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = String.format("%d:%02d", durMins, durSecs),
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
