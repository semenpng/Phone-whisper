package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.monitor.SystemTelemetry
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreenBright
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletAccent
import com.example.ui.theme.WarningAmber

@Composable
fun ResourceGaugesOverview(
    telemetry: SystemTelemetry,
    provider: String,
    threads: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Row: RAM Usage and RTF Factor
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // RAM Card
            MetricCard(
                title = "PROCESS RAM",
                value = "${String.format("%.0f", telemetry.jvmHeapUsedMb + telemetry.nativeHeapAllocatedMb)} MB",
                subtitle = "Heap ${String.format("%.0f", telemetry.jvmHeapUsedMb)}M • Native ${String.format("%.0f", telemetry.nativeHeapAllocatedMb)}M",
                progress = if (telemetry.jvmHeapMaxMb > 0) ((telemetry.jvmHeapUsedMb / telemetry.jvmHeapMaxMb).coerceIn(0f, 1f)) else 0.2f,
                progressColor = if (telemetry.lowMemoryState) ErrorRed else CyanAccent,
                modifier = Modifier.weight(1f)
            )

            // RTF Factor Card
            val rtf = telemetry.lastRtf
            val rtfColor = when {
                rtf <= 0.35f -> SuccessGreenBright
                rtf <= 0.85f -> CyanAccent
                rtf <= 1.0f -> WarningAmber
                else -> ErrorRed
            }
            val speedupStr = if (rtf > 0.001f) "${String.format("%.1f", 1f / rtf)}x" else "Real-time"
            MetricCard(
                title = "REAL-TIME FACTOR",
                value = if (rtf > 0) String.format("%.3f", rtf) else "0.240",
                subtitle = "Speed: $speedupStr • ${telemetry.lastLatencyMs}ms lat",
                progress = if (rtf > 0) (1f - rtf.coerceIn(0f, 1f)) else 0.75f,
                progressColor = rtfColor,
                modifier = Modifier.weight(1f)
            )
        }

        // Bottom Row: Audio Signal & Execution Hardware
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Audio Buffer Peak dBFS
            val dbfs = telemetry.audioBufferPeakDbfs
            val normalizedDbfs = ((dbfs + 60f) / 60f).coerceIn(0f, 1f)
            val signalColor = when {
                dbfs > -6f -> ErrorRed
                dbfs > -18f -> SuccessGreenBright
                else -> VioletAccent
            }
            MetricCard(
                title = "AUDIO INPUT PEAK",
                value = "${String.format("%.1f", dbfs)} dBFS",
                subtitle = "Frames: ${telemetry.audioFramesProcessed}",
                progress = normalizedDbfs,
                progressColor = signalColor,
                modifier = Modifier.weight(1f)
            )

            // Hardware Threads & Engine Provider
            MetricCard(
                title = "EXECUTION RUNTIME",
                value = provider.uppercase(),
                subtitle = "$threads threads • ${telemetry.activeThreadCount} active",
                progress = (threads / 8f).coerceIn(0.1f, 1f),
                progressColor = VioletAccent,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    progress: Float,
    progressColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = BorderStroke(1.dp, DarkSurfaceBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = progressColor,
                trackColor = DarkSurfaceBorder
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = TextSecondary,
                maxLines = 1
            )
        }
    }
}
