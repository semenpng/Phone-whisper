package com.example.model

/**
 * Kotlin representations of Sherpa-ONNX offline speech recognition model configurations.
 * These correspond directly to the Sherpa-ONNX C++/Kotlin native bindings:
 * - OfflineWhisperModelConfig for OpenAI Whisper models
 * - OfflineNemoEncDecTransducerModelConfig for NeMo Parakeet / Transducer / TDT models
 * - OfflineNemoEncDecCtcModelConfig for NeMo CTC models (e.g. GigaAM-v3, FastConformer CTC)
 * - OfflineSenseVoiceModelConfig for Alibaba FunASR SenseVoice models
 * - OfflineZipformerCtcModelConfig / OfflineMoonshineModelConfig for other architectures
 */

enum class ModelArchitecture(val displayName: String, val configClassName: String, val description: String) {
    NEMO_CTC(
        displayName = "NeMo CTC",
        configClassName = "OfflineNemoEncDecCtcModelConfig",
        description = "Optimized for GigaAM-v3 (Russian), Romanian FastConformer, Spanish/Portuguese Conformer CTC"
    ),
    NEMO_TRANSDUCER(
        displayName = "NeMo Transducer / TDT",
        configClassName = "OfflineNemoEncDecTransducerModelConfig",
        description = "Optimized for NVIDIA Parakeet TDT 0.6B (Encoder + Decoder + Joiner)"
    ),
    SENSE_VOICE(
        displayName = "SenseVoice (FunASR)",
        configClassName = "OfflineSenseVoiceModelConfig",
        description = "Alibaba multilingual model (ZH, EN, JA, KO, YUE) with native ITN and emotion detection"
    ),
    WHISPER(
        displayName = "Whisper",
        configClassName = "OfflineWhisperModelConfig",
        description = "OpenAI Whisper encoder-decoder architecture with multilingual tokenization"
    ),
    ZIPFORMER_CTC(
        displayName = "Zipformer CTC",
        configClassName = "OfflineZipformerCtcModelConfig",
        description = "Next-gen Kaldi Zipformer lightweight CTC models"
    ),
    MOONSHINE(
        displayName = "Moonshine",
        configClassName = "OfflineMoonshineModelConfig",
        description = "Resource-efficient on-device transformer model"
    ),
    CUSTOM(
        displayName = "Custom ONNX",
        configClassName = "Dynamic Config",
        description = "User-supplied custom ONNX models and configurations"
    )
}

/**
 * Configuration for NeMo CTC models such as GigaAM-v3, ro-fast-conformer, stt_es_fastconformer, stt_pt_fastconformer.
 */
data class OfflineNemoEncDecCtcModelConfig(
    var model: String = "",
)

/**
 * Configuration for NeMo Transducer / TDT models like Parakeet-TDT 0.6B.
 */
data class OfflineNemoEncDecTransducerModelConfig(
    var encoder: String = "",
    var decoder: String = "",
    var joiner: String = "",
)

/**
 * Configuration for FunASR SenseVoice models.
 */
data class OfflineSenseVoiceModelConfig(
    var model: String = "",
    var language: String = "auto",
    var useInverseTextNormalization: Boolean = true,
)

/**
 * Configuration for Whisper models.
 */
data class OfflineWhisperModelConfig(
    var encoder: String = "",
    var decoder: String = "",
    var language: String = "",
    var task: String = "transcribe",
    var tailPaddings: Int = -1,
)

/**
 * Configuration for Zipformer CTC models.
 */
data class OfflineZipformerCtcModelConfig(
    var model: String = "",
)

/**
 * Configuration for Moonshine models.
 */
data class OfflineMoonshineModelConfig(
    var preprocessor: String = "",
    var encoder: String = "",
    var uncachedDecoder: String = "",
    var cachedDecoder: String = "",
)

/**
 * Master offline model configuration combining individual model configurations,
 * execution provider (CPU / NNAPI), thread count, tokens mapping, and debugging.
 */
data class OfflineModelConfig(
    var tokens: String = "",
    var numThreads: Int = 2,
    var debug: Boolean = false,
    var provider: String = "cpu", // "cpu", "nnapi", "gpu"
    var modelType: String = "",
    var nemoCtc: OfflineNemoEncDecCtcModelConfig = OfflineNemoEncDecCtcModelConfig(),
    var nemoTransducer: OfflineNemoEncDecTransducerModelConfig = OfflineNemoEncDecTransducerModelConfig(),
    var senseVoice: OfflineSenseVoiceModelConfig = OfflineSenseVoiceModelConfig(),
    var whisper: OfflineWhisperModelConfig = OfflineWhisperModelConfig(),
    var zipformerCtc: OfflineZipformerCtcModelConfig = OfflineZipformerCtcModelConfig(),
    var moonshine: OfflineMoonshineModelConfig = OfflineMoonshineModelConfig()
) {
    fun toSummaryString(): String {
        val activeConfig = when {
            nemoCtc.model.isNotEmpty() -> "OfflineNemoEncDecCtcModelConfig(model=${nemoCtc.model.substringAfterLast('/')})"
            nemoTransducer.encoder.isNotEmpty() -> "OfflineNemoEncDecTransducerModelConfig(enc=${nemoTransducer.encoder.substringAfterLast('/')}, dec=${nemoTransducer.decoder.substringAfterLast('/')}, join=${nemoTransducer.joiner.substringAfterLast('/')})"
            senseVoice.model.isNotEmpty() -> "OfflineSenseVoiceModelConfig(model=${senseVoice.model.substringAfterLast('/')}, lang=${senseVoice.language}, itn=${senseVoice.useInverseTextNormalization})"
            whisper.encoder.isNotEmpty() -> "OfflineWhisperModelConfig(enc=${whisper.encoder.substringAfterLast('/')}, dec=${whisper.decoder.substringAfterLast('/')}, lang=${whisper.language})"
            zipformerCtc.model.isNotEmpty() -> "OfflineZipformerCtcModelConfig(model=${zipformerCtc.model.substringAfterLast('/')})"
            else -> "Not configured"
        }
        return "Config: $activeConfig, tokens=${tokens.substringAfterLast('/')}, threads=$numThreads, provider=$provider"
    }
}
