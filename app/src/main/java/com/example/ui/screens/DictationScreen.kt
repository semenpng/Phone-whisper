package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TranscriptionResult
import com.example.ui.MainViewModel
import com.example.ui.components.WaveformVisualizer
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreenBright
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletAccent

@Composable
fun DictationScreen(
    viewModel: MainViewModel,
    onNavigateToModels: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedModel by viewModel.selectedModel.collectAsState()
    val executionParams by viewModel.executionParams.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val isTranscribing by viewModel.isTranscribing.collectAsState()
    val waveformAmplitudes by viewModel.waveformAmplitudes.collectAsState()
    val history by viewModel.transcriptionHistory.collectAsState()
    val isFloatingOverlay by viewModel.isFloatingOverlayActive.collectAsState()

    var showSubtitleDialog by remember { mutableStateOf(false) }
    var selectedTranscriptionForSubtitle by remember { mutableStateOf<TranscriptionResult?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            // Read bytes from chosen audio file
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val bytes = stream.readBytes()
                    viewModel.transcribeCustomAudio(bytes)
                }
            } catch (e: Exception) {
                // Audio file reading handled gracefully
            }
        }
    }

    // Mic Pulse Animation
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Active Model Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToModels() }
                .testTag("active_model_banner"),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
            border = BorderStroke(1.dp, DarkSurfaceBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ACTIVE STT MODEL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = selectedModel.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = selectedModel.architecture.configClassName,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyanAccent
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = VioletAccent.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, VioletAccent.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "${executionParams.targetLanguage.uppercase()} • ${executionParams.provider.uppercase()}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VioletAccent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Waveform Visualizer
        WaveformVisualizer(
            amplitudes = waveformAmplitudes,
            isRecording = isRecording,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Hero Push-to-Talk Button
        Box(
            modifier = Modifier
                .size(130.dp),
            contentAlignment = Alignment.Center
        ) {
            // Outer glow if recording
            if (isRecording) {
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(ErrorRed.copy(alpha = 0.35f), Color.Transparent)
                            )
                        )
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(CyanAccent.copy(alpha = 0.15f), Color.Transparent)
                            )
                        )
                )
            }

            // Main Action Button
            Surface(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .clickable {
                        if (isRecording) {
                            viewModel.stopRecordingAndTranscribe()
                        } else {
                            viewModel.startRecording()
                        }
                    }
                    .testTag("push_to_talk_button"),
                shape = CircleShape,
                color = if (isRecording) ErrorRed else CyanAccent,
                shadowElevation = 8.dp,
                border = BorderStroke(
                    3.dp,
                    if (isRecording) Color.White.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.4f)
                )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isTranscribing) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(36.dp)
                        )
                    } else {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Mic else Icons.Default.MicNone,
                            contentDescription = if (isRecording) "Stop Recording" else "Start Push-to-Talk",
                            tint = Color.Black,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = if (isRecording) "RECORDING SPEECH (Tap to Transcribe)" else if (isTranscribing) "TRANSCRIBING AUDIO..." else "TAP TO DICTATE",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isRecording) ErrorRed else if (isTranscribing) VioletAccent else TextSecondary,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Audio File Import & Floating Dictation Quick Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { filePickerLauncher.launch(arrayOf("audio/*")) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("transcribe_audio_file_button"),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkSurfaceElevated),
                border = BorderStroke(1.dp, DarkSurfaceBorder)
            ) {
                Icon(Icons.Default.AudioFile, contentDescription = "Audio file", modifier = Modifier.size(16.dp), tint = CyanAccent)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Transcribe File", fontSize = 12.sp, color = TextPrimary)
            }

            OutlinedButton(
                onClick = {
                    try {
                        val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        viewModel.setFloatingOverlayActive(!isFloatingOverlay)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("toggle_floating_mode_button"),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isFloatingOverlay) VioletAccent.copy(alpha = 0.15f) else DarkSurfaceElevated
                ),
                border = BorderStroke(
                    1.dp,
                    if (isFloatingOverlay) VioletAccent else DarkSurfaceBorder
                )
            ) {
                Icon(Icons.Default.Layers, contentDescription = "Floating mode", modifier = Modifier.size(16.dp), tint = VioletAccent)
                Spacer(modifier = Modifier.width(6.dp))
                Text("System Overlay", fontSize = 12.sp, color = if (isFloatingOverlay) VioletAccent else TextPrimary)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Transcriptions History / Results Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RECOGNITION OUTPUT (${history.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 0.5.sp
            )

            if (history.isNotEmpty()) {
                IconButton(
                    onClick = { viewModel.clearTranscriptionHistory() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (history.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                border = BorderStroke(1.dp, DarkSurfaceBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.MicNone, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No transcriptions yet",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Press the mic to record or pick an audio file to transcribe with ${selectedModel.name}.",
                        fontSize = 12.sp,
                        color = TextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                history.forEachIndexed { index, item ->
                    TranscriptionItemCard(
                        item = item,
                        isLatest = index == 0,
                        onShowSubtitles = {
                            selectedTranscriptionForSubtitle = item
                            showSubtitleDialog = true
                        }
                    )
                }
            }
        }
    }

    // Subtitle Export Modal Dialog
    if (showSubtitleDialog && selectedTranscriptionForSubtitle != null) {
        val srtText = selectedTranscriptionForSubtitle!!.toSrtSubtitle()
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showSubtitleDialog = false },
            title = {
                Text("Subtitles Export (SRT / VTT)", fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Column {
                    Text(
                        text = "Generated timed subtitles using word-level alignment:",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF090D13), RoundedCornerShape(8.dp))
                            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = srtText,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CyanAccent
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(ClipData.newPlainText("SRT Subtitles", srtText))
                        showSubtitleDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black)
                ) {
                    Text("Copy SRT", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSubtitleDialog = false }) {
                    Text("Close", color = TextSecondary)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }
}

@Composable
fun TranscriptionItemCard(
    item: TranscriptionResult,
    isLatest: Boolean,
    onShowSubtitles: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLatest) DarkSurfaceElevated else Color(0xFF14181E)
        ),
        border = BorderStroke(
            1.dp,
            if (isLatest) CyanAccent.copy(alpha = 0.5f) else DarkSurfaceBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Main text
            Text(
                text = item.processedText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                lineHeight = 22.sp
            )

            // Detected emotions or audio event tags (SenseVoice)
            if (item.detectedEmotions.isNotEmpty() || item.audioEvents.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item.detectedEmotions.forEach { emotion ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = SuccessGreenBright.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "[$emotion]",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = SuccessGreenBright,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    item.audioEvents.forEach { event ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = VioletAccent.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = event,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = VioletAccent,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer Row: Telemetry chips & actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Metrics
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${item.metrics.latencyMs}ms",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyanAccent
                    )
                    Text(
                        text = "•",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                    Text(
                        text = "RTF: ${String.format("%.3f", item.metrics.realTimeFactor)}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = SuccessGreenBright
                    )
                }

                // Action buttons: Copy, Share, Subtitles
                Row {
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText("Transcription", item.processedText))
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }

                    IconButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, item.processedText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Transcription"))
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }

                    IconButton(
                        onClick = onShowSubtitles,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Subtitles, contentDescription = "Subtitles", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
