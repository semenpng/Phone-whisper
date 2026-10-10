package com.example.engine

import android.content.Context
import android.net.Uri
import com.example.model.ModelArchitecture
import com.example.model.ModelPreset
import com.example.model.ModelPresetsCatalog
import com.example.monitor.ExecutionLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

sealed class DownloadState {
    object Idle : DownloadState()
    data class Progress(
        val modelId: String,
        val currentFileName: String,
        val fileIndex: Int,
        val totalFiles: Int,
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val percentage: Int,
        val speedKbps: Float
    ) : DownloadState()
    data class Completed(val modelId: String) : DownloadState()
    data class Error(val modelId: String, val message: String) : DownloadState()
}

data class CachedModelInfo(
    val modelId: String,
    val isComplete: Boolean,
    val totalSizeBytes: Long,
    val filesPresent: List<String>,
    val directoryPath: String
)

class ModelStorageManager(private val context: Context) {

    private val baseModelsDir = File(context.filesDir, "sherpa_models").apply {
        if (!exists()) mkdirs()
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private var activeCall: okhttp3.Call? = null
    @Volatile private var isCancelled = false

    fun getModelDirectory(modelId: String): File {
        val dir = File(baseModelsDir, modelId)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getCachedModelInfo(preset: ModelPreset): CachedModelInfo {
        val dir = getModelDirectory(preset.id)
        val files = dir.listFiles() ?: emptyArray()
        val fileNames = files.map { it.name }
        val requiredNames = preset.files.map { it.fileName }
        val allRequiredPresent = requiredNames.all { req -> fileNames.contains(req) }
        val totalSize = files.sumOf { it.length() }

        return CachedModelInfo(
            modelId = preset.id,
            isComplete = allRequiredPresent && totalSize > 1024,
            totalSizeBytes = totalSize,
            filesPresent = fileNames,
            directoryPath = dir.absolutePath
        )
    }

    fun getCustomModels(): List<CachedModelInfo> {
        val subdirs = baseModelsDir.listFiles()?.filter { it.isDirectory } ?: emptyList()
        val presetIds = ModelPresetsCatalog.presets.map { it.id }.toSet()
        return subdirs.filter { !presetIds.contains(it.name) }.map { dir ->
            val files = dir.listFiles() ?: emptyArray()
            val totalSize = files.sumOf { it.length() }
            CachedModelInfo(
                modelId = dir.name,
                isComplete = files.any { it.name.endsWith(".onnx") },
                totalSizeBytes = totalSize,
                filesPresent = files.map { it.name },
                directoryPath = dir.absolutePath
            )
        }
    }

    suspend fun downloadPreset(preset: ModelPreset): Boolean = withContext(Dispatchers.IO) {
        isCancelled = false
        val modelDir = getModelDirectory(preset.id)
        val totalFiles = preset.files.size
        ExecutionLogger.i("ModelStorage", "Starting download for preset '${preset.name}' (${preset.estimatedSizeMb} MB est)")

        var totalDownloadedAll = 0L

        for ((index, fileReq) in preset.files.withIndex()) {
            if (isCancelled) {
                _downloadState.value = DownloadState.Error(preset.id, "Download cancelled by user")
                return@withContext false
            }

            val targetFile = File(modelDir, fileReq.fileName)
            ExecutionLogger.i("ModelStorage", "Downloading file [${index + 1}/$totalFiles]: ${fileReq.fileName} from ${fileReq.downloadUrl}")

            val request = Request.Builder()
                .url(fileReq.downloadUrl)
                .build()

            val call = okHttpClient.newCall(request)
            activeCall = call

            try {
                val response = call.execute()
                if (!response.isSuccessful) {
                    val errMsg = "HTTP error ${response.code} downloading ${fileReq.fileName}"
                    ExecutionLogger.e("ModelStorage", errMsg)
                    _downloadState.value = DownloadState.Error(preset.id, errMsg)
                    return@withContext false
                }

                val body = response.body ?: throw IllegalStateException("Empty response body")
                val contentLength = body.contentLength()
                val inputStream = body.byteStream()
                val outputStream = FileOutputStream(targetFile)

                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int
                var fileBytesDownloaded = 0L
                var lastTime = System.currentTimeMillis()
                var lastBytes = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    if (isCancelled) {
                        outputStream.close()
                        inputStream.close()
                        targetFile.delete()
                        _downloadState.value = DownloadState.Error(preset.id, "Download cancelled")
                        return@withContext false
                    }
                    outputStream.write(buffer, 0, bytesRead)
                    fileBytesDownloaded += bytesRead
                    totalDownloadedAll += bytesRead

                    val now = System.currentTimeMillis()
                    if (now - lastTime > 400) {
                        val deltaMs = now - lastTime
                        val speedKbps = ((fileBytesDownloaded - lastBytes) / 1024f) / (deltaMs / 1000f)
                        val percent = if (contentLength > 0) ((fileBytesDownloaded * 100) / contentLength).toInt() else 50

                        _downloadState.value = DownloadState.Progress(
                            modelId = preset.id,
                            currentFileName = fileReq.fileName,
                            fileIndex = index + 1,
                            totalFiles = totalFiles,
                            bytesDownloaded = fileBytesDownloaded,
                            totalBytes = contentLength,
                            percentage = percent,
                            speedKbps = speedKbps
                        )
                        lastTime = now
                        lastBytes = fileBytesDownloaded
                    }
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()
                ExecutionLogger.i("ModelStorage", "Successfully saved ${fileReq.fileName} (${targetFile.length() / (1024 * 1024)} MB)")
            } catch (e: Exception) {
                if (isCancelled) {
                    ExecutionLogger.w("ModelStorage", "Download of ${fileReq.fileName} was cancelled")
                    _downloadState.value = DownloadState.Idle
                    return@withContext false
                }
                ExecutionLogger.e("ModelStorage", "Download failed for ${fileReq.fileName}: ${e.message}")
                _downloadState.value = DownloadState.Error(preset.id, e.message ?: "Network error")
                return@withContext false
            } finally {
                activeCall = null
            }
        }

        ExecutionLogger.i("ModelStorage", "Model '${preset.name}' successfully cached in storage")
        _downloadState.value = DownloadState.Completed(preset.id)
        true
    }

    fun cancelDownload() {
        isCancelled = true
        activeCall?.cancel()
        _downloadState.value = DownloadState.Idle
        ExecutionLogger.i("ModelStorage", "Active download cancellation signaled")
    }

    suspend fun importLocalFile(modelId: String, fileName: String, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val dir = getModelDirectory(modelId)
            val destFile = File(dir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            ExecutionLogger.i("ModelStorage", "Imported local file $fileName (${destFile.length() / 1024} KB) into $modelId")
            true
        } catch (e: Exception) {
            ExecutionLogger.e("ModelStorage", "Failed to import file $fileName: ${e.message}")
            false
        }
    }

    fun deleteModelCache(modelId: String): Boolean {
        val dir = getModelDirectory(modelId)
        val success = dir.deleteRecursively()
        dir.mkdirs()
        ExecutionLogger.i("ModelStorage", "Evicted cache for model $modelId (result=$success)")
        return success
    }

    fun getUsedStorageMb(): Float {
        val totalBytes = baseModelsDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        return totalBytes / (1024f * 1024f)
    }

    fun resetDownloadState() {
        _downloadState.value = DownloadState.Idle
    }
}
