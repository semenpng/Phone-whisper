package com.example

import com.example.model.ModelArchitecture
import com.example.model.ModelPresetsCatalog
import com.example.model.OfflineModelConfig
import com.example.model.OfflineNemoEncDecCtcModelConfig
import com.example.model.OfflineNemoEncDecTransducerModelConfig
import com.example.model.OfflineSenseVoiceModelConfig
import com.example.model.OfflineWhisperModelConfig
import com.example.model.TranscriptionResult
import com.example.model.WordTimestamp
import com.example.plugin.CustomVocabularyPlugin
import com.example.plugin.InverseTextNormalizationPlugin
import com.example.plugin.PunctuationCapitalizationPlugin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testGigaAmCtcConfiguration() {
        val ctcConfig = OfflineNemoEncDecCtcModelConfig(model = "/data/models/giga_am_v3/model.int8.onnx")
        val offlineConfig = OfflineModelConfig(
            tokens = "/data/models/giga_am_v3/tokens.txt",
            numThreads = 4,
            provider = "cpu",
            nemoCtc = ctcConfig
        )

        assertEquals("/data/models/giga_am_v3/model.int8.onnx", offlineConfig.nemoCtc.model)
        assertEquals("/data/models/giga_am_v3/tokens.txt", offlineConfig.tokens)
        assertEquals(4, offlineConfig.numThreads)
        assertTrue(offlineConfig.toSummaryString().contains("OfflineNemoEncDecCtcModelConfig"))
    }

    @Test
    fun testParakeetTdtTransducerConfiguration() {
        val transducerConfig = OfflineNemoEncDecTransducerModelConfig(
            encoder = "/data/models/parakeet/encoder.int8.onnx",
            decoder = "/data/models/parakeet/decoder.int8.onnx",
            joiner = "/data/models/parakeet/joiner.int8.onnx"
        )
        val offlineConfig = OfflineModelConfig(
            tokens = "/data/models/parakeet/tokens.txt",
            numThreads = 4,
            provider = "nnapi",
            nemoTransducer = transducerConfig
        )

        assertEquals("/data/models/parakeet/encoder.int8.onnx", offlineConfig.nemoTransducer.encoder)
        assertEquals("/data/models/parakeet/decoder.int8.onnx", offlineConfig.nemoTransducer.decoder)
        assertEquals("/data/models/parakeet/joiner.int8.onnx", offlineConfig.nemoTransducer.joiner)
        assertTrue(offlineConfig.toSummaryString().contains("OfflineNemoEncDecTransducerModelConfig"))
    }

    @Test
    fun testSenseVoiceConfiguration() {
        val senseVoiceConfig = OfflineSenseVoiceModelConfig(
            model = "/data/models/sense_voice/model.int8.onnx",
            language = "zh",
            useInverseTextNormalization = true
        )
        val offlineConfig = OfflineModelConfig(
            tokens = "/data/models/sense_voice/tokens.txt",
            senseVoice = senseVoiceConfig
        )

        assertEquals("zh", offlineConfig.senseVoice.language)
        assertTrue(offlineConfig.senseVoice.useInverseTextNormalization)
        assertTrue(offlineConfig.toSummaryString().contains("OfflineSenseVoiceModelConfig"))
    }

    @Test
    fun testWhisperConfiguration() {
        val whisperConfig = OfflineWhisperModelConfig(
            encoder = "/data/models/whisper/tiny-encoder.int8.onnx",
            decoder = "/data/models/whisper/tiny-decoder.int8.onnx",
            language = "ru",
            task = "transcribe",
            tailPaddings = 1000
        )
        val offlineConfig = OfflineModelConfig(
            tokens = "/data/models/whisper/tiny-tokens.txt",
            whisper = whisperConfig
        )

        assertEquals("ru", offlineConfig.whisper.language)
        assertEquals("transcribe", offlineConfig.whisper.task)
        assertEquals(1000, offlineConfig.whisper.tailPaddings)
        assertTrue(offlineConfig.toSummaryString().contains("OfflineWhisperModelConfig"))
    }

    @Test
    fun testPresetsCatalogIntegrity() {
        val presets = ModelPresetsCatalog.presets
        assertTrue(presets.isNotEmpty())

        val gigaAm = presets.find { it.id == "giga_am_v3_ru" }
        assertNotNull(gigaAm)
        assertEquals(ModelArchitecture.NEMO_CTC, gigaAm!!.architecture)

        val parakeet = presets.find { it.id == "parakeet_tdt_06b_v2" }
        assertNotNull(parakeet)
        assertEquals(ModelArchitecture.NEMO_TRANSDUCER, parakeet!!.architecture)

        val senseVoice = presets.find { it.id == "sense_voice_small" }
        assertNotNull(senseVoice)
        assertEquals(ModelArchitecture.SENSE_VOICE, senseVoice!!.architecture)

        val roFastConformer = presets.find { it.id == "ro_fast_conformer_ctc" }
        assertNotNull(roFastConformer)
        assertEquals(ModelArchitecture.NEMO_CTC, roFastConformer!!.architecture)
    }

    @Test
    fun testInverseTextNormalizationPlugin() {
        val plugin = InverseTextNormalizationPlugin()
        val rawResult = TranscriptionResult(
            rawText = "я купил двадцать пять пирожков за сто рублей",
            processedText = "я купил двадцать пять пирожков за сто рублей",
            modelId = "giga_am_v3_ru",
            languageCode = "ru"
        )
        val processed = plugin.process(rawResult)
        assertTrue(processed.processedText.contains("25"))
        assertTrue(processed.processedText.contains("100 ₽"))
    }

    @Test
    fun testPunctuationAndCapitalizationPlugin() {
        val plugin = PunctuationCapitalizationPlugin()
        val rawResult = TranscriptionResult(
            rawText = "как тебя зовут",
            processedText = "как тебя зовут",
            modelId = "giga_am_v3_ru",
            languageCode = "ru"
        )
        val processed = plugin.process(rawResult)
        assertTrue(processed.processedText.startsWith("Как"))
        assertTrue(processed.processedText.endsWith("?"))
    }

    @Test
    fun testCustomVocabularyPlugin() {
        val plugin = CustomVocabularyPlugin()
        plugin.addRule("шерпа", "Sherpa-ONNX")

        val rawResult = TranscriptionResult(
            rawText = "распознавание речи через шерпа работает отлично",
            processedText = "распознавание речи через шерпа работает отлично",
            modelId = "test",
            languageCode = "ru"
        )
        val processed = plugin.process(rawResult)
        assertTrue(processed.processedText.contains("Sherpa-ONNX"))
    }

    @Test
    fun testSrtSubtitleGeneration() {
        val result = TranscriptionResult(
            rawText = "One two three four five six",
            processedText = "One two three four five six",
            modelId = "parakeet_tdt_06b_v2",
            languageCode = "en",
            wordTimestamps = listOf(
                WordTimestamp("One", 0.0f, 0.5f),
                WordTimestamp("two", 0.5f, 1.0f),
                WordTimestamp("three", 1.0f, 1.5f),
                WordTimestamp("four", 1.5f, 2.0f),
                WordTimestamp("five", 2.0f, 2.5f),
                WordTimestamp("six", 2.5f, 3.0f)
            )
        )
        val srt = result.toSrtSubtitle()
        assertTrue(srt.contains("00:00:00,000 --> 00:00:03,000"))
        assertTrue(srt.contains("One two three four five six"))
    }
}
