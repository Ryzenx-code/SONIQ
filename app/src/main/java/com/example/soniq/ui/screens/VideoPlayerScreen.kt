package com.example.soniq.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.soniq.data.MediaItem
import com.example.soniq.ui.theme.NeonCyan
import com.example.soniq.ui.theme.NeonElectricBlue
import com.example.soniq.ui.theme.NeonViolet
import com.example.soniq.ui.theme.SonicFlowGradient
import com.example.soniq.ui.theme.SurfaceSlate
import com.example.soniq.ui.theme.VibrantMagenta

@Composable
fun VideoPlayerScreen(
    videoItem: MediaItem,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableStateOf(42000L) }
    var selectedAudioTrack by remember { mutableStateOf("Dolby Atmos 7.1.4") }
    var selectedSubtitle by remember { mutableStateOf("English [CC]") }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }

    val durationMs = videoItem.durationMs
    val progress = (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("video_player_screen")
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("video_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = videoItem.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = videoItem.videoResolution.ifEmpty { "4K UHD 60fps HDR" },
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Audio & Subtitle Selectors
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { showAudioDialog = true },
                    modifier = Modifier.testTag("video_audio_track_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Audiotrack,
                        contentDescription = "Audio Tracks",
                        tint = NeonElectricBlue
                    )
                }

                IconButton(
                    onClick = { showSubtitleDialog = true },
                    modifier = Modifier.testTag("video_subtitles_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ClosedCaption,
                        contentDescription = "Subtitles",
                        tint = NeonViolet
                    )
                }
            }
        }

        // Main Video Viewport Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF0F1523), Color(0xFF07090E), Color(0xFF05070C))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Simulated 4K Cinematic Render Simulation
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(brush = SonicFlowGradient)
                        .clickable { isPlaying = !isPlaying }
                        .testTag("video_viewport_play_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(52.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9999.dp))
                        .background(SurfaceSlate)
                        .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(9999.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "HARDWARE ACCELERATED • AV1 / HEVC MAIN10",
                        color = NeonCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.05.sp
                    )
                }
            }

            // Subtitle text render overlay
            if (selectedSubtitle != "Off") {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xD9000000))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "♪ [Binaural soundscape resonant harmonics] ♪",
                        color = Color.Yellow,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Bottom Controls Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF07090E))
                .padding(16.dp)
        ) {
            // Scrubber
            Slider(
                value = progress,
                onValueChange = { currentPositionMs = (it * durationMs).toLong() },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = NeonElectricBlue,
                    inactiveTrackColor = SurfaceSlate
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("video_seeker")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val curMins = currentPositionMs / 60000
                val curSecs = (currentPositionMs % 60000) / 1000
                val durMins = durationMs / 60000
                val durSecs = (durationMs % 60000) / 1000

                Text(
                    text = "${String.format("%d:%02d", curMins, curSecs)} / ${String.format("%d:%02d", durMins, durSecs)}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = selectedAudioTrack,
                        color = NeonCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Fullscreen",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }

    // Audio Track Selector Dialog
    if (showAudioDialog) {
        AlertDialog(
            onDismissRequest = { showAudioDialog = false },
            title = { Text("Select Audio Stream", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Dolby Atmos 7.1.4", "Ambisonic B-Format 360", "Stereo PCM 24-bit 96kHz").forEach { trackName ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedAudioTrack == trackName) NeonElectricBlue.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable {
                                    selectedAudioTrack = trackName
                                    showAudioDialog = false
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = trackName, color = Color.White, fontWeight = FontWeight.Bold)
                            if (selectedAudioTrack == trackName) {
                                Text(text = "Active", color = NeonCyan, fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAudioDialog = false }) {
                    Text("Close", color = NeonElectricBlue)
                }
            },
            containerColor = SurfaceSlate
        )
    }

    // Subtitle Selector Dialog
    if (showSubtitleDialog) {
        AlertDialog(
            onDismissRequest = { showSubtitleDialog = false },
            title = { Text("Select Subtitle", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Off", "English [CC]", "Japanese [SDH]", "Spanish").forEach { sub ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedSubtitle == sub) NeonViolet.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable {
                                    selectedSubtitle = sub
                                    showSubtitleDialog = false
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = sub, color = Color.White, fontWeight = FontWeight.Bold)
                            if (selectedSubtitle == sub) {
                                Text(text = "Active", color = NeonViolet, fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSubtitleDialog = false }) {
                    Text("Close", color = NeonElectricBlue)
                }
            },
            containerColor = SurfaceSlate
        )
    }
}
