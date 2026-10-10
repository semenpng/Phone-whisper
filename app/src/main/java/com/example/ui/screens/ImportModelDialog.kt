package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.model.ModelArchitecture
import com.example.model.ModelFileRequirement
import com.example.model.ModelPreset
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.SuccessGreenBright
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletAccent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportModelDialog(
    onImportSuccess: (ModelPreset) -> Unit,
    onImportFiles: suspend (modelId: String, fileName: String, uri: Uri) -> Boolean,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var modelName by remember { mutableStateOf("") }
    var selectedArchitecture by remember { mutableStateOf(ModelArchitecture.NEMO_CTC) }
    var targetLanguage by remember { mutableStateOf("ru") }
    var archMenuExpanded by remember { mutableStateOf(false) }

    var onnxFileUri by remember { mutableStateOf<Uri?>(null) }
    var onnxFileName by remember { mutableStateOf<String?>(null) }

    var tokensFileUri by remember { mutableStateOf<Uri?>(null) }
    var tokensFileName by remember { mutableStateOf<String?>(null) }

    val pickOnnxLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            onnxFileUri = it
            onnxFileName = it.lastPathSegment?.substringAfterLast('/') ?: "model.onnx"
        }
    }

    val pickTokensLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            tokensFileUri = it
            tokensFileName = it.lastPathSegment?.substringAfterLast('/') ?: "tokens.txt"
        }
    }

    var isSaving by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Import Local ONNX Model",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Import your custom converted weights (INT8/FP32 ONNX) and token dictionary directly into sherpa-onnx runtime.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )

                // Model Name
                OutlinedTextField(
                    value = modelName,
                    onValueChange = { modelName = it },
                    label = { Text("Model Title") },
                    placeholder = { Text("e.g. My Custom GigaAM / FastConformer") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = DarkSurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )

                // Architecture Dropdown
                ExposedDropdownMenuBox(
                    expanded = archMenuExpanded,
                    onExpandedChange = { archMenuExpanded = !archMenuExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = "${selectedArchitecture.displayName} (${selectedArchitecture.configClassName})",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Sherpa Configuration Class") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = archMenuExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = DarkSurfaceBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = archMenuExpanded,
                        onDismissRequest = { archMenuExpanded = false },
                        modifier = Modifier.background(DarkSurfaceElevated)
                    ) {
                        ModelArchitecture.values().forEach { arch ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(text = arch.displayName, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text(text = arch.configClassName, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = CyanAccent)
                                    }
                                },
                                onClick = {
                                    selectedArchitecture = arch
                                    archMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Target Language
                OutlinedTextField(
                    value = targetLanguage,
                    onValueChange = { targetLanguage = it },
                    label = { Text("Target Language Code") },
                    placeholder = { Text("ru, en, es, pt, ro, zh, etc.") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = DarkSurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )

                // File Picker: ONNX weights
                Column {
                    Text(
                        text = "ONNX Model File (.onnx)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { pickOnnxLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (onnxFileUri != null) SuccessGreenBright.copy(alpha = 0.1f) else DarkBackground
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (onnxFileUri != null) SuccessGreenBright else DarkSurfaceBorder
                        )
                    ) {
                        Icon(
                            imageVector = if (onnxFileUri != null) Icons.Default.Check else Icons.Default.FolderOpen,
                            contentDescription = "Pick file",
                            tint = if (onnxFileUri != null) SuccessGreenBright else CyanAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        Text(
                            text = onnxFileName ?: "Select .onnx file",
                            color = if (onnxFileUri != null) SuccessGreenBright else TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                // File Picker: tokens.txt
                Column {
                    Text(
                        text = "Vocabulary Tokens File (tokens.txt)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { pickTokensLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (tokensFileUri != null) SuccessGreenBright.copy(alpha = 0.1f) else DarkBackground
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (tokensFileUri != null) SuccessGreenBright else DarkSurfaceBorder
                        )
                    ) {
                        Icon(
                            imageVector = if (tokensFileUri != null) Icons.Default.Check else Icons.Default.AttachFile,
                            contentDescription = "Pick tokens",
                            tint = if (tokensFileUri != null) SuccessGreenBright else VioletAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        Text(
                            text = tokensFileName ?: "Select tokens.txt file",
                            color = if (tokensFileUri != null) SuccessGreenBright else TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val id = "custom_" + modelName.lowercase().replace("\\s+".toRegex(), "_").ifEmpty { "imported_model" }
                    val preset = ModelPreset(
                        id = id,
                        name = modelName.ifEmpty { "Imported ONNX Model" },
                        shortDescription = "Custom imported ${selectedArchitecture.displayName} model",
                        architecture = selectedArchitecture,
                        languageCode = targetLanguage,
                        languageDisplayName = targetLanguage.uppercase(),
                        estimatedSizeMb = 150,
                        huggingFaceRepo = "local/custom",
                        files = listOf(
                            ModelFileRequirement(fileName = onnxFileName ?: "model.onnx", downloadUrl = ""),
                            ModelFileRequirement(fileName = tokensFileName ?: "tokens.txt", downloadUrl = "")
                        )
                    )

                    scope.launch {
                        isSaving = true
                        onnxFileUri?.let { uri ->
                            onImportFiles(id, onnxFileName ?: "model.onnx", uri)
                        }
                        tokensFileUri?.let { uri ->
                            onImportFiles(id, tokensFileName ?: "tokens.txt", uri)
                        }
                        isSaving = false
                        onImportSuccess(preset)
                    }
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                modifier = Modifier.testTag("submit_import_button")
            ) {
                Text(if (isSaving) "Saving..." else "Import & Register", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = DarkSurfaceElevated
    )
}
