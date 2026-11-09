package com.example.soniq.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.soniq.ui.theme.NeonElectricBlue
import com.example.soniq.ui.theme.SurfaceSlateHigh

@Composable
fun RotaryKnob(
    value: Int, // 0 to 100
    onValueChange: (Int) -> Unit,
    title: String,
    subtitle: String,
    icon: ImageVector,
    activeColor: Color = NeonElectricBlue,
    modifier: Modifier = Modifier
) {
    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { dragAccumulator = 0f },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragAccumulator -= dragAmount.y
                            if (kotlin.math.abs(dragAccumulator) >= 4f) {
                                val delta = (dragAccumulator / 4f).toInt()
                                val nextVal = (value + delta).coerceIn(0, 100)
                                onValueChange(nextVal)
                                dragAccumulator = 0f
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                val strokeW = 4.dp.toPx()
                val radius = (size.minDimension - strokeW) / 2
                val center = Offset(size.width / 2, size.height / 2)

                // Background track arc (240 degrees from 150 to 390)
                drawArc(
                    color = Color(0x22FFFFFF),
                    startAngle = 150f,
                    sweepAngle = 240f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )

                // Active arc
                val activeSweep = (value / 100f) * 240f
                drawArc(
                    color = activeColor,
                    startAngle = 150f,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeW + 1.dp.toPx(), cap = StrokeCap.Round)
                )

                // Center disc
                drawCircle(
                    color = SurfaceSlateHigh,
                    radius = radius * 0.72f,
                    center = center
                )
            }

            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = activeColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Text(
            text = "$value%",
            color = activeColor,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp
        )
        Text(
            text = subtitle,
            color = Color(0xFF94A3B8),
            fontSize = 9.sp
        )
    }
}
