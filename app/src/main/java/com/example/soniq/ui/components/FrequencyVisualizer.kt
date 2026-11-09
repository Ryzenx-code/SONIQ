package com.example.soniq.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.soniq.ui.theme.NeonCyan
import com.example.soniq.ui.theme.NeonElectricBlue
import com.example.soniq.ui.theme.NeonViolet
import com.example.soniq.ui.theme.VibrantMagenta
import kotlin.math.sin

@Composable
fun FrequencyVisualizer(
    isPlaying: Boolean,
    bandGains: List<Float>,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spectrum_wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF05070C))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
            .padding(6.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Reference dB Grid Lines
            val dbLevels = listOf(
                Pair(0.15f, Color(0x24EC4899)), // +12 dB
                Pair(0.32f, Color(0x14FFFFFF)), // +6 dB
                Pair(0.50f, NeonElectricBlue.copy(alpha = 0.35f)), // 0 dB Unity
                Pair(0.68f, Color(0x14FFFFFF)), // -6 dB
                Pair(0.85f, Color(0x1FFFFFFF))  // -12 dB Floor
            )

            dbLevels.forEach { (ratio, color) ->
                val y = h * ratio
                drawLine(
                    color = color,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = if (ratio == 0.50f) 1.5f else 1f
                )
            }

            // Real-time curve calculation
            val points = mutableListOf<Offset>()
            val steps = 60
            for (i in 0..steps) {
                val t = i.toFloat() / steps
                val x = t * w

                // Map x to corresponding band gain
                val bandIndex = (t * (bandGains.size - 1)).toInt().coerceIn(0, bandGains.size - 1)
                val gain = bandGains.getOrElse(bandIndex) { 0f }

                val animatedAmp = if (isPlaying) {
                    sin(phase + t * 4f) * 12f + sin(phase * 1.8f + t * 8f) * 6f
                } else {
                    0f
                }

                // 0dB is at y = h * 0.5
                val gainOffset = -(gain / 12f) * (h * 0.35f)
                val y = (h * 0.5f + gainOffset + animatedAmp).coerceIn(10f, h - 8f)
                points.add(Offset(x, y))
            }

            // Area path under curve
            val areaPath = Path().apply {
                moveTo(0f, h)
                points.forEachIndexed { index, pt ->
                    if (index == 0) lineTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                }
                lineTo(w, h)
                close()
            }

            val areaGradient = Brush.verticalGradient(
                colors = listOf(
                    NeonCyan.copy(alpha = 0.35f),
                    NeonViolet.copy(alpha = 0.20f),
                    VibrantMagenta.copy(alpha = 0.08f),
                    Color.Transparent
                )
            )
            drawPath(path = areaPath, brush = areaGradient)

            // Neon spline line
            val linePath = Path().apply {
                points.forEachIndexed { index, pt ->
                    if (index == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                }
            }

            val strokeGradient = Brush.horizontalGradient(
                colors = listOf(
                    NeonCyan,
                    NeonElectricBlue,
                    NeonViolet,
                    VibrantMagenta
                )
            )
            drawPath(
                path = linePath,
                brush = strokeGradient,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Dynamic peak nodes
            if (points.size >= 45) {
                val node1 = points[12]
                val node2 = points[30]
                val node3 = points[48]

                drawCircle(color = NeonCyan, radius = 5.dp.toPx(), center = node1)
                drawCircle(color = NeonViolet, radius = 5.dp.toPx(), center = node2)
                drawCircle(color = VibrantMagenta, radius = 5.dp.toPx(), center = node3)
            }
        }

        // dB labels on left
        Text(
            text = "+12 dB",
            color = VibrantMagenta.copy(alpha = 0.85f),
            fontSize = 9.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 6.dp, top = 8.dp)
        )
        Text(
            text = "0 dB UNITY",
            color = NeonElectricBlue,
            fontSize = 9.sp,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 6.dp)
        )
        Text(
            text = "-12 dB",
            color = Color(0xFF64748B),
            fontSize = 9.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 6.dp, bottom = 8.dp)
        )
    }
}
