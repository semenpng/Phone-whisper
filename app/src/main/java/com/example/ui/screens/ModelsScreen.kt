package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ModelPreset
import com.example.model.ModelPresetsCatalog
import com.example.ui.MainViewModel
import com.example.ui.components.ModelCard
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
fun ModelsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedModel by viewModel.selectedModel.collectAsState()
    val executionParams by viewModel.executionParams.collectAsState()
    val cachedMap by viewModel.cachedModelsMap.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()
    val configJson by viewModel.activeConfigJson.collectAsState()
    val customModels by viewModel.customModels.collectAsState()

    var showConfigDialogForPreset by remember { mutableStateOf<ModelPreset?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }

    val totalStorageMb = viewModel.storageManager.getUsedStorageMb()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        // Top Storage Banner & Actions
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
            border = BorderStroke(1.dp, DarkSurfaceBorder)
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
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "LOCAL MODEL CACHE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${String.format("%.1f", totalStorageMb)} MB allocated",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = { viewModel.refreshCacheStatus() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh cache", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }

                    Button(
                        onClick = { showImportDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("import_model_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import ONNX", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Preset List
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SUPPORTED SHERPA-ONNX PRESETS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "${ModelPresetsCatalog.presets.size} presets",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(ModelPresetsCatalog.presets, key = { it.id }) { preset ->
                val cached = cachedMap[preset.id]
                val isSelected = selectedModel.id == preset.id

                ModelCard(
                    preset = preset,
                    isSelected = isSelected,
                    cachedInfo = cached,
                    downloadState = downloadState,
                    onSelect = { viewModel.selectModel(preset) },
                    onDownload = { viewModel.downloadModel(preset) },
                    onCancelDownload = { viewModel.cancelDownload() },
                    onDeleteCache = { viewModel.evictModelCache(preset.id) },
                    onConfigure = { showConfigDialogForPreset = preset }
                )
            }
        }
    }

    // Model Config Parameters Dialog
    showConfigDialogForPreset?.let { preset ->
        ModelConfigDialog(
            preset = preset,
            currentParams = executionParams,
            configJson = configJson,
            onSave = { updatedParams ->
                viewModel.updateExecutionParams(updatedParams)
                showConfigDialogForPreset = null
            },
            onDismiss = { showConfigDialogForPreset = null }
        )
    }

    // Import Local Model Dialog
    if (showImportDialog) {
        ImportModelDialog(
            onImportSuccess = { newPreset ->
                viewModel.selectModel(newPreset)
                showImportDialog = false
            },
            onImportFiles = { modelId, fileName, uri ->
                viewModel.importLocalModelFile(modelId, fileName, uri)
            },
            onDismiss = { showImportDialog = false }
        )
    }
}
