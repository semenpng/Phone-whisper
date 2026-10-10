package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.EngineExecutionParams
import com.example.model.ModelPreset
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelConfigDialog(
    preset: ModelPreset,
    currentParams: EngineExecutionParams,
    configJson: String,
    onSave: (EngineExecutionParams) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    var threads by remember { mutableIntStateOf(currentParams.numThreads) }
    var provider by remember { mutableStateOf(currentParams.provider) }
    var targetLanguage by remember { mutableStateOf(currentParams.targetLanguage) }
    var decodingMethod by remember { mutableStateOf(currentParams.decodingMethod) }
    var useITN by remember { mutableStateOf(currentParams.useITN) }
    var debugMode by remember { mutableStateOf(currentParams.debugMode) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Configure Execution Parameters",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${preset.name} • ${preset.architecture.configClassName}",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = CyanAccent
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DarkBackground,
                    contentColor = CyanAccent
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Parameters", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("JSON Config", fontSize = 12.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedTab == 0) {
                    // Threads slider
                    Text(
                        text = "CPU / Execution Threads: $threads",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Slider(
                        value = threads.toFloat(),
                        onValueChange = { threads = it.toInt() },
                        valueRange = 1f..8f,
                        steps = 6,
                        colors = SliderDefaults.colors(thumbColor = CyanAccent, activeTrackColor = CyanAccent),
                        modifier = Modifier.testTag("threads_slider")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Hardware Execution Provider (CPU / NNAPI)
                    Text(
                        text = "Hardware Acceleration Provider",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("cpu", "nnapi").forEach { prov ->
                            val isChosen = provider == prov
                            OutlinedButton(
                                onClick = { provider = prov },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isChosen) CyanAccent.copy(alpha = 0.15f) else DarkSurfaceElevated
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isChosen) 2.dp else 1.dp,
                                    color = if (isChosen) CyanAccent else DarkSurfaceBorder
                                )
                            ) {
                                Text(
                                    text = prov.uppercase(),
                                    fontSize = 12.sp,
                                    fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isChosen) CyanAccent else TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Target Language
                    Text(
                        text = "Target Recognition Language",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = targetLanguage,
                        onValueChange = { targetLanguage = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = DarkSurfaceBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        placeholder = { Text("e.g. auto, ru, en, es, pt, ro, zh", color = TextMuted) },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Inverse Text Normalization (ITN) toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Inverse Text Normalization",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Formats spoken numbers, currency & dates into symbols",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = useITN,
                            onCheckedChange = { useITN = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyanAccent, checkedTrackColor = CyanAccent.copy(alpha = 0.3f))
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Debug Log mode toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Engine Debug Logging",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Outputs layer timings and tensor dimensions to console",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = debugMode,
                            onCheckedChange = { debugMode = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = VioletAccent, checkedTrackColor = VioletAccent.copy(alpha = 0.3f))
                        )
                    }
                } else {
                    // JSON Config Preview tab
                    Text(
                        text = "Sherpa-ONNX Generated Runtime Config:",
                        fontSize = 11.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF090D13), RoundedCornerShape(8.dp))
                            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = configJson,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CyanAccent,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        currentParams.copy(
                            numThreads = threads,
                            provider = provider,
                            targetLanguage = targetLanguage,
                            useITN = useITN,
                            debugMode = debugMode
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                modifier = Modifier.testTag("save_config_button")
            ) {
                Text("Apply & Save", fontWeight = FontWeight.Bold)
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
