package com.example.model

/**
 * File descriptor representing an ONNX weight file or tokens vocabulary file required by a model.
 */
data class ModelFileRequirement(
    val fileName: String,
    val downloadUrl: String,
    val isOptional: Boolean = false,
    val description: String = ""
)

/**
 * Preset configuration for a supported speech recognition model.
 */
data class ModelPreset(
    val id: String,
    val name: String,
    val shortDescription: String,
    val architecture: ModelArchitecture,
    val languageCode: String,
    val languageDisplayName: String,
    val estimatedSizeMb: Int,
    val huggingFaceRepo: String,
    val files: List<ModelFileRequirement>,
    val defaultThreads: Int = 4,
    val defaultProvider: String = "cpu",
    val isCommunityMobileOptimized: Boolean = true
) {
    fun getTokensFileName(): String =
        files.firstOrNull { it.fileName.contains("tokens", ignoreCase = true) }?.fileName ?: "tokens.txt"

    fun getMainModelFileName(): String =
        files.firstOrNull { it.fileName.endsWith(".onnx") && !it.fileName.contains("joiner") && !it.fileName.contains("decoder") }?.fileName ?: "model.int8.onnx"
}

object ModelPresetsCatalog {
    val presets: List<ModelPreset> = listOf(
        ModelPreset(
            id = "giga_am_v3_ru",
            name = "GigaAM-v3 Russian (NeMo CTC)",
            shortDescription = "State-of-the-art Russian ASR by Sber AI / Sage, converted to sherpa-onnx INT8",
            architecture = ModelArchitecture.NEMO_CTC,
            languageCode = "ru",
            languageDisplayName = "Russian (Русский)",
            estimatedSizeMb = 239,
            huggingFaceRepo = "csukuangfj/sherpa-onnx-nemo-ctc-giga-am-v3-russian-2025-12-16",
            files = listOf(
                ModelFileRequirement(
                    fileName = "model.int8.onnx",
                    downloadUrl = "https://huggingface.co/csukuangfj/sherpa-onnx-nemo-ctc-giga-am-v3-russian-2025-12-16/resolve/main/model.int8.onnx",
                    description = "Quantized NeMo CTC acoustic model (INT8)"
                ),
                ModelFileRequirement(
                    fileName = "tokens.txt",
                    downloadUrl = "https://huggingface.co/csukuangfj/sherpa-onnx-nemo-ctc-giga-am-v3-russian-2025-12-16/resolve/main/tokens.txt",
                    description = "Vocabulary token map for Russian BPE"
                )
            )
        ),

        ModelPreset(
            id = "parakeet_tdt_06b_v2",
            name = "NVIDIA Parakeet TDT 0.6B v2",
            shortDescription = "High-accuracy Token-and-Duration Transducer by NVIDIA with fast multi-pass decoding",
            architecture = ModelArchitecture.NEMO_TRANSDUCER,
            languageCode = "en",
            languageDisplayName = "English",
            estimatedSizeMb = 320,
            huggingFaceRepo = "csukuangfj/sherpa-onnx-nemo-parakeet-tdt-0.6b-v2-int8",
            files = listOf(
                ModelFileRequirement(
                    fileName = "encoder.int8.onnx",
                    downloadUrl = "https://huggingface.co/csukuangfj/sherpa-onnx-nemo-parakeet-tdt-0.6b-v2-int8/resolve/main/encoder.int8.onnx",
                    description = "Conformer TDT Audio Encoder (INT8)"
                ),
                ModelFileRequirement(
                    fileName = "decoder.int8.onnx",
                    downloadUrl = "https://huggingface.co/csukuangfj/sherpa-onnx-nemo-parakeet-tdt-0.6b-v2-int8/resolve/main/decoder.int8.onnx",
                    description = "Prediction Network / Decoder (INT8)"
                ),
                ModelFileRequirement(
                    fileName = "joiner.int8.onnx",
                    downloadUrl = "https://huggingface.co/csukuangfj/sherpa-onnx-nemo-parakeet-tdt-0.6b-v2-int8/resolve/main/joiner.int8.onnx",
                    description = "Joint Network (INT8)"
                ),
                ModelFileRequirement(
                    fileName = "tokens.txt",
                    downloadUrl = "https://huggingface.co/csukuangfj/sherpa-onnx-nemo-parakeet-tdt-0.6b-v2-int8/resolve/main/tokens.txt",
                    description = "WordPiece/SentencePiece token vocabulary"
                )
            )
        ),

        ModelPreset(
            id = "sense_voice_small",
            name = "SenseVoice-Small (Alibaba FunASR)",
            shortDescription = "Multilingual fast ASR with automatic emotion, audio event tags & built-in inverse text normalization",
            architecture = ModelArchitecture.SENSE_VOICE,
            languageCode = "auto",
            languageDisplayName = "Multilingual (ZH, EN, JA, KO, YUE)",
            estimatedSizeMb = 235,
            huggingFaceRepo = "csukuangfj/sherpa-onnx-sense-voice-zh-en-ja-ko-yue-int8-2025-09-09",
            files = listOf(
                ModelFileRequirement(
                    fileName = "model.int8.onnx",
                    downloadUrl = "https://huggingface.co/csukuangfj/sherpa-onnx-sense-voice-zh-en-ja-ko-yue-int8-2025-09-09/resolve/main/model.int8.onnx",
                    description = "SenseVoice quantized acoustic model"
                ),
                ModelFileRequirement(
                    fileName = "tokens.txt",
                    downloadUrl = "https://huggingface.co/csukuangfj/sherpa-onnx-sense-voice-zh-en-ja-ko-yue-int8-2025-09-09/resolve/main/tokens.txt",
                    description = "Multilingual SenseVoice tokens dictionary"
                )
            )
        ),

        ModelPreset(
            id = "fastconformer_es_ctc",
            name = "FastConformer CTC Spanish",
            shortDescription = "NVIDIA NeMo Spanish Conformer CTC hybrid large model with INT8 quantization",
            architecture = ModelArchitecture.NEMO_CTC,
            languageCode = "es",
            languageDisplayName = "Spanish (Español)",
            estimatedSizeMb = 210,
            huggingFaceRepo = "csukuangfj/sherpa-onnx-nemo-fast-conformer-ctc-es-1424-int8",
            files = listOf(
                ModelFileRequirement(
                    fileName = "model.int8.onnx",
                    downloadUrl = "https://huggingface.co/csukuangfj/sherpa-onnx-nemo-fast-conformer-ctc-es-1424-int8/resolve/main/model.int8.onnx",
                    description = "NeMo Spanish Conformer CTC INT8 model"
                ),
                ModelFileRequirement(
                    fileName = "tokens.txt",
                    downloadUrl = "https://huggingface.co/csukuangfj/sherpa-onnx-nemo-fast-conformer-ctc-es-1424-int8/resolve/main/tokens.txt",
                    description = "Spanish SentencePiece tokens vocabulary"
                )
            )
        ),

        ModelPreset(
            id = "fastconformer_pt_ctc",
            name = "FastConformer CTC Portuguese",
            shortDescription = "NVIDIA NeMo Brazilian & European Portuguese hybrid large Conformer CTC",
            architecture = ModelArchitecture.NEMO_CTC,
            languageCode = "pt",
            languageDisplayName = "Portuguese (Português)",
            estimatedSizeMb = 215,
            huggingFaceRepo = "nvidia/stt_pt_fastconformer_hybrid_large_pc",
            files = listOf(
                ModelFileRequirement(
                    fileName = "model.int8.onnx",
                    downloadUrl = "https://huggingface.co/csukuangfj/sherpa-onnx-nemo-fast-conformer-ctc-pt-int8/resolve/main/model.int8.onnx",
                    description = "Portuguese Conformer CTC INT8 model"
                ),
                ModelFileRequirement(
                    fileName = "tokens.txt",
                    downloadUrl = "https://huggingface.co/csukuangfj/sherpa-onnx-nemo-fast-conformer-ctc-pt-int8/resolve/main/tokens.txt",
                    description = "Portuguese tokens vocabulary"
                )
            )
        ),

        ModelPreset(
            id = "ro_fast_conformer_ctc",
            name = "Romanian FastConformer CTC",
            shortDescription = "UPB NLP Romanian FastConformer model optimized for Romanian speech recognition",
            architecture = ModelArchitecture.NEMO_CTC,
            languageCode = "ro",
            languageDisplayName = "Romanian (Română)",
            estimatedSizeMb = 185,
            huggingFaceRepo = "upb-nlp/ro-fast-conformer",
            files = listOf(
                ModelFileRequirement(
                    fileName = "model.int8.onnx",
                    downloadUrl = "https://huggingface.co/csukuangfj/sherpa-onnx-nemo-fast-conformer-ctc-ro-int8/resolve/main/model.int8.onnx",
                    description = "Romanian Conformer CTC INT8 model"
                ),
                ModelFileRequirement(
                    fileName = "tokens.txt",
                    downloadUrl = "https://huggingface.co/csukuangfj/sherpa-onnx-nemo-fast-conformer-ctc-ro-int8/resolve/main/tokens.txt",
                    description = "Romanian BPE tokens vocabulary"
                )
            )
        ),

        ModelPreset(
            id = "whisper_tiny_multilingual",
            name = "OpenAI Whisper Tiny (INT8)",
            shortDescription = "Lightweight 99-language Whisper model optimized for mobile phones and low memory",
            architecture = ModelArchitecture.WHISPER,
            languageCode = "auto",
            languageDisplayName = "Multilingual (99+ languages)",
            estimatedSizeMb = 75,
            huggingFaceRepo = "csukuangfj/sherpa-onnx-whisper-tiny",
            files = listOf(
                ModelFileRequirement(
                    fileName = "tiny-encoder.int8.onnx",
                    downloadUrl = "https://huggingface.co/csukuangfj/sherpa-onnx-whisper-tiny/resolve/main/tiny-encoder.int8.onnx",
                    description = "Whisper audio spectrogram encoder"
                ),
                ModelFileRequirement(
                    fileName = "tiny-decoder.int8.onnx",
                    downloadUrl = "https://huggingface.co/csukuangfj/sherpa-onnx-whisper-tiny/resolve/main/tiny-decoder.int8.onnx",
                    description = "Whisper autoregressive text decoder"
                ),
                ModelFileRequirement(
                    fileName = "tiny-tokens.txt",
                    downloadUrl = "https://huggingface.co/csukuangfj/sherpa-onnx-whisper-tiny/resolve/main/tiny-tokens.txt",
                    description = "Whisper multilingual BPE vocabulary"
                )
            )
        )
    )

    fun findById(id: String): ModelPreset? = presets.find { it.id == id }
}
