package com.example.data

import android.net.Uri
import java.util.UUID

enum class AppTab(val label: String) {
    HOME("Download"),
    BROWSER("Browser"),
    LIBRARY("Library"),
    ACTIVITY("Activity")
}

enum class DownloadMode(val label: String) {
    SINGLE("Single Link"),
    BATCH("Batch (.txt)")
}

enum class DownloadFormat(val ext: String, val label: String) {
    MP4("mp4", "MP4 Video"),
    MP3("mp3", "MP3 Audio")
}

enum class VideoQuality(val id: String, val label: String, val formatSelector: String) {
    BEST("best", "Best Quality", "bestvideo+bestaudio/best"),
    Q1080("1080p", "1080p Full HD", "bestvideo[height<=1080]+bestaudio/best[height<=1080]/best"),
    Q720("720p", "720p HD", "bestvideo[height<=720]+bestaudio/best[height<=720]/best"),
    Q480("480p", "480p SD", "bestvideo[height<=480]+bestaudio/best[height<=480]/best")
}

enum class AudioQuality(val bitrate: String, val label: String) {
    K320("320K", "320 kbps"),
    K192("192K", "192 kbps"),
    K128("128K", "128 kbps")
}

enum class TaskState {
    QUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELLED
}

data class DownloadTaskItem(
    val id: UUID,
    val url: String,
    val displayName: String,
    val format: DownloadFormat,
    val qualityLabel: String,
    val progress: Float = 0f,
    val statusText: String = "Queued...",
    val speedText: String = "",
    val etaText: String = "",
    val state: TaskState = TaskState.QUEUED,
    val createdAt: Long = System.currentTimeMillis()
)

data class MediaFileItem(
    val id: String,
    val name: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val isVideo: Boolean,
    val mimeType: String,
    val contentUri: Uri,
    val filePath: String? = null
)

enum class ActivityType {
    SUCCESS,
    ERROR,
    INFO
}

data class ActivityLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val subtitle: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: ActivityType,
    val url: String? = null
)

data class AppSettings(
    val saveLocationUri: String? = null,
    val saveLocationName: String = "Downloads/MediaDownloader",
    val defaultFormat: DownloadFormat = DownloadFormat.MP4,
    val videoQuality: VideoQuality = VideoQuality.BEST,
    val audioQuality: AudioQuality = AudioQuality.K192,
    val useAria2c: Boolean = true,
    val cookiesFilePath: String? = null,
    val cookiesFileName: String? = null
)
