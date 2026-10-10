package com.example.plugin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.example.model.TranscriptionResult
import com.example.monitor.ExecutionLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.regex.Pattern

interface AudioTextPlugin {
    val id: String
    val name: String
    val description: String
    val category: String
    var isEnabled: Boolean

    fun process(input: TranscriptionResult): TranscriptionResult
}

/**
 * Inverse Text Normalization Plugin: converts spoken numbers, currencies and phone formats
 * e.g. "twenty five" -> "25", "сто рублей" -> "100 ₽", "one two three" -> "123"
 */
class InverseTextNormalizationPlugin : AudioTextPlugin {
    override val id = "itn_plugin"
    override val name = "Inverse Text Normalization (ITN)"
    override val description = "Converts spoken numbers, currency, dates, and units into standard digits and symbols"
    override val category = "Formatting"
    override var isEnabled = true

    private val numberMapRu = mapOf(
        "ноль" to "0", "один" to "1", "два" to "2", "три" to "3", "четыре" to "4",
        "пять" to "5", "шесть" to "6", "семь" to "7", "восемь" to "8", "девять" to "9",
        "десять" to "10", "двадцать" to "20", "тридцать" to "30", "сорок" to "40",
        "пятьдесят" to "50", "шестьдесят" to "60", "семьдесят" to "70", "восемьдесят" to "80",
        "девяносто" to "90", "сто" to "100", "тысяча" to "1000", "миллион" to "1000000"
    )

    private val numberMapEn = mapOf(
        "zero" to "0", "one" to "1", "two" to "2", "three" to "3", "four" to "4",
        "five" to "5", "six" to "6", "seven" to "7", "eight" to "8", "nine" to "9",
        "ten" to "10", "eleven" to "11", "twelve" to "12", "twenty" to "20",
        "thirty" to "30", "forty" to "40", "fifty" to "50", "hundred" to "100",
        "thousand" to "1000"
    )

    override fun process(input: TranscriptionResult): TranscriptionResult {
        if (!isEnabled) return input
        var text = input.processedText

        // Compound numbers (e.g. "двадцать пять" -> "25", "тридцать пять" -> "35")
        val compoundTens = mapOf(
            "двадцать" to 20, "тридцать" to 30, "сорок" to 40, "пятьдесят" to 50,
            "шестьдесят" to 60, "семьдесят" to 70, "восемьдесят" to 80, "девяносто" to 90
        )
        val units = mapOf(
            "один" to 1, "два" to 2, "три" to 3, "четыре" to 4, "пять" to 5,
            "шесть" to 6, "семь" to 7, "восемь" to 8, "девять" to 9
        )
        for ((tenWord, tenVal) in compoundTens) {
            for ((unitWord, unitVal) in units) {
                val combinedWord = "$tenWord $unitWord"
                val combinedVal = (tenVal + unitVal).toString()
                text = text.replace(Regex("(?U)\\b(?i)$combinedWord\\b"), combinedVal)
            }
        }

        // Apply Russian ITN replacements
        for ((word, digit) in numberMapRu) {
            val regex = Regex("(?U)\\b(?i)$word\\b")
            text = text.replace(regex, digit)
        }

        // Apply English ITN replacements
        for ((word, digit) in numberMapEn) {
            val regex = Regex("(?U)\\b(?i)$word\\b")
            text = text.replace(regex, digit)
        }

        // Currency symbols
        text = text.replace(Regex("(?U)\\b(?i)рублей\\b"), "₽")
            .replace(Regex("(?U)\\b(?i)рубля\\b"), "₽")
            .replace(Regex("(?U)\\b(?i)рубль\\b"), "₽")
            .replace(Regex("(?U)\\b(?i)dollars\\b"), "$")
            .replace(Regex("(?U)\\b(?i)dollar\\b"), "$")
            .replace(Regex("(?U)\\b(?i)евро\\b"), "€")
            .replace(Regex("(?U)\\b(?i)euros\\b"), "€")
            .replace(Regex("(?U)\\b(?i)процентов\\b"), "%")
            .replace(Regex("(?U)\\b(?i)percent\\b"), "%")

        // Collapse "100 ₽" formatting
        text = text.replace(Regex("(\\d+)\\s+₽"), "$1 ₽")

        return input.copy(processedText = text)
    }
}

/**
 * Punctuation and Capitalization Restoration Plugin.
 * Capitalizes sentences and inserts terminal punctuation.
 */
class PunctuationCapitalizationPlugin : AudioTextPlugin {
    override val id = "punct_plugin"
    override val name = "Punctuation & Capitalization"
    override val description = "Restores sentence capitalization, commas, and ending periods or question marks"
    override val category = "Styling"
    override var isEnabled = true

    override fun process(input: TranscriptionResult): TranscriptionResult {
        if (!isEnabled) return input
        var text = input.processedText.trim()
        if (text.isEmpty()) return input

        // Sentence start capitalization
        text = text.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

        // Multiple sentences capitalization after period or question mark
        text = text.replace(Regex("([.!?]\\s+)([a-zа-яё])")) { match ->
            match.groupValues[1] + match.groupValues[2].uppercase()
        }

        // Add ending punctuation if absent
        if (!text.endsWith(".") && !text.endsWith("?") && !text.endsWith("!")) {
            val isQuestion = text.startsWith("Как", ignoreCase = true) ||
                    text.startsWith("Что", ignoreCase = true) ||
                    text.startsWith("Где", ignoreCase = true) ||
                    text.startsWith("Когда", ignoreCase = true) ||
                    text.startsWith("Почему", ignoreCase = true) ||
                    text.startsWith("Зачем", ignoreCase = true) ||
                    text.startsWith("Who", ignoreCase = true) ||
                    text.startsWith("What", ignoreCase = true) ||
                    text.startsWith("Where", ignoreCase = true) ||
                    text.startsWith("When", ignoreCase = true) ||
                    text.startsWith("Why", ignoreCase = true) ||
                    text.startsWith("How", ignoreCase = true) ||
                    text.endsWith("ли")
            text += if (isQuestion) "?" else "."
        }

        return input.copy(processedText = text)
    }
}

