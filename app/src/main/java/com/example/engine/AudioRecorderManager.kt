package com.example.engine

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.example.monitor.ExecutionLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import kotlin.math.log10
import kotlin.math.sqrt

class AudioRecorderManager {

    companion object {
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _currentAmplitude = MutableStateFlow(0f) // 0.0 to 1.0 normalized
    val currentAmplitude: StateFlow<Float> = _currentAmplitude.asStateFlow()

    private val _currentDbfs = MutableStateFlow(-60f)
    val currentDbfs: StateFlow<Float> = _currentDbfs.asStateFlow()

    private val _waveformAmplitudes = MutableStateFlow<List<Float>>(emptyList())
    val waveformAmplitudes: StateFlow<List<Float>> = _waveformAmplitudes.asStateFlow()

    private val recordedPcmStream = ByteArrayOutputStream()

    @SuppressLint("MissingPermission")
    fun startRecording(onSignalUpdate: ((peakDbfs: Float, framesDelta: Long) -> Unit)? = null): Boolean {
        if (_isRecording.value) return true

        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            .coerceAtLeast(SAMPLE_RATE / 10 * 2) // at least 100ms buffer

        try {
            val record = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                ExecutionLogger.e("AudioRecorder", "AudioRecord initialization failed (state != INITIALIZED)")
                record.release()
                return false
            }

            audioRecord = record
            recordedPcmStream.reset()
            record.startRecording()
            _isRecording.value = true
            _waveformAmplitudes.value = emptyList()

            ExecutionLogger.i("AudioRecorder", "Started microphone stream: 16kHz 16-bit mono PCM, buffer size=$bufferSize bytes")

            recordingJob = scope.launch {
                val readBuffer = ShortArray(1024)
                val byteBuffer = ByteArray(2048)

                while (isActive && _isRecording.value) {
                    val readShorts = record.read(readBuffer, 0, readBuffer.size)
                    if (readShorts > 0) {
                        // Convert shorts to bytes for PCM stream
                        for (i in 0 until readShorts) {
                            val sample = readBuffer[i]
                            byteBuffer[i * 2] = (sample.toInt() and 0x00FF).toByte()
                            byteBuffer[i * 2 + 1] = ((sample.toInt() shr 8) and 0x00FF).toByte()
                        }
                        synchronized(recordedPcmStream) {
                            recordedPcmStream.write(byteBuffer, 0, readShorts * 2)
                        }

                        // Calculate RMS amplitude
                        var sumSquare = 0.0
                        for (i in 0 until readShorts) {
                            val sample = readBuffer[i].toDouble()
                            sumSquare += sample * sample
                        }
                        val rms = sqrt(sumSquare / readShorts)
                        val normalized = (rms / 32768.0).toFloat().coerceIn(0f, 1f)
                        val dbfs = if (rms > 0) (20 * log10(rms / 32767.0)).toFloat().coerceIn(-60f, 0f) else -60f

                        _currentAmplitude.value = normalized
                        _currentDbfs.value = dbfs

                        val currentWaveform = _waveformAmplitudes.value.toMutableList()
                        currentWaveform.add(normalized)
                        if (currentWaveform.size > 50) currentWaveform.removeAt(0)
                        _waveformAmplitudes.value = currentWaveform

                        onSignalUpdate?.invoke(dbfs, readShorts.toLong())
                    }
                }
            }
            return true
        } catch (e: Exception) {
            ExecutionLogger.e("AudioRecorder", "Failed to start AudioRecord: ${e.message}")
            _isRecording.value = false
            return false
        }
    }

    fun stopRecording(): ByteArray {
        _isRecording.value = false
        recordingJob?.cancel()
        recordingJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            ExecutionLogger.w("AudioRecorder", "Error stopping AudioRecord: ${e.message}")
        } finally {
            audioRecord = null
        }

        _currentAmplitude.value = 0f
        _currentDbfs.value = -60f

        val recordedBytes = synchronized(recordedPcmStream) {
            recordedPcmStream.toByteArray()
        }
        val durationSec = recordedBytes.size / (SAMPLE_RATE * 2f)
        ExecutionLogger.i("AudioRecorder", "Recording stopped. Captured ${recordedBytes.size} bytes (${String.format("%.2f", durationSec)}s audio)")
        return recordedBytes
    }
}
