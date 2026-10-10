package com.example.model

/**
 * Detailed word timing information for subtitle export or karaoke/highlight rendering.
 */
data class WordTimestamp(
    val word: String,
    val startTimeSec: Float,
    val endTimeSec: Float
)

/**
 * Execution metrics collected during speech recognition inference.
 */
data class InferenceMetrics(
    val latencyMs: Long = 0,
    val audioDurationMs: Long = 0,
    val realTimeFactor: Float = 0f, // latencyMs / audioDurationMs (< 1.0 means faster than real-time)
    val peakMemoryMb: Float = 0f,
    val threadCount: Int = 4,
    val executionProvider: String = "cpu",
    val configClassUsed: String = ""
)

/**
 * Full transcription output from the speech recognition engine and plugin pipeline.
 */
data class TranscriptionResult(
    val rawText: String,
    val processedText: String = rawText,
    val modelId: String,
    val languageCode: String,
    val timestamp: Long = System.currentTimeMillis(),
    val wordTimestamps: List<WordTimestamp> = emptyList(),
    val metrics: InferenceMetrics = InferenceMetrics(),
    val detectedEmotions: List<String> = emptyList(), // SenseVoice can output [HAPPY], [SAD], [LAUGHTER]
    val audioEvents: List<String> = emptyList() // SenseVoice audio event tags [APPLAUSE], [BGM]
) {
    fun toSrtSubtitle(): String {
        if (wordTimestamps.isEmpty()) {
            val start = formatTimestampSrt(0f)
            val end = formatTimestampSrt((metrics.audioDurationMs / 1000f).coerceAtLeast(1f))
            return "1\n$start --> $end\n$processedText\n"
        }
        val sb = StringBuilder()
        // Group words into short subtitle lines of ~5-7 words
        val chunks = wordTimestamps.chunked(6)
        chunks.forEachIndexed { index, chunk ->
            val startSec = chunk.first().startTimeSec
            val endSec = chunk.last().endTimeSec
            val text = chunk.joinToString(" ") { it.word }
            sb.append("${index + 1}\n")
            sb.append("${formatTimestampSrt(startSec)} --> ${formatTimestampSrt(endSec)}\n")
            sb.append("$text\n\n")
        }
        return sb.toString()
    }

    private fun formatTimestampSrt(sec: Float): String {
        val totalMs = (sec * 1000).toLong()
        val hours = totalMs / 3600000
        val minutes = (totalMs % 3600000) / 60000
        val seconds = (totalMs % 60000) / 1000
        val millis = totalMs % 1000
        return String.format("%02d:%02d:%02d,%03d", hours, minutes, seconds, millis)
    }
}