/**
 * Custom Vocabulary / Regex Replacer Plugin:
 * Allows user-defined dictionary transformations and technical terminology substitution.
 */
class CustomVocabularyPlugin : AudioTextPlugin {
    override val id = "vocab_plugin"
    override val name = "Custom Vocabulary & Regex"
    override val description = "Replaces custom jargon, domain keywords, and regex patterns with preferred terms"
    override val category = "Dictionary"
    override var isEnabled = true

    private val userRules = mutableMapOf<String, String>(
        "шерпа" to "Sherpa-ONNX",
        "шерпа оннкс" to "sherpa-onnx",
        "гига ам" to "GigaAM-v3",
        "паракит" to "Parakeet TDT",
        "фаст конформер" to "FastConformer",
        "сенс войс" to "SenseVoice"
    )

    fun getRules(): Map<String, String> = userRules.toMap()

    fun addRule(target: String, replacement: String) {
        userRules[target.trim().lowercase()] = replacement.trim()
        ExecutionLogger.i("PluginPipeline", "Added vocabulary rule: '$target' -> '$replacement'")
    }

    fun removeRule(target: String) {
        userRules.remove(target.trim().lowercase())
    }

    override fun process(input: TranscriptionResult): TranscriptionResult {
        if (!isEnabled) return input
        var text = input.processedText
        for ((pattern, replacement) in userRules) {
            val regex = Regex("(?U)\\b(?i)${Regex.escape(pattern)}\\b")
            text = text.replace(regex, replacement)
        }
        return input.copy(processedText = text)
    }
}

/**
 * Profanity & Privacy Redaction Plugin:
 * Redacts phone numbers, card patterns, and sensitive keywords.
 */
class ProfanityPrivacyFilterPlugin : AudioTextPlugin {
    override val id = "privacy_plugin"
    override val name = "Privacy & Redaction Filter"
    override val description = "Redacts phone numbers, credit cards, and sensitive strings with asterisks"
    override val category = "Security"
    override var isEnabled = false

    private val phoneRegex = Regex("(\\+?\\d{1,3}[- ]?)?\\(?\\d{3}\\)?[- ]?\\d{3}[- ]?\\d{4}")
    private val cardRegex = Regex("\\b\\d{4}[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{4}\\b")

    override fun process(input: TranscriptionResult): TranscriptionResult {
        if (!isEnabled) return input
        var text = input.processedText
        text = text.replace(phoneRegex, "[PHONE_REDACTED]")
        text = text.replace(cardRegex, "[CARD_REDACTED]")
        return input.copy(processedText = text)
    }
}

/**
 * Clipboard Auto-Copy Plugin:
 * Automatically copies finished transcriptions to Android system clipboard.
 */
class ClipboardAutoCopyPlugin(private val context: Context) : AudioTextPlugin {
    override val id = "clipboard_plugin"
    override val name = "Auto-Copy to Clipboard"
    override val description = "Automatically copies recognized text to system clipboard upon completion"
    override val category = "System"
    override var isEnabled = true

    override fun process(input: TranscriptionResult): TranscriptionResult {
        if (!isEnabled || input.processedText.isBlank()) return input
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText("Transcribed Speech", input.processedText)
            clipboard?.setPrimaryClip(clip)
            ExecutionLogger.i("PluginPipeline", "Transcribed text auto-copied to system clipboard")
        } catch (e: Exception) {
            ExecutionLogger.w("PluginPipeline", "Clipboard copy error: ${e.message}")
        }
        return input
    }
}

/**
 * Master Pipeline Manager.
 */
class PluginPipelineManager(context: Context) {
    val itnPlugin = InverseTextNormalizationPlugin()
    val punctPlugin = PunctuationCapitalizationPlugin()
    val vocabPlugin = CustomVocabularyPlugin()
    val privacyPlugin = ProfanityPrivacyFilterPlugin()
    val clipboardPlugin = ClipboardAutoCopyPlugin(context)

    private val _plugins = MutableStateFlow<List<AudioTextPlugin>>(
        listOf(itnPlugin, punctPlugin, vocabPlugin, privacyPlugin, clipboardPlugin)
    )
    val plugins: StateFlow<List<AudioTextPlugin>> = _plugins.asStateFlow()

    fun togglePlugin(pluginId: String, enabled: Boolean) {
        _plugins.value = _plugins.value.map { plugin ->
            if (plugin.id == pluginId) {
                plugin.isEnabled = enabled
            }
            plugin
        }
        ExecutionLogger.i("PluginPipeline", "Plugin '$pluginId' toggled to $enabled")
    }

    fun executePipeline(rawResult: TranscriptionResult): TranscriptionResult {
        var current = rawResult
        for (plugin in _plugins.value) {
            if (plugin.isEnabled) {
                val start = System.nanoTime()
                current = plugin.process(current)
                val durationMs = (System.nanoTime() - start) / 1_000_000f
                ExecutionLogger.d("PluginPipeline", "Executed '${plugin.name}' in ${String.format("%.2f", durationMs)}ms")
            }
        }
        return current
    }
}
