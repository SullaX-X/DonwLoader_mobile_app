package com.example.ui

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.data.ActivityLogEntry
import com.example.data.ActivityType
import com.example.data.AppSettings
import com.example.data.AppTab
import com.example.data.AudioQuality
import com.example.data.DownloadFormat
import com.example.data.DownloadMode
import com.example.data.DownloadTaskItem
import com.example.data.MediaFileItem
import com.example.data.MediaLibraryRepository
import com.example.data.PreferencesRepository
import com.example.data.TaskState
import com.example.data.VideoQuality
import com.example.engine.EngineManager
import com.example.engine.EngineStatus
import com.example.worker.DownloadWorker
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

enum class LibraryFilter(val label: String) {
    ALL("All"),
    VIDEO("Video"),
    AUDIO("Audio")
}

private data class EnqueuedMeta(
    val url: String,
    val displayName: String,
    val format: DownloadFormat,
    val qualityLabel: String,
    val createdAt: Long
)

class DownloaderViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepo = PreferencesRepository(application)
    private val workManager = WorkManager.getInstance(application)

    val settings: StateFlow<AppSettings> = prefsRepo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val engineStatus: StateFlow<EngineStatus> = EngineManager.status

    private val _currentTab = MutableStateFlow(AppTab.HOME)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _downloadMode = MutableStateFlow(DownloadMode.SINGLE)
    val downloadMode: StateFlow<DownloadMode> = _downloadMode.asStateFlow()

    private val _selectedFormat = MutableStateFlow(DownloadFormat.MP4)
    val selectedFormat: StateFlow<DownloadFormat> = _selectedFormat.asStateFlow()

    private val _batchUrls = MutableStateFlow<List<String>>(emptyList())
    val batchUrls: StateFlow<List<String>> = _batchUrls.asStateFlow()

    private val _batchFileName = MutableStateFlow<String?>(null)
    val batchFileName: StateFlow<String?> = _batchFileName.asStateFlow()

    private val _tasks = MutableStateFlow<List<DownloadTaskItem>>(emptyList())
    val tasks: StateFlow<List<DownloadTaskItem>> = _tasks.asStateFlow()

    private val _libraryFiles = MutableStateFlow<List<MediaFileItem>>(emptyList())
    private val _isLoadingLibrary = MutableStateFlow(false)
    val isLoadingLibrary: StateFlow<Boolean> = _isLoadingLibrary.asStateFlow()

    private val _librarySearchQuery = MutableStateFlow("")
    val librarySearchQuery: StateFlow<String> = _librarySearchQuery.asStateFlow()

    private val _libraryFilter = MutableStateFlow(LibraryFilter.ALL)
    val libraryFilter: StateFlow<LibraryFilter> = _libraryFilter.asStateFlow()

    val filteredLibraryFiles: StateFlow<List<MediaFileItem>> = combine(
        _libraryFiles,
        _librarySearchQuery,
        _libraryFilter
    ) { files, query, filter ->
        files.filter { item ->
            val matchesFilter = when (filter) {
                LibraryFilter.ALL -> true
                LibraryFilter.VIDEO -> item.isVideo
                LibraryFilter.AUDIO -> !item.isVideo
            }
            val matchesQuery = query.isBlank() || item.name.contains(query.trim(), ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activityLogs = MutableStateFlow<List<ActivityLogEntry>>(emptyList())
    val activityLogs: StateFlow<List<ActivityLogEntry>> = _activityLogs.asStateFlow()

    private val _browserUrl = MutableStateFlow("https://m.youtube.com")
    val browserUrl: StateFlow<String> = _browserUrl.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    private val taskMetaMap = ConcurrentHashMap<UUID, EnqueuedMeta>()
    private val reportedTerminalIds = HashSet<UUID>()
    private val dismissedIds = MutableStateFlow<Set<UUID>>(emptySet())

    init {
        viewModelScope.launch {
            settings.map { it.defaultFormat }.distinctUntilChanged().collect { fmt ->
                _selectedFormat.value = fmt
            }
        }

        viewModelScope.launch {
            settings.map { it.saveLocationUri }.distinctUntilChanged().collect {
                refreshLibrary()
            }
        }

        observeWorkQueue()
    }

    private fun observeWorkQueue() {
        viewModelScope.launch {
            combine(
                workManager.getWorkInfosByTagFlow(DownloadWorker.WORK_TAG),
                dismissedIds
            ) { workInfos, dismissed ->
                val mapped = workInfos
                    .filter { it.id !in dismissed }
                    .mapNotNull { info -> mapWorkInfo(info) }
                    .sortedWith(
                        compareBy<DownloadTaskItem> {
                            when (it.state) {
                                TaskState.RUNNING -> 0
                                TaskState.QUEUED -> 1
                                TaskState.FAILED -> 2
                                TaskState.SUCCEEDED -> 3
                                TaskState.CANCELLED -> 4
                            }
                        }.thenByDescending { it.createdAt }
                    )
                mapped
            }.collect { mappedTasks ->
                _tasks.value = mappedTasks
                var shouldRefreshLibrary = false

                mappedTasks.forEach { task ->
                    if ((task.state == TaskState.SUCCEEDED || task.state == TaskState.FAILED) &&
                        reportedTerminalIds.add(task.id)
                    ) {
                        if (task.state == TaskState.SUCCEEDED) {
                            shouldRefreshLibrary = true
                            addLog(
                                title = "Download Complete",
                                subtitle = task.displayName,
                                type = ActivityType.SUCCESS,
                                url = task.url.takeIf { it.isNotBlank() }
                            )
                        } else {
                            addLog(
                                title = "Download Failed",
                                subtitle = "${task.displayName} • ${task.statusText}",
                                type = ActivityType.ERROR,
                                url = task.url.takeIf { it.isNotBlank() }
                            )
                        }
                    }
                }

                if (shouldRefreshLibrary) {
                    refreshLibrary()
                }
            }
        }
    }

    private fun mapWorkInfo(info: WorkInfo): DownloadTaskItem? {
        val meta = taskMetaMap[info.id]
        val state = when (info.state) {
            WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> TaskState.QUEUED
            WorkInfo.State.RUNNING -> TaskState.RUNNING
            WorkInfo.State.SUCCEEDED -> TaskState.SUCCEEDED
            WorkInfo.State.FAILED -> TaskState.FAILED
            WorkInfo.State.CANCELLED -> TaskState.CANCELLED
        }

        // Skip stale terminal records from previous app launches unless tracked in this session
        if (meta == null && state in setOf(TaskState.SUCCEEDED, TaskState.FAILED, TaskState.CANCELLED)) {
            return null
        }

        val formatFromTag = info.tags.firstOrNull { it.startsWith("meta_fmt:") }
            ?.removePrefix("meta_fmt:")
            ?.let { ext -> DownloadFormat.entries.firstOrNull { it.ext == ext } }
        val qualityFromTag = info.tags.firstOrNull { it.startsWith("meta_qual:") }
            ?.removePrefix("meta_qual:")

        val format = meta?.format ?: formatFromTag ?: DownloadFormat.MP4
        val qualityLabel = meta?.qualityLabel ?: qualityFromTag ?: ""

        val progressData = if (state == TaskState.SUCCEEDED) {
            info.outputData
        } else {
            info.progress
        }

        val progress = when (state) {
            TaskState.SUCCEEDED -> 1f
            TaskState.FAILED, TaskState.CANCELLED -> 0f
            else -> progressData.getFloat(DownloadWorker.KEY_PROGRESS, 0f).coerceIn(0f, 1f)
        }

        val status = when (state) {
            TaskState.QUEUED -> "Queued..."
            TaskState.RUNNING -> progressData.getString(DownloadWorker.KEY_STATUS) ?: "Downloading..."
            TaskState.SUCCEEDED -> info.outputData.getString(DownloadWorker.KEY_STATUS) ?: "Download Complete"
            TaskState.FAILED -> info.outputData.getString(DownloadWorker.KEY_STATUS) ?: "Download Failed"
            TaskState.CANCELLED -> "Cancelled"
        }

        val fileName = info.outputData.getString(DownloadWorker.KEY_FILE_NAME)
            ?.takeIf { it.isNotBlank() }
            ?: info.progress.getString(DownloadWorker.KEY_FILE_NAME)?.takeIf { it.isNotBlank() }
            ?: meta?.displayName
            ?: "Media.${format.ext}"

        val speed = if (state == TaskState.RUNNING) {
            info.progress.getString(DownloadWorker.KEY_SPEED).orEmpty()
        } else ""

        val eta = if (state == TaskState.RUNNING) {
            info.progress.getString(DownloadWorker.KEY_ETA).orEmpty()
        } else ""

        return DownloadTaskItem(
            id = info.id,
            url = meta?.url.orEmpty(),
            displayName = fileName,
            format = format,
            qualityLabel = qualityLabel,
            progress = progress,
            statusText = status,
            speedText = speed,
            etaText = eta,
            state = state,
            createdAt = meta?.createdAt ?: 0L
        )
    }

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
        if (tab == AppTab.LIBRARY) {
            refreshLibrary()
        }
    }

    fun updateUrlInput(value: String) {
        _urlInput.value = value
    }

    fun selectDownloadMode(mode: DownloadMode) {
        _downloadMode.value = mode
    }

    fun selectFormat(format: DownloadFormat) {
        _selectedFormat.value = format
    }

    fun setSettingsOpen(open: Boolean) {
        _isSettingsOpen.value = open
    }

    fun updateBrowserUrl(url: String) {
        _browserUrl.value = url
    }

    fun updateLibrarySearch(query: String) {
        _librarySearchQuery.value = query
    }

    fun updateLibraryFilter(filter: LibraryFilter) {
        _libraryFilter.value = filter
    }

    fun handleSharedText(text: String) {
        val urls = MediaLibraryRepository.extractUrlsFromText(text)
        if (urls.isNotEmpty()) {
            _urlInput.value = urls.first()
            _downloadMode.value = DownloadMode.SINGLE
            _currentTab.value = AppTab.HOME
            emitMessage("Link loaded from Share")
        } else if (text.isNotBlank()) {
            _urlInput.value = text.trim()
            _downloadMode.value = DownloadMode.SINGLE
            _currentTab.value = AppTab.HOME
        }
    }

    fun importBatchTxt(uri: Uri) {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val urls = runCatching {
                MediaLibraryRepository.readUrlsFromTxtUri(ctx, uri)
            }.getOrDefault(emptyList())

            val name = runCatching {
                ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (idx != -1) cursor.getString(idx) else null
                        } else null
                    }
            }.getOrNull() ?: "links.txt"

            _batchUrls.value = urls
            _batchFileName.value = name

            if (urls.isEmpty()) {
                emitMessage("No valid http/https links found in $name")
            } else {
                emitMessage("Loaded ${urls.size} link(s) from $name")
                addLog(
                    title = "Batch File Loaded",
                    subtitle = "${urls.size} link(s) ready from $name",
                    type = ActivityType.INFO
                )
            }
        }
    }

    fun clearBatchSelection() {
        _batchUrls.value = emptyList()
        _batchFileName.value = null
    }

    fun startSingleDownload() {
        val raw = _urlInput.value.trim()
        val urls = MediaLibraryRepository.extractUrlsFromText(raw)
        if (urls.isEmpty()) {
            emitMessage("Please enter a valid http/https media link")
            return
        }
        if (urls.size == 1) {
            enqueueDownload(urls.first(), _selectedFormat.value)
            emitMessage("Download added to queue")
        } else {
            enqueueBatch(urls, _selectedFormat.value)
            emitMessage("Added ${urls.size} links to queue")
        }
    }

    fun startBatchDownload() {
        val urls = _batchUrls.value
        if (urls.isEmpty()) {
            emitMessage("Select a .txt file with links first")
            return
        }
        enqueueBatch(urls, _selectedFormat.value)
        emitMessage("Queued ${urls.size} downloads")
    }

    fun downloadFromBrowser(url: String, format: DownloadFormat) {
        val extracted = MediaLibraryRepository.extractUrlsFromText(url).firstOrNull()
        if (extracted == null) {
            emitMessage("Open a valid page before downloading")
            return
        }
        _urlInput.value = extracted
        _selectedFormat.value = format
        enqueueDownload(extracted, format)
        emitMessage("Queued ${format.label} download")
    }

    fun retryUrl(url: String) {
        if (url.isBlank()) return
        enqueueDownload(url, _selectedFormat.value)
        emitMessage("Retrying download")
    }

    private fun enqueueBatch(urls: List<String>, format: DownloadFormat) {
        val currentSettings = settings.value
        val requests = urls.map { url -> buildWorkRequest(url, format, currentSettings) }
        if (requests.isEmpty()) return

        var continuation = workManager.beginUniqueWork(
            "batch_${System.currentTimeMillis()}",
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            requests.first()
        )
        requests.drop(1).forEach { req ->
            continuation = continuation.then(req)
        }
        continuation.enqueue()
    }

    private fun enqueueDownload(url: String, format: DownloadFormat) {
        val req = buildWorkRequest(url, format, settings.value)
        workManager.enqueue(req)
    }

    private fun buildWorkRequest(
        url: String,
        format: DownloadFormat,
        currentSettings: AppSettings
    ): androidx.work.OneTimeWorkRequest {
        val qualityLabel = if (format == DownloadFormat.MP4) {
            currentSettings.videoQuality.label
        } else {
            currentSettings.audioQuality.label
        }

        val req = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .addTag(DownloadWorker.WORK_TAG)
            .addTag("meta_fmt:${format.ext}")
            .addTag("meta_qual:$qualityLabel")
            .setInputData(
                workDataOf(
                    DownloadWorker.KEY_URL to url,
                    DownloadWorker.KEY_FORMAT to format.ext,
                    DownloadWorker.KEY_VIDEO_SELECTOR to currentSettings.videoQuality.formatSelector,
                    DownloadWorker.KEY_AUDIO_BITRATE to currentSettings.audioQuality.bitrate,
                    DownloadWorker.KEY_USE_ARIA2C to currentSettings.useAria2c,
                    DownloadWorker.KEY_SAVE_LOCATION_URI to currentSettings.saveLocationUri,
                    DownloadWorker.KEY_COOKIES_PATH to currentSettings.cookiesFilePath
                )
            )
            .build()

        taskMetaMap[req.id] = EnqueuedMeta(
            url = url,
            displayName = MediaLibraryRepository.buildDisplayNameFromUrl(url, format),
            format = format,
            qualityLabel = qualityLabel,
            createdAt = System.currentTimeMillis()
        )

        return req
    }

    fun cancelTask(id: UUID) {
        workManager.cancelWorkById(id)
        emitMessage("Download cancelled")
    }

    fun dismissTask(id: UUID) {
        dismissedIds.value = dismissedIds.value + id
    }

    fun clearFinishedTasks() {
        val finishedIds = _tasks.value
            .filter { it.state in setOf(TaskState.SUCCEEDED, TaskState.FAILED, TaskState.CANCELLED) }
            .map { it.id }
            .toSet()
        if (finishedIds.isNotEmpty()) {
            dismissedIds.value = dismissedIds.value + finishedIds
        }
    }

    fun refreshLibrary() {
        viewModelScope.launch {
            _isLoadingLibrary.value = true
            val files = MediaLibraryRepository.scanLibrary(
                getApplication(),
                settings.value.saveLocationUri
            )
            _libraryFiles.value = files
            _isLoadingLibrary.value = false
        }
    }

    fun openMediaItem(item: MediaFileItem) {
        val ok = MediaLibraryRepository.openMediaFile(getApplication(), item)
        if (!ok) {
            emitMessage("No media player found for ${item.name}")
        }
    }

    fun shareMediaItem(item: MediaFileItem) {
        val ok = MediaLibraryRepository.shareMediaFile(getApplication(), item)
        if (!ok) {
            emitMessage("Unable to share ${item.name}")
        }
    }

    fun deleteMediaItem(item: MediaFileItem) {
        viewModelScope.launch {
            val isSaf = settings.value.saveLocationUri != null
            val ok = MediaLibraryRepository.deleteMediaFile(getApplication(), item, isSaf)
            if (ok) {
                emitMessage("Deleted ${item.name}")
                refreshLibrary()
            } else {
                emitMessage("Could not delete ${item.name}")
            }
        }
    }

    fun setSaveLocation(uri: Uri) {
        viewModelScope.launch {
            prefsRepo.setSaveLocation(uri)
            emitMessage("Save folder updated")
        }
    }

    fun resetSaveLocation() {
        viewModelScope.launch {
            prefsRepo.resetSaveLocationToDefault()
            emitMessage("Reset to Downloads/MediaDownloader")
        }
    }

    fun setDefaultFormat(format: DownloadFormat) {
        viewModelScope.launch {
            prefsRepo.setDefaultFormat(format)
        }
    }

    fun setVideoQuality(quality: VideoQuality) {
        viewModelScope.launch {
            prefsRepo.setVideoQuality(quality)
        }
    }

    fun setAudioQuality(quality: AudioQuality) {
        viewModelScope.launch {
            prefsRepo.setAudioQuality(quality)
        }
    }

    fun setUseAria2c(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setUseAria2c(enabled)
        }
    }

    fun importCookies(uri: Uri) {
        viewModelScope.launch {
            val result = prefsRepo.importCookiesFile(uri)
            result.onSuccess { name ->
                emitMessage("Cookies loaded: $name")
                addLog(
                    title = "Cookies Imported",
                    subtitle = "Using $name for authenticated downloads",
                    type = ActivityType.INFO
                )
            }.onFailure { err ->
                emitMessage(err.localizedMessage ?: "Failed to import cookies")
            }
        }
    }

    fun syncBrowserCookies() {
        viewModelScope.launch {
            val result = prefsRepo.syncCookiesFromBrowser(_browserUrl.value)
            result.onSuccess { count ->
                emitMessage("Synced $count cookies from Browser")
                addLog(
                    title = "Browser Cookies Synced",
                    subtitle = "Exported $count cookies from built-in Browser",
                    type = ActivityType.INFO
                )
            }.onFailure { err ->
                emitMessage(err.localizedMessage ?: "Failed to sync browser cookies")
            }
        }
    }

    fun clearCookies() {
        viewModelScope.launch {
            prefsRepo.clearCookiesFile()
            emitMessage("Cookies cleared")
        }
    }

    fun triggerEngineUpdate() {
        viewModelScope.launch {
            val result = EngineManager.updateEngine(getApplication())
            result.onSuccess { msg ->
                emitMessage(msg)
                addLog(
                    title = "Download Engine",
                    subtitle = msg,
                    type = ActivityType.INFO
                )
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Engine update failed"
                emitMessage(msg)
                addLog(
                    title = "Engine Update Failed",
                    subtitle = msg,
                    type = ActivityType.ERROR
                )
            }
        }
    }

    fun clearActivityLogs() {
        _activityLogs.value = emptyList()
    }

    private fun addLog(title: String, subtitle: String, type: ActivityType, url: String? = null) {
        val entry = ActivityLogEntry(
            title = title,
            subtitle = subtitle,
            type = type,
            url = url
        )
        _activityLogs.value = (listOf(entry) + _activityLogs.value).take(50)
    }

    private fun emitMessage(message: String) {
        _snackbarEvents.tryEmit(message)
    }
}
