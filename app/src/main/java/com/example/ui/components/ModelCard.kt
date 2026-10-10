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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.CachedModelInfo
import com.example.engine.DownloadState
import com.example.model.ModelArchitecture
import com.example.model.ModelPreset
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreenBright
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletAccent

@Composable
fun ModelCard(
    preset: ModelPreset,
    isSelected: Boolean,
    cachedInfo: CachedModelInfo?,
    downloadState: DownloadState,
    onSelect: () -> Unit,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onDeleteCache: () -> Unit,
    onConfigure: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDownloadingThis = downloadState is DownloadState.Progress && downloadState.modelId == preset.id
    val isCached = cachedInfo?.isComplete == true
    val cachedSizeBytes = cachedInfo?.totalSizeBytes ?: 0L

    val archColor = when (preset.architecture) {
        ModelArchitecture.NEMO_CTC -> CyanAccent
        ModelArchitecture.NEMO_TRANSDUCER -> VioletAccent
        ModelArchitecture.SENSE_VOICE -> Color(0xFFFF9800)
        ModelArchitecture.WHISPER -> Color(0xFF10A37F)
        ModelArchitecture.ZIPFORMER_CTC -> Color(0xFF00E5FF)
        else -> Color(0xFF9C27B0)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("model_card_${preset.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) DarkSurfaceElevated else Color(0xFF161B22)
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) CyanAccent else DarkSurfaceBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Name & Active Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = preset.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = preset.shortDescription,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                }

                if (isSelected) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = CyanAccent.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, CyanAccent)
                    ) {
                        Text(
                            text = "ACTIVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Badges row: Architecture Class & Language
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Config class tag
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = archColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, archColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = preset.architecture.configClassName,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = archColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                // Language tag
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF21262D)
                ) {
                    Text(
                        text = preset.languageDisplayName,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Size
                Text(
                    text = if (isCached) "${cachedSizeBytes / (1024 * 1024)} MB cached" else "~${preset.estimatedSizeMb} MB",
                    fontSize = 11.sp,
                    color = if (isCached) SuccessGreenBright else TextMuted
                )
            }

            // Progress bar if downloading
            if (isDownloadingThis) {
                val progress = downloadState as DownloadState.Progress
                Spacer(modifier = Modifier.height(12.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Downloading ${progress.currentFileName} (${progress.fileIndex}/${progress.totalFiles})",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "${progress.percentage}% • ${String.format("%.1f", progress.speedKbps / 1024f)} MB/s",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CyanAccent
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress.percentage / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = CyanAccent,
                        trackColor = DarkSurfaceBorder
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isDownloadingThis) {
                    OutlinedButton(
                        onClick = onCancelDownload,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("cancel_download_${preset.id}"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                        border = BorderStroke(1.dp, ErrorRed)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Cancel", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cancel", fontSize = 12.sp)
                    }
                } else if (!isCached) {
                    Button(
                        onClick = onDownload,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("download_model_${preset.id}"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = "Download", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download (${preset.estimatedSizeMb} MB)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Cached - Select or Active
                    if (!isSelected) {
                        Button(
                            onClick = onSelect,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("select_model_${preset.id}"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238636))
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Select", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Use Model", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, SuccessGreenBright.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Ready", tint = SuccessGreenBright, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Selected", fontSize = 12.sp, color = SuccessGreenBright)
                        }
                    }

                    // Delete cache button
                    IconButton(
                        onClick = onDeleteCache,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("delete_cache_${preset.id}")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete cache", tint = TextMuted, modifier = Modifier.size(18.dp))
                    }
                }

                // Configure / Inspect dialog button
                IconButton(
                    onClick = onConfigure,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("config_model_${preset.id}")
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Configure parameters", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
