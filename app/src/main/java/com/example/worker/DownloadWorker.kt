package com.example.worker

import android.Manifest
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.DownloaderApp
import com.example.R
import com.example.data.MediaLibraryRepository
import com.example.data.WebViewCookieExporter
import com.example.engine.EngineManager
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class DownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return createForegroundInfo(0, "Preparing download...")
    }

    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_URL)?.trim()
        if (url.isNullOrEmpty()) {
            return Result.failure(workDataOf(KEY_STATUS to "Missing download URL"))
        }

        val format = inputData.getString(KEY_FORMAT) ?: "mp4"
        val videoSelector = inputData.getString(KEY_VIDEO_SELECTOR) ?: "bestvideo+bestaudio/best"
        val audioBitrate = inputData.getString(KEY_AUDIO_BITRATE) ?: "192K"
        val useAria2c = inputData.getBoolean(KEY_USE_ARIA2C, true)
        val saveLocationUriString = inputData.getString(KEY_SAVE_LOCATION_URI)
        val configuredCookiesPath = inputData.getString(KEY_COOKIES_PATH)

        val tempDir = File(applicationContext.cacheDir, "dl_${id}")
        val processId = id.toString()

        try {
            if (!tempDir.exists()) tempDir.mkdirs()

            trySetForeground(0, "Preparing download engine...")
            setProgress(
                workDataOf(
                    KEY_STATUS to "Preparing download engine...",
                    KEY_PROGRESS to 0f,
                    KEY_SPEED to "",
                    KEY_ETA to ""
                )
            )

            // Ensures native binaries are initialized AND yt-dlp is updated from the old bundled version
            val initResult = EngineManager.ensureInitialized(applicationContext)
            if (initResult.isFailure) {
                val msg = initResult.exceptionOrNull()?.localizedMessage ?: "Engine init failed"
                return Result.failure(workDataOf(KEY_STATUS to "Error: $msg", KEY_PROGRESS to 0f))
            }

            // Resolve cookies: use user-configured cookies.txt if present, or auto-export authenticated
            // YouTube cookies from the built-in Browser WebView if available.
            val effectiveCookiesPath = resolveEffectiveCookies(configuredCookiesPath, tempDir, url)

            var lastSpeed = ""
            var detectedTitle = ""

            val primaryRequest = buildYoutubeDLRequest(
                url = url,
                tempDir = tempDir,
                format = format,
                videoSelector = videoSelector,
                audioBitrate = audioBitrate,
                useAria2c = useAria2c,
                cookiesPath = effectiveCookiesPath,
                extractorArgs = null
            )

            val primaryResult = runCatching {
                executeRequest(
                    request = primaryRequest,
                    processId = processId,
                    onSpeed = { lastSpeed = it },
                    onTitle = { detectedTitle = it },
                    currentSpeed = { lastSpeed },
                    currentTitle = { detectedTitle }
                )
            }

            if (primaryResult.isFailure && !isStopped) {
                val firstError = primaryResult.exceptionOrNull()
                if (firstError is YoutubeDL.CanceledException) throw firstError

                val errMessage = firstError?.message.orEmpty()
                Log.w(TAG, "Primary download attempt failed ($errMessage), attempting fallback strategy")

                // Clean any partial artifacts before fallback
                cleanPartialFiles(tempDir)

                // If the error indicates an outdated extractor ("Precondition check failed" / "HTTP Error 400"),
                // force an engine update before retrying.
                if (errMessage.contains("Precondition check", ignoreCase = true) ||
                    errMessage.contains("HTTP Error 400", ignoreCase = true)
                ) {
                    setProgress(
                        workDataOf(
                            KEY_STATUS to "Updating yt-dlp extractor...",
                            KEY_PROGRESS to 0f
                        )
                    )
                    EngineManager.updateEngine(applicationContext)
                }

                setProgress(
                    workDataOf(
                        KEY_STATUS to "Retrying with alternate client...",
                        KEY_PROGRESS to 0f
                    )
                )

                // Fallback 1: Use multi-client player fallback (tv_embedded, android_vr, ios, mweb)
                // and native HTTP downloader instead of aria2c.
                val fallbackCookies = effectiveCookiesPath ?: WebViewCookieExporter.exportFromWebView(
                    context = applicationContext,
                    targetFile = File(tempDir, "fallback_cookies.txt"),
                    extraUrl = url,
                    requireAuthenticatedForYoutube = false
                )?.first?.absolutePath

                val fallbackRequest1 = buildYoutubeDLRequest(
                    url = url,
                    tempDir = tempDir,
                    format = format,
                    videoSelector = videoSelector,
                    audioBitrate = audioBitrate,
                    useAria2c = false,
                    cookiesPath = fallbackCookies,
                    extractorArgs = "youtube:player_client=tv_embedded,android_vr,ios,mweb"
                )

                val fallback1Result = runCatching {
                    executeRequest(
                        request = fallbackRequest1,
                        processId = "${processId}_fb1",
                        onSpeed = { lastSpeed = it },
                        onTitle = { detectedTitle = it },
                        currentSpeed = { lastSpeed },
                        currentTitle = { detectedTitle }
                    )
                }

                if (fallback1Result.isFailure && !isStopped) {
                    val secondError = fallback1Result.exceptionOrNull()
                    if (secondError is YoutubeDL.CanceledException) throw secondError

                    cleanPartialFiles(tempDir)

                    // Fallback 2: Most permissive format selector + ios/mweb/tv clients
                    val fallbackRequest2 = buildYoutubeDLRequest(
                        url = url,
                        tempDir = tempDir,
                        format = format,
                        videoSelector = "best[ext=mp4]/best",
                        audioBitrate = audioBitrate,
                        useAria2c = false,
                        cookiesPath = fallbackCookies,
                        extractorArgs = "youtube:player_client=ios,mweb,tv"
                    )

                    executeRequest(
                        request = fallbackRequest2,
                        processId = "${processId}_fb2",
                        onSpeed = { lastSpeed = it },
                        onTitle = { detectedTitle = it },
                        currentSpeed = { lastSpeed },
                        currentTitle = { detectedTitle }
                    )
                }
            }

            if (isStopped) {
                return Result.failure(workDataOf(KEY_STATUS to "Cancelled", KEY_PROGRESS to 0f))
            }

            setProgress(
                workDataOf(
                    KEY_PROGRESS to 1f,
                    KEY_STATUS to "Saving to folder...",
                    KEY_SPEED to "",
                    KEY_ETA to "",
                    KEY_FILE_NAME to detectedTitle
                )
            )
            updateNotification(100, "Saving to folder...")

            val mediaFiles = tempDir.listFiles()?.filter { file ->
                file.isFile &&
                    !file.name.endsWith(".part") &&
                    !file.name.endsWith(".ytdl") &&
                    !file.name.endsWith(".tmp") &&
                    !file.name.endsWith("cookies.txt") &&
                    file.length() > 0L
            }.orEmpty()

            if (mediaFiles.isEmpty()) {
                return Result.failure(
                    workDataOf(KEY_STATUS to "No output media file produced", KEY_PROGRESS to 0f)
                )
            }

            val savedNames = saveOutputFiles(mediaFiles, saveLocationUriString)
            if (savedNames.isEmpty()) {
                return Result.failure(
                    workDataOf(KEY_STATUS to "Failed to save file to destination folder", KEY_PROGRESS to 0f)
                )
            }

            val finalName = savedNames.first()
            postCompletionNotification(finalName, success = true)

            return Result.success(
                workDataOf(
                    KEY_STATUS to "Saved: $finalName",
                    KEY_PROGRESS to 1f,
                    KEY_FILE_NAME to finalName
                )
            )
        } catch (e: YoutubeDL.CanceledException) {
            Log.i(TAG, "Download cancelled: $processId")
            return Result.failure(workDataOf(KEY_STATUS to "Cancelled", KEY_PROGRESS to 0f))
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            val cleanError = formatReadableError(e.message ?: e.localizedMessage)
            postCompletionNotification(cleanError, success = false)
            return Result.failure(workDataOf(KEY_STATUS to cleanError, KEY_PROGRESS to 0f))
        } finally {
            runCatching { tempDir.deleteRecursively() }
        }
    }

    private fun resolveEffectiveCookies(
        configuredCookiesPath: String?,
        tempDir: File,
        url: String
    ): String? {
        if (!configuredCookiesPath.isNullOrBlank() && File(configuredCookiesPath).exists()) {
            return configuredCookiesPath
        }
        val autoCookiesFile = File(tempDir, "auto_webview_cookies.txt")
        return WebViewCookieExporter.exportFromWebView(
            context = applicationContext,
            targetFile = autoCookiesFile,
            extraUrl = url,
            requireAuthenticatedForYoutube = true
        )?.first?.absolutePath
    }

    private fun buildYoutubeDLRequest(
        url: String,
        tempDir: File,
        format: String,
        videoSelector: String,
        audioBitrate: String,
        useAria2c: Boolean,
        cookiesPath: String?,
        extractorArgs: String?
    ): YoutubeDLRequest {
        return YoutubeDLRequest(url).apply {
            val timestamp = System.currentTimeMillis()
            addOption("--output", "${tempDir.absolutePath}/%(title).80s_$timestamp.%(ext)s")
            addOption("--no-playlist")
            addOption("--no-mtime")
            addOption("--no-check-certificates")
            addOption("--geo-bypass")

            if (useAria2c) {
                addOption("--downloader", "aria2c")
                addOption("--downloader-args", "aria2c:-x 4 -s 4 -k 1M")
            }
            if (!cookiesPath.isNullOrBlank() && File(cookiesPath).exists()) {
                addOption("--cookies", cookiesPath)
            }
            if (!extractorArgs.isNullOrBlank()) {
                addOption("--extractor-args", extractorArgs)
            }

            if (format == "mp3") {
                addOption("--extract-audio")
                addOption("--audio-format", "mp3")
                addOption("--audio-quality", audioBitrate)
            } else {
                addOption("-f", videoSelector)
                addOption("--merge-output-format", "mp4")
            }
        }
    }

    private suspend fun executeRequest(
        request: YoutubeDLRequest,
        processId: String,
        onSpeed: (String) -> Unit,
        onTitle: (String) -> Unit,
        currentSpeed: () -> String,
        currentTitle: () -> String
    ) = withContext(Dispatchers.IO) {
        var lastUpdateTime = 0L
        suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation {
                runCatching { YoutubeDL.getInstance().destroyProcessById(processId) }
            }
            try {
                YoutubeDL.getInstance().execute(request, processId) { progress, etaInSeconds, line ->
                    if (isStopped) {
                        runCatching { YoutubeDL.getInstance().destroyProcessById(processId) }
                        return@execute
                    }

                    MediaLibraryRepository.extractSpeedFromLine(line)?.let(onSpeed)
                    if (currentTitle().isEmpty()) {
                        extractTitleHintFromLine(line)?.let(onTitle)
                    }

                    val now = System.currentTimeMillis()
                    if (now - lastUpdateTime > 600L || progress >= 100f) {
                        lastUpdateTime = now
                        val clamped = (progress / 100f).coerceIn(0f, 1f)
                        val etaFormatted = MediaLibraryRepository.formatEta(etaInSeconds)
                        val statusMsg = when {
                            progress < 0f -> "Connecting..."
                            progress >= 99.5f -> "Processing media..."
                            else -> "Downloading ${progress.toInt()}%"
                        }
                        setProgressAsync(
                            workDataOf(
                                KEY_PROGRESS to clamped,
                                KEY_STATUS to statusMsg,
                                KEY_SPEED to currentSpeed(),
                                KEY_ETA to etaFormatted,
                                KEY_FILE_NAME to currentTitle()
                            )
                        )
                        updateNotification(progress.toInt().coerceIn(0, 100), statusMsg)
                    }
                }
                if (continuation.isActive) continuation.resume(Unit)
            } catch (e: Exception) {
                if (continuation.isActive) continuation.resumeWithException(e)
            }
        }
    }

    private fun cleanPartialFiles(tempDir: File) {
        tempDir.listFiles()?.forEach { file ->
            if (!file.name.endsWith("cookies.txt")) {
                runCatching { file.delete() }
            }
        }
    }

    private fun formatReadableError(raw: String?): String {
        if (raw.isNullOrBlank()) return "Download failed"
        if (raw.contains("Sign in to confirm", ignoreCase = true) ||
            raw.contains("not a bot", ignoreCase = true)
        ) {
            return "YouTube requires sign-in on this IP. Open Browser tab → Sign in to YouTube → Tap 'Sync Cookies'."
        }
        val errorLine = raw.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("ERROR:", ignoreCase = true) }
            ?.removePrefix("ERROR:")
            ?.trim()
        if (!errorLine.isNullOrBlank()) {
            return errorLine.take(160)
        }
        return raw.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() && !it.startsWith("WARNING:", ignoreCase = true) }
            ?.take(160)
            ?: "Download failed"
    }

    private suspend fun saveOutputFiles(
        files: List<File>,
        saveLocationUriString: String?
    ): List<String> = withContext(Dispatchers.IO) {
        val savedNames = mutableListOf<String>()

        if (saveLocationUriString != null) {
            val treeUri = Uri.parse(saveLocationUriString)
            val docUri = DocumentsContract.buildDocumentUriUsingTree(
                treeUri,
                DocumentsContract.getTreeDocumentId(treeUri)
            )
            for (file in files) {
                val safeName = MediaLibraryRepository.sanitizeFilename(file.name)
                val mimeType = resolveMimeType(safeName)
                runCatching {
                    val newFileUri = DocumentsContract.createDocument(
                        applicationContext.contentResolver,
                        docUri,
                        mimeType,
                        safeName
                    ) ?: error("Could not create document in selected folder")

                    applicationContext.contentResolver.openOutputStream(newFileUri)?.use { out ->
                        file.inputStream().use { input -> input.copyTo(out) }
                    } ?: error("Could not open output stream")

                    savedNames.add(safeName)
                }.onFailure {
                    Log.e(TAG, "Failed to save $safeName to SAF", it)
                }
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            for (file in files) {
                val safeName = MediaLibraryRepository.sanitizeFilename(file.name)
                val mimeType = resolveMimeType(safeName)
                runCatching {
                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, safeName)
                        put(MediaStore.Downloads.MIME_TYPE, mimeType)
                        put(
                            MediaStore.Downloads.RELATIVE_PATH,
                            "${Environment.DIRECTORY_DOWNLOADS}/MediaDownloader"
                        )
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }
                    val uri = applicationContext.contentResolver.insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        values
                    ) ?: error("MediaStore insert returned null")

                    applicationContext.contentResolver.openOutputStream(uri)?.use { out ->
                        file.inputStream().use { input -> input.copyTo(out) }
                    } ?: error("MediaStore openOutputStream failed")

                    val doneValues = ContentValues().apply {
                        put(MediaStore.Downloads.IS_PENDING, 0)
                    }
                    applicationContext.contentResolver.update(uri, doneValues, null, null)
                    savedNames.add(safeName)
                }.onFailure {
                    Log.e(TAG, "Failed to save $safeName via MediaStore", it)
                }
            }
        } else {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val destFolder = File(downloadDir, "MediaDownloader")
            if (!destFolder.exists()) destFolder.mkdirs()

            for (file in files) {
                val safeName = MediaLibraryRepository.sanitizeFilename(file.name)
                runCatching {
                    val destFile = File(destFolder, safeName)
                    file.inputStream().use { input ->
                        destFile.outputStream().use { out -> input.copyTo(out) }
                    }
                    savedNames.add(safeName)
                }.onFailure {
                    Log.e(TAG, "Failed to copy $safeName to Downloads", it)
                }
            }
        }

        savedNames
    }

    private fun extractTitleHintFromLine(line: String): String? {
        val marker = "Destination: "
        val idx = line.indexOf(marker)
        if (idx != -1) {
            val path = line.substring(idx + marker.length).trim()
            val fileName = path.substringAfterLast('/')
            if (fileName.isNotBlank()) return fileName
        }
        return null
    }

    private fun resolveMimeType(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.endsWith(".mp3") -> "audio/mpeg"
            lower.endsWith(".m4a") -> "audio/mp4"
            lower.endsWith(".opus") -> "audio/opus"
            lower.endsWith(".webm") -> "video/webm"
            lower.endsWith(".mkv") -> "video/x-matroska"
            else -> "video/mp4"
        }
    }

    private suspend fun trySetForeground(progress: Int, status: String) {
        runCatching {
            setForeground(createForegroundInfo(progress, status))
        }.onFailure {
            Log.w(TAG, "Could not promote worker to foreground: ${it.message}")
        }
    }

    private fun createForegroundInfo(progress: Int, status: String): ForegroundInfo {
        val notification = buildProgressNotification(progress, status)
        val notifId = id.hashCode()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                notifId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(notifId, notification)
        }
    }

    private fun buildProgressNotification(progress: Int, status: String) =
        NotificationCompat.Builder(applicationContext, DownloaderApp.CHANNEL_DOWNLOADS)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(applicationContext.getString(R.string.app_name))
            .setContentText(status)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(100, progress, progress <= 0)
            .build()

    private fun updateNotification(progress: Int, status: String) {
        if (!hasNotificationPermission()) return
        notificationManager.notify(id.hashCode(), buildProgressNotification(progress, status))
    }

    private fun postCompletionNotification(message: String, success: Boolean) {
        if (!hasNotificationPermission()) return
        val notif = NotificationCompat.Builder(applicationContext, DownloaderApp.CHANNEL_DOWNLOADS)
            .setSmallIcon(
                if (success) android.R.drawable.stat_sys_download_done
                else android.R.drawable.stat_notify_error
            )
            .setContentTitle(if (success) "Download Complete" else "Download Failed")
            .setContentText(message)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(id.hashCode() + 1, notif)
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    companion object {
        private const val TAG = "DownloadWorker"
        const val WORK_TAG = "media_download_task"
        const val KEY_URL = "url"
        const val KEY_FORMAT = "format"
        const val KEY_VIDEO_SELECTOR = "videoSelector"
        const val KEY_AUDIO_BITRATE = "audioBitrate"
        const val KEY_USE_ARIA2C = "useAria2c"
        const val KEY_SAVE_LOCATION_URI = "saveLocationUri"
        const val KEY_COOKIES_PATH = "cookiesPath"

        const val KEY_STATUS = "status"
        const val KEY_PROGRESS = "progress"
        const val KEY_SPEED = "speed"
        const val KEY_ETA = "eta"
        const val KEY_FILE_NAME = "fileName"
    }
}
