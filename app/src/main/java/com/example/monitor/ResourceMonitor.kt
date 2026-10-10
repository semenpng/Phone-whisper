package com.example.monitor

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class SystemTelemetry(
    val jvmHeapUsedMb: Float = 0f,
    val jvmHeapMaxMb: Float = 0f,
    val nativeHeapAllocatedMb: Float = 0f,
    val deviceAvailMemMb: Float = 0f,
    val deviceTotalMemMb: Float = 0f,
    val lowMemoryState: Boolean = false,
    val activeThreadCount: Int = 0,
    val lastLatencyMs: Long = 0,
    val lastRtf: Float = 0f,
    val audioBufferPeakDbfs: Float = -60f,
    val audioFramesProcessed: Long = 0
)

class ResourceMonitor(private val context: Context) {
    private val _telemetry = MutableStateFlow(SystemTelemetry())
    val telemetry: StateFlow<SystemTelemetry> = _telemetry.asStateFlow()

    private var monitorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    fun startMonitoring() {
        if (monitorJob != null) return
        monitorJob = scope.launch {
            while (isActive) {
                sampleTelemetry()
                delay(800) // sample every 800ms
            }
        }
        ExecutionLogger.i("ResourceMonitor", "Real-time resource telemetry daemon started")
    }

    fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
    }

    fun updateInferenceMetrics(latencyMs: Long, rtf: Float) {
        _telemetry.value = _telemetry.value.copy(
            lastLatencyMs = latencyMs,
            lastRtf = rtf
        )
    }

    fun updateAudioSignal(peakDbfs: Float, framesDelta: Long) {
        _telemetry.value = _telemetry.value.copy(
            audioBufferPeakDbfs = peakDbfs,
            audioFramesProcessed = _telemetry.value.audioFramesProcessed + framesDelta
        )
    }

    private fun sampleTelemetry() {
        val runtime = Runtime.getRuntime()
        val jvmUsedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024f * 1024f)
        val jvmMaxMb = runtime.maxMemory() / (1024f * 1024f)
        val nativeAllocMb = Debug.getNativeHeapAllocatedSize() / (1024f * 1024f)

        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)

        val availSysMb = memInfo.availMem / (1024f * 1024f)
        val totalSysMb = memInfo.totalMem / (1024f * 1024f)
        val threadCount = Thread.activeCount()

        _telemetry.value = _telemetry.value.copy(
            jvmHeapUsedMb = jvmUsedMb,
            jvmHeapMaxMb = jvmMaxMb,
            nativeHeapAllocatedMb = nativeAllocMb,
            deviceAvailMemMb = availSysMb,
            deviceTotalMemMb = totalSysMb,
            lowMemoryState = memInfo.lowMemory,
            activeThreadCount = threadCount
        )
    }
}
