package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.AudioRecorderManager
import com.example.engine.CachedModelInfo
import com.example.engine.DownloadState
import com.example.engine.EngineExecutionParams
import com.example.engine.ModelStorageManager
import com.example.engine.SpeechRecognitionEngine
import com.example.model.ModelArchitecture
import com.example.model.ModelPreset
import com.example.model.ModelPresetsCatalog
import com.example.model.TranscriptionResult
import com.example.monitor.ExecutionLogger
import com.example.monitor.LogEntry
import com.example.monitor.LogLevel
import com.example.monitor.ResourceMonitor
import com.example.monitor.SystemTelemetry
import com.example.plugin.AudioTextPlugin
import com.example.plugin.PluginPipelineManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val storageManager = ModelStorageManager(application)
    val pipelineManager = PluginPipelineManager(application)
    val speechEngine = SpeechRecognitionEngine(application, storageManager, pipelineManager)
    val audioRecorder = AudioRecorderManager()
    val resourceMonitor = ResourceMonitor(application)

    // UI state flows
    val selectedModel: StateFlow<ModelPreset> = speechEngine.selectedModel
    val executionParams: StateFlow<EngineExecutionParams> = speechEngine.executionParams
    val isRecording: StateFlow<Boolean> = audioRecorder.isRecording
    val isTranscribing: StateFlow<Boolean> = speechEngine.isTranscribing
    val activeConfigJson: StateFlow<String> = speechEngine.activeConfigJson
    val transcriptionHistory: StateFlow<List<TranscriptionResult>> = speechEngine.transcriptionHistory
    val downloadState: StateFlow<DownloadState> = storageManager.downloadState
    val telemetry: StateFlow<SystemTelemetry> = resourceMonitor.telemetry
    val plugins: StateFlow<List<AudioTextPlugin>> = pipelineManager.plugins
    val logs: StateFlow<List<LogEntry>> = ExecutionLogger.logs
    val currentAmplitude: StateFlow<Float> = audioRecorder.currentAmplitude
    val waveformAmplitudes: StateFlow<List<Float>> = audioRecorder.waveformAmplitudes

    // Cached models status map
    private val _cachedModelsMap = MutableStateFlow<Map<String, CachedModelInfo>>(emptyMap())
    val cachedModelsMap: StateFlow<Map<String, CachedModelInfo>> = _cachedModelsMap.asStateFlow()

    private val _customModels = MutableStateFlow<List<CachedModelInfo>>(emptyList())
    val customModels: StateFlow<List<CachedModelInfo>> = _customModels.asStateFlow()

    // Floating Mode overlay state (Phone Whisper feature)
    private val _isFloatingOverlayActive = MutableStateFlow(false)
    val isFloatingOverlayActive: StateFlow<Boolean> = _isFloatingOverlayActive.asStateFlow()

    init {
        resourceMonitor.startMonitoring()
        refreshCacheStatus()
        ExecutionLogger.i("MainViewModel", "Phone Whisper STT Studio initialized with Sherpa-ONNX architecture support")
    }

    override fun onCleared() {
        super.onCleared()
        resourceMonitor.stopMonitoring()
    }

    fun refreshCacheStatus() {
        viewModelScope.launch {
            val map = mutableMapOf<String, CachedModelInfo>()
            for (preset in ModelPresetsCatalog.presets) {
                map[preset.id] = storageManager.getCachedModelInfo(preset)
            }
            _cachedModelsMap.value = map
            _customModels.value = storageManager.getCustomModels()
        }
    }

    fun selectModel(preset: ModelPreset) {
        speechEngine.selectModel(preset)
    }

    fun updateExecutionParams(params: EngineExecutionParams) {
        speechEngine.updateExecutionParams(params)
    }

    fun startRecording() {
        val started = audioRecorder.startRecording { peakDbfs, framesDelta ->
            resourceMonitor.updateAudioSignal(peakDbfs, framesDelta)
        }
        if (started) {
            ExecutionLogger.i("MainViewModel", "Microphone recording started")
        }
    }

    fun stopRecordingAndTranscribe() {
        viewModelScope.launch {
            val pcmBytes = audioRecorder.stopRecording()
            if (pcmBytes.isNotEmpty()) {
                val result = speechEngine.transcribeAudio(pcmBytes)
                resourceMonitor.updateInferenceMetrics(result.metrics.latencyMs, result.metrics.realTimeFactor)
            }
        }
    }

    fun transcribeCustomAudio(pcmBytes: ByteArray) {
        viewModelScope.launch {
            val result = speechEngine.transcribeAudio(pcmBytes)
            resourceMonitor.updateInferenceMetrics(result.metrics.latencyMs, result.metrics.realTimeFactor)
        }
    }

    fun downloadModel(preset: ModelPreset) {
        viewModelScope.launch {
            val success = storageManager.downloadPreset(preset)
            refreshCacheStatus()
            if (success && speechEngine.selectedModel.value.id == preset.id) {
                speechEngine.selectModel(preset) // reload
            }
        }
    }

    fun cancelDownload() {
        storageManager.cancelDownload()
    }

    fun evictModelCache(modelId: String) {
        viewModelScope.launch {
            storageManager.deleteModelCache(modelId)
            refreshCacheStatus()
        }
    }

    fun togglePlugin(pluginId: String, enabled: Boolean) {
        pipelineManager.togglePlugin(pluginId, enabled)
    }

    fun addVocabularyRule(target: String, replacement: String) {
        pipelineManager.vocabPlugin.addRule(target, replacement)
    }

    fun removeVocabularyRule(target: String) {
        pipelineManager.vocabPlugin.removeRule(target)
    }

    fun getVocabularyRules(): Map<String, String> = pipelineManager.vocabPlugin.getRules()

    fun clearTranscriptionHistory() {
        speechEngine.clearHistory()
    }

    fun clearLogs() {
        ExecutionLogger.clear()
    }

    fun setFloatingOverlayActive(active: Boolean) {
        _isFloatingOverlayActive.value = active
        ExecutionLogger.i("MainViewModel", "Floating push-to-talk widget state: $active")
    }

    suspend fun importLocalModelFile(modelId: String, fileName: String, uri: Uri): Boolean {
        val success = storageManager.importLocalFile(modelId, fileName, uri)
        refreshCacheStatus()
        return success
    }
}
