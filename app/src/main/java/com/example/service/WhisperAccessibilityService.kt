package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.example.R
import com.example.engine.AudioRecorderManager
import com.example.engine.ModelStorageManager
import com.example.engine.SpeechRecognitionEngine
import com.example.monitor.ExecutionLogger
import com.example.plugin.PluginPipelineManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import kotlin.math.abs

/**
 * System-wide Phone Whisper Floating Dictation Accessibility Service.
 * Renders a floating, draggable push-to-talk button on top of any app.
 * On audio capture, passes PCM audio through the active Sherpa-ONNX model
 * (GigaAM-v3, Parakeet TDT, SenseVoice, FastConformer, Whisper) and plugin pipeline,
 * then auto-injects transcribed text directly into the focused EditText.
 */
class WhisperAccessibilityService : AccessibilityService() {

    companion object {
        var instance: WhisperAccessibilityService? = null
        private const val TAG = "PhoneWhisperService"
        private const val SAMPLE_RATE = 16000
        private const val BTN_DP = 48
        private const val PAD_DP = 10
        private const val MARGIN_DP = 10
        private const val TAP_THRESHOLD_DP = 10
        private const val FEEDBACK_OFFSET_DP = 64

        private const val COLOR_IDLE = 0xDD161B22.toInt()
        private const val COLOR_RECORDING = 0xDDF85149.toInt()
        private const val COLOR_BUSY = 0xDD8B5CF6.toInt()
        private const val COLOR_FEEDBACK_BG = 0xEE0D1117.toInt()
        private const val COLOR_RING = 0xFF00D2FF.toInt()
    }

    private enum class State { IDLE, RECORDING, TRANSCRIBING }
    private var state = State.IDLE

    private var overlayView: FrameLayout? = null
    private var button: ImageView? = null
    private var spinner: ProgressBar? = null
    private var feedbackView: TextView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var feedbackLayoutParams: WindowManager.LayoutParams? = null

