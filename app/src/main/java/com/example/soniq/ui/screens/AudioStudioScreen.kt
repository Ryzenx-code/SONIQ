package com.example.soniq.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.soniq.audio.AudioEngineState
import com.example.soniq.data.EqualizerPreset
import com.example.soniq.data.SampleMediaRepository
import com.example.soniq.ui.components.MultiBandEqualizer
import com.example.soniq.ui.components.RealtimeAudioVisualizer
import com.example.soniq.ui.components.RotaryKnob
import com.example.soniq.ui.components.VisualizerMode
import com.example.soniq.ui.theme.NeonCyan
import com.example.soniq.ui.theme.NeonElectricBlue
import com.example.soniq.ui.theme.NeonViolet
import com.example.soniq.ui.theme.OledPitchBlack
import com.example.soniq.ui.theme.SurfaceSlate
import com.example.soniq.ui.theme.SurfaceSlateHigh
import com.example.soniq.ui.theme.VibrantMagenta

@Composable
fun AudioStudioScreen(
    state: AudioEngineState,
    onToggleDsp: () -> Unit,
    onSelectPreset: (EqualizerPreset) -> Unit,
    onSetBandGain: (Int, Float) -> Unit,
    onResetUnity: () -> Unit,
    onSetBassBoost: (Int) -> Unit,
    onSetVirtualizer: (Int) -> Unit,
    onSetCrystalizer: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(OledPitchBlack)
            .testTag("audio_studio_scroll"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. OLED Power & DAC Hardware Status Banner
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceSlate),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (state.isDspEnabled) NeonCyan else Color(0xFF64748B))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "32-BIT / 384KHZ DIRECT MASTER",
                                color = NeonCyan,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.05.sp
                            )
                            Text(
                                text = "SONIQ DSP Engine v4.2 Pro",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(Color(0x2200E5FF))
                            .border(1.dp, NeonCyan.copy(alpha = 0.35f), RoundedCornerShape(9999.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "OLED ECO ACTIVE",
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp,
                            letterSpacing = 0.04.sp
                        )
                    }
                }
            }
        }

        // 2. Real-Time Audio Canvas Visualizer responding dynamically to volume & EQ
        item {
            RealtimeAudioVisualizer(
                volume = state.volume,
                isPlaying = state.isPlaying,
                bandGains = state.bandGains,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                initialMode = VisualizerMode.SPECTRUM,
                showModeSelector = true
            )
        }

        // 3. Multi-Band Equalizer Component (Parametric Multi-Band EQ with Compose Sliders)
        item {
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
        }

        // 3. Tactile Acoustic Rotary Dials: Dynamic Bass Boost & 3D Surround
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceSlate),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "TACTILE ACOUSTIC ENGINE",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.04.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        RotaryKnob(
                            value = state.bassBoost,
                            onValueChange = onSetBassBoost,
                            title = "Bass Boost",
                            subtitle = "Dynamic Sub-Harm",
                            icon = Icons.Default.VolumeUp,
                            activeColor = NeonCyan,
                            modifier = Modifier.testTag("knob_bass_boost")
                        )

                        RotaryKnob(
                            value = state.virtualizer,
                            onValueChange = onSetVirtualizer,
                            title = "3D Surround",
                            subtitle = "Binaural Stage",
                            icon = Icons.Default.SurroundSound,
                            activeColor = NeonViolet,
                            modifier = Modifier.testTag("knob_surround")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Crystalizer Harmonics Restoration
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Crystalizer Harmonics",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Air & High-Frequency Brilliance",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp
                                )
                            }
                            Text(
                                text = "${state.crystalizer}%",
                                color = VibrantMagenta,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Slider(
                            value = state.crystalizer.toFloat(),
                            onValueChange = { onSetCrystalizer(it.toInt()) },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = VibrantMagenta,
                                inactiveTrackColor = SurfaceSlateHigh
                            ),
                            modifier = Modifier.testTag("crystalizer_slider")
                        )
                    }
                }
            }
        }

        // 4. Headphone & Direct DAC Pipeline Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceSlate),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = null,
                            tint = NeonElectricBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Headphone & DAC Pipeline",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Bit-Perfect • 384kHz • Zero Jitter",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(NeonElectricBlue.copy(alpha = 0.2f))
                            .border(1.dp, NeonElectricBlue.copy(alpha = 0.4f), RoundedCornerShape(9999.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "BIT-PERFECT",
                            color = NeonElectricBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }

        // Bottom space for mini player
        item {
            Spacer(modifier = Modifier.height(84.dp))
        }
    }
}
