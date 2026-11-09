package com.example.soniq.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.soniq.data.EqualizerPreset
import com.example.soniq.ui.theme.NeonCyan
import com.example.soniq.ui.theme.NeonElectricBlue
import com.example.soniq.ui.theme.NeonViolet
import com.example.soniq.ui.theme.OledPitchBlack
import com.example.soniq.ui.theme.SurfaceSlate
import com.example.soniq.ui.theme.SurfaceSlateHigh
import com.example.soniq.ui.theme.VibrantMagenta

val TEN_BAND_FREQUENCIES = listOf(
    "31Hz", "63Hz", "125Hz", "250Hz", "500Hz",
    "1kHz", "2kHz", "4kHz", "8kHz", "16kHz"
)

val FIVE_BAND_FREQUENCIES = listOf(
    "60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz"
)

@Composable
fun MultiBandEqualizer(
    isEnabled: Boolean,
    onToggleEnabled: () -> Unit,
    bandGains: List<Float>,
    onBandGainChange: (Int, Float) -> Unit,
    onResetToUnity: () -> Unit,
    currentPresetName: String,
    presets: List<EqualizerPreset>,
    onSelectPreset: (EqualizerPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    var is10BandMode by remember { mutableStateOf(true) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceSlate
        ),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(20.dp))
            .testTag("multi_band_equalizer_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Bar: Title, Engine Status & Master Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isEnabled) NeonElectricBlue.copy(alpha = 0.2f) else Color(0x14FFFFFF)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = if (isEnabled) NeonCyan else Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "Parametric Multi-Band EQ",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (isEnabled) "DSP Low-Latency Processing Active" else "Bypass Mode (Flat Native Output)",
                            color = if (isEnabled) NeonCyan else Color(0xFF64748B),
                            fontSize = 11.sp
                        )
                    }
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = { onToggleEnabled() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = NeonElectricBlue,
                        uncheckedThumbColor = Color(0xFF94A3B8),
                        uncheckedTrackColor = SurfaceSlateHigh
                    ),
                    modifier = Modifier.testTag("eq_master_toggle")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Real-time Frequency Response Curve Visualizer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(OledPitchBlack)
                    .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val zeroY = h * 0.5f

                    // 0 dB Unity line
                    drawLine(
                        color = Color(0x22FFFFFF),
                        start = Offset(0f, zeroY),
                        end = Offset(w, zeroY),
                        strokeWidth = 1.5f
                    )

                    // Reference grid lines (+6dB, -6dB)
                    drawLine(
                        color = Color(0x10FFFFFF),
                        start = Offset(0f, zeroY - (h * 0.25f)),
                        end = Offset(w, zeroY - (h * 0.25f)),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = Color(0x10FFFFFF),
                        start = Offset(0f, zeroY + (h * 0.25f)),
                        end = Offset(w, zeroY + (h * 0.25f)),
                        strokeWidth = 1f
                    )

                    if (isEnabled) {
                        // Spline curve through frequency points
                        val points = mutableListOf<Offset>()
                        val activeGains = if (is10BandMode) {
                            bandGains
                        } else {
                            listOf(
                                bandGains.getOrElse(1) { 0f },
                                bandGains.getOrElse(3) { 0f },
                                bandGains.getOrElse(5) { 0f },
                                bandGains.getOrElse(7) { 0f },
                                bandGains.getOrElse(9) { 0f }
                            )
                        }

                        val steps = activeGains.size
                        for (i in 0 until steps) {
                            val x = if (steps > 1) (i.toFloat() / (steps - 1)) * (w - 24f) + 12f else w / 2f
                            val gain = activeGains[i]
                            val y = (zeroY - (gain / 12f) * (h * 0.42f)).coerceIn(8f, h - 8f)
                            points.add(Offset(x, y))
                        }

                        // Gradient fill area
                        val fillPath = Path().apply {
                            moveTo(0f, h)
                            points.forEachIndexed { idx, pt ->
                                if (idx == 0) lineTo(0f, pt.y)
                                lineTo(pt.x, pt.y)
                            }
                            lineTo(w, points.lastOrNull()?.y ?: zeroY)
                            lineTo(w, h)
                            close()
                        }

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                listOf(
                                    NeonCyan.copy(alpha = 0.25f),
                                    NeonElectricBlue.copy(alpha = 0.12f),
                                    Color.Transparent
                                )
                            )
                        )

                        // Smooth neon curve line
                        val strokePath = Path().apply {
                            points.forEachIndexed { idx, pt ->
                                if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                            }
                        }

                        drawPath(
                            path = strokePath,
                            brush = Brush.horizontalGradient(
                                listOf(NeonCyan, NeonElectricBlue, NeonViolet, VibrantMagenta)
                            ),
                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Peak nodes
                        points.forEach { pt ->
                            drawCircle(color = OledPitchBlack, radius = 4.dp.toPx(), center = pt)
                            drawCircle(color = NeonCyan, radius = 2.5.dp.toPx(), center = pt)
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "+12 dB",
                        color = VibrantMagenta.copy(alpha = 0.7f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "UNITY 0 dB",
                        color = Color(0xFF64748B),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "-12 dB",
                        color = Color(0xFF64748B),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Presets Row & Reset Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ACOUSTIC PRESETS",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.04.sp
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onResetToUnity() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("eq_reset_unity_btn"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Reset to Flat",
                        tint = NeonCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "RESET TO FLAT",
                        color = NeonCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Presets Horizontal Carousel
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(presets) { preset ->
                    val isSelected = currentPresetName == preset.name
                    val chipBg by animateColorAsState(
                        targetValue = if (isSelected) NeonElectricBlue.copy(alpha = 0.25f) else OledPitchBlack,
                        animationSpec = tween(200),
                        label = "chip_bg"
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(chipBg)
                            .border(
                                1.dp,
                                if (isSelected) NeonElectricBlue else Color(0x22FFFFFF),
                                RoundedCornerShape(9999.dp)
                            )
                            .clickable { onSelectPreset(preset) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("eq_preset_${preset.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(NeonCyan)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = preset.name,
                                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Band Mode Selector: 10-Band vs 5-Band
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(OledPitchBlack)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (is10BandMode) SurfaceSlateHigh else Color.Transparent)
                        .clickable { is10BandMode = true }
                        .padding(vertical = 6.dp)
                        .testTag("eq_mode_10_band"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "10-Band Precision Rack",
                        color = if (is10BandMode) Color.White else Color(0xFF64748B),
                        fontWeight = if (is10BandMode) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 11.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (!is10BandMode) SurfaceSlateHigh else Color.Transparent)
                        .clickable { is10BandMode = false }
                        .padding(vertical = 6.dp)
                        .testTag("eq_mode_5_band"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "5-Band Simplified",
                        color = if (!is10BandMode) Color.White else Color(0xFF64748B),
                        fontWeight = if (!is10BandMode) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Multi-Band Sliders List
            val displayedBands = if (is10BandMode) {
                TEN_BAND_FREQUENCIES.mapIndexed { idx, freq -> Triple(idx, freq, bandGains.getOrElse(idx) { 0f }) }
            } else {
                listOf(1, 3, 5, 7, 9).mapIndexed { idx, actualIdx ->
                    Triple(actualIdx, FIVE_BAND_FREQUENCIES[idx], bandGains.getOrElse(actualIdx) { 0f })
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                displayedBands.forEach { (actualIndex, freqLabel, gain) ->
                    EqualizerBandSliderRow(
                        frequency = freqLabel,
                        gainDb = gain,
                        isEnabled = isEnabled,
                        onGainChange = { onBandGainChange(actualIndex, it) },
                        testTag = "slider_band_$actualIndex"
                    )
                }
            }
        }
    }
}

@Composable
private fun EqualizerBandSliderRow(
    frequency: String,
    gainDb: Float,
    isEnabled: Boolean,
    onGainChange: (Float) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Frequency Label
        Text(
            text = frequency,
            color = if (isEnabled) Color.White else Color(0xFF64748B),
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.width(52.dp)
        )

        // Compose Slider
        Slider(
            value = gainDb,
            onValueChange = onGainChange,
            valueRange = -12.0f..12.0f,
            enabled = isEnabled,
            colors = SliderDefaults.colors(
                thumbColor = if (isEnabled) Color.White else Color(0xFF64748B),
                activeTrackColor = when {
                    !isEnabled -> Color(0xFF33353B)
                    gainDb > 3f -> VibrantMagenta
                    gainDb > 0f -> NeonCyan
                    gainDb < -3f -> NeonViolet
                    else -> NeonElectricBlue
                },
                inactiveTrackColor = Color(0xFF141824)
            ),
            modifier = Modifier
                .weight(1f)
                .testTag(testTag)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Numerical dB Readout Badge with Tabular formatting
        val sign = if (gainDb > 0.05f) "+" else ""
        val formattedDb = "$sign${String.format("%.1f", gainDb)} dB"

        Box(
            modifier = Modifier
                .width(62.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    when {
                        !isEnabled -> Color(0x0FFFFFFF)
                        gainDb > 0.1f -> NeonCyan.copy(alpha = 0.18f)
                        gainDb < -0.1f -> NeonViolet.copy(alpha = 0.18f)
                        else -> Color(0x14FFFFFF)
                    }
                )
                .padding(horizontal = 4.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = formattedDb,
                color = when {
                    !isEnabled -> Color(0xFF64748B)
                    gainDb > 0.1f -> NeonCyan
                    gainDb < -0.1f -> NeonViolet
                    else -> Color(0xFF94A3B8)
                },
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }
    }
}