    private var audioRecord: AudioRecord? = null
    private var pcmStream: ByteArrayOutputStream? = null
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Default)

    private lateinit var storageManager: ModelStorageManager
    private lateinit var pipelineManager: PluginPipelineManager
    private lateinit var speechEngine: SpeechRecognitionEngine

    private val dp get() = resources.displayMetrics.density
    private val screenW get() = resources.displayMetrics.widthPixels
    private val screenH get() = resources.displayMetrics.heightPixels

    override fun onServiceConnected() {
        instance = this
        storageManager = ModelStorageManager(this)
        pipelineManager = PluginPipelineManager(this)
        speechEngine = SpeechRecognitionEngine(this, storageManager, pipelineManager)

        ExecutionLogger.i(TAG, "Phone Whisper Accessibility Service connected. Displaying floating overlay.")
        showOverlay()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        instance = null
        removeOverlay()
        super.onDestroy()
    }

    // --- Floating Overlay Management ---

    private fun showOverlay() {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val buttonSize = (BTN_DP * dp).toInt()
        val pad = (PAD_DP * dp).toInt()
        val totalSize = buttonSize + pad * 2

        val lp = WindowManager.LayoutParams(
            totalSize,
            totalSize,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenW - totalSize - (MARGIN_DP * dp).toInt()
            y = screenH / 2 - totalSize / 2
        }
        layoutParams = lp

        // Feedback Pill View
        val fbLp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = lp.x
            y = lp.y - (FEEDBACK_OFFSET_DP * dp).toInt()
        }
        feedbackLayoutParams = fbLp

        val fbView = TextView(this).apply {
            textSize = 12f
            setTextColor(0xFFF0F6FC.toInt())
            setPadding((12 * dp).toInt(), (6 * dp).toInt(), (12 * dp).toInt(), (6 * dp).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 16 * dp
                setColor(COLOR_FEEDBACK_BG)
                setStroke((1 * dp).toInt(), 0xFF30363D.toInt())
            }
            visibility = View.GONE
            alpha = 0f
        }
        feedbackView = fbView

        // Floating Action Button with Cyan Ring
        val btn = ImageView(this).apply {
            setImageResource(R.drawable.ic_mic)
            imageTintList = ColorStateList.valueOf(0xFFFFFFFF.toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(COLOR_IDLE)
                setStroke((2 * dp).toInt(), COLOR_RING)
            }
            elevation = 6 * dp
        }
        button = btn

        val spin = ProgressBar(this).apply {
            isIndeterminate = true
            indeterminateTintList = ColorStateList.valueOf(COLOR_RING)
            visibility = View.GONE
        }
        spinner = spin

        val root = FrameLayout(this).apply {
            setPadding(pad, pad, pad, pad)
            clipChildren = false
            clipToPadding = false
            addView(btn, FrameLayout.LayoutParams(buttonSize, buttonSize, Gravity.CENTER))
            addView(spin, FrameLayout.LayoutParams((buttonSize * 0.7f).toInt(), (buttonSize * 0.7f).toInt(), Gravity.CENTER))
        }
        overlayView = root

        setupTouchListener(root, wm, lp, fbLp)

        try {
            wm.addView(fbView, fbLp)
            wm.addView(root, lp)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding overlay view: ${e.message}")
        }
    }

    private fun removeOverlay() {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        overlayView?.let {
            try { wm.removeView(it) } catch (e: Exception) {}
            overlayView = null
        }
        feedbackView?.let {
            try { wm.removeView(it) } catch (e: Exception) {}
            feedbackView = null
        }
    }

    private fun setupTouchListener(
        view: View,
        wm: WindowManager,
        lp: WindowManager.LayoutParams,
        fbLp: WindowManager.LayoutParams
    ) {
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        var isDrag = false

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = lp.x
                    startY = lp.y
                    touchX = event.rawX
                    touchY = event.rawY
                    isDrag = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchX).toInt()
                    val dy = (event.rawY - touchY).toInt()
                    if (abs(dx) > TAP_THRESHOLD_DP * dp || abs(dy) > TAP_THRESHOLD_DP * dp) {
                        isDrag = true
                    }
                    if (isDrag) {
                        lp.x = startX + dx
                        lp.y = startY + dy
                        fbLp.x = lp.x
                        fbLp.y = lp.y - (FEEDBACK_OFFSET_DP * dp).toInt()
                        try {
                            wm.updateViewLayout(view, lp)
                            if (feedbackView?.visibility == View.VISIBLE) {
                                wm.updateViewLayout(feedbackView, fbLp)
                            }
                        } catch (e: Exception) {}
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDrag) {
                        onButtonClicked()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun onButtonClicked() {
        when (state) {
            State.IDLE -> startRecording()
            State.RECORDING -> stopRecordingAndTranscribe()
            State.TRANSCRIBING -> {} // Busy
        }
    }

    private fun startRecording() {
        val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuf.coerceAtLeast(SAMPLE_RATE / 5 * 2)
            )
            audioRecord?.startRecording()
            state = State.RECORDING
            setAppearance(COLOR_RECORDING)

            pcmStream = ByteArrayOutputStream()
            val stream = pcmStream!!

            scope.launch(Dispatchers.IO) {
                val buf = ByteArray(1024)
                while (state == State.RECORDING) {
                    val n = audioRecord?.read(buf, 0, buf.size) ?: -1
                    if (n > 0) {
                        stream.write(buf, 0, n)
                    }
                }
            }
        } catch (e: Exception) {
            ExecutionLogger.e(TAG, "AudioRecord start failed: ${e.message}")
            state = State.IDLE
            setAppearance(COLOR_IDLE)
        }
    }

    private fun stopRecordingAndTranscribe() {
        state = State.TRANSCRIBING
        setAppearance(COLOR_BUSY)
        setBusy(true)

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {}
        audioRecord = null

        val pcm = pcmStream?.toByteArray() ?: ByteArray(0)
        pcmStream = null

        if (pcm.isEmpty()) {
            reset("No audio recorded")
            return
        }

        scope.launch {
            try {
                val result = speechEngine.transcribeAudio(pcm)
                handler.post {
                    injectText(result.processedText)
                    state = State.IDLE
                    setBusy(false)
                    setAppearance(COLOR_IDLE)
                }
            } catch (e: Exception) {
                handler.post {
                    reset("Transcription error: ${e.message}")
                }
            }
        }
    }

    private fun injectText(text: String) {
        if (text.isBlank()) {
            showFeedback("No speech recognized", 2000)
            return
        }

        // Copy to system clipboard
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("PhoneWhisper", text)
        clipboard.setPrimaryClip(clip)
        showFeedback("Injected: $text", 2500)

        // Find candidate EditText in active foreground application
        val candidates = findInjectionCandidates()
        var injected = false
        try {
            for (candidate in candidates) {
                if (tryInjectIntoNode(candidate, text)) {
                    injected = true
                    ExecutionLogger.i(TAG, "Successfully injected text into active field (${candidate.className})")
                    break
                }
            }
        } finally {
            candidates.forEach { it.recycle() }
        }

        if (!injected) {
            ExecutionLogger.i(TAG, "No editable field targeted. Text retained in clipboard.")
        }
    }

    private fun findInjectionCandidates(): List<AccessibilityNodeInfo> {
        val candidates = mutableListOf<AccessibilityNodeInfo>()
        rootInActiveWindow?.let { root ->
            collectCandidates(root, candidates)
            root.recycle()
        }
        windows?.filter { it.isActive || it.isFocused }?.forEach { window ->
            val root = window.root ?: return@forEach
            collectCandidates(root, candidates)
            root.recycle()
        }
        return candidates
    }

    private fun collectCandidates(node: AccessibilityNodeInfo, out: MutableList<AccessibilityNodeInfo>) {
        if (node.isFocused || node.isEditable || node.className?.toString()?.contains("EditText") == true) {
            out.add(AccessibilityNodeInfo.obtain(node))
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try {
                collectCandidates(child, out)
            } finally {
                child.recycle()
            }
        }
    }

    private fun tryInjectIntoNode(node: AccessibilityNodeInfo, text: String): Boolean {
        node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)

        // Try paste
        if (node.performAction(AccessibilityNodeInfo.ACTION_PASTE)) return true

        // Try direct SET_TEXT
        if (node.isEditable || node.className?.toString()?.contains("EditText") == true) {
            val current = node.text?.toString().orEmpty()
            val start = if (node.textSelectionStart >= 0) node.textSelectionStart else current.length
            val end = if (node.textSelectionEnd >= 0) node.textSelectionEnd else start
            val updated = current.replaceRange(minOf(start, end), maxOf(start, end), text)
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, updated)
            }
            return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        }
        return false
    }

    private fun showFeedback(msg: String, durationMs: Long) {
        val fb = feedbackView ?: return
        fb.text = msg
        fb.visibility = View.VISIBLE
        fb.animate().alpha(1f).setDuration(150).start()
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            fb.animate().alpha(0f).setDuration(200).withEndAction {
                fb.visibility = View.GONE
            }.start()
        }, durationMs)
    }

    private fun setAppearance(color: Int) {
        val bg = button?.background as? GradientDrawable
        bg?.setColor(color)
    }

    private fun setBusy(busy: Boolean) {
        spinner?.visibility = if (busy) View.VISIBLE else View.GONE
        button?.visibility = if (busy) View.INVISIBLE else View.VISIBLE
    }

    private fun reset(msg: String) {
        showFeedback(msg, 2000)
        state = State.IDLE
        setBusy(false)
        setAppearance(COLOR_IDLE)
    }
}
