package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AudioWaveActive
import com.example.ui.theme.AudioWaveInactive
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.VioletAccent

@Composable
fun WaveformVisualizer(
    amplitudes: List<Float>,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    val pulseAnim = remember { Animatable(0.2f) }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            pulseAnim.animateTo(
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            pulseAnim.snapTo(0.1f)
        }
    }

    val activeGradient = Brush.verticalGradient(
        colors = listOf(CyanAccent, VioletAccent)
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        val barCount = 36
        val barSpacing = 4.dp.toPx()
        val totalSpacing = barSpacing * (barCount - 1)
        val barWidth = ((size.width - totalSpacing) / barCount).coerceAtLeast(3.dp.toPx())
        val centerY = size.height / 2f

        for (i in 0 until barCount) {
            val ampIndex = amplitudes.size - 1 - (barCount - 1 - i)
            val baseAmp = if (ampIndex in amplitudes.indices) amplitudes[ampIndex] else 0.05f
            val boostedAmp = if (isRecording) {
                (baseAmp * 1.8f * pulseAnim.value).coerceIn(0.1f, 1.0f)
            } else {
                0.06f
            }

            val barHeight = (boostedAmp * size.height).coerceAtLeast(4.dp.toPx())
            val x = i * (barWidth + barSpacing)
            val y = centerY - (barHeight / 2f)

            if (isRecording) {
                drawRoundRect(
                    brush = activeGradient,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            } else {
                drawRoundRect(
                    color = AudioWaveInactive,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }
        }
    }
}
