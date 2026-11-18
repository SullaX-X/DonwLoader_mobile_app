package com.example.data

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object MediaLibraryRepository {
    private const val TAG = "MediaLibraryRepo"
    private val URL_REGEX = Regex("""https?://[^\s"'<>]+""", RegexOption.IGNORE_CASE)
    private val SPEED_REGEX = Regex("""(\d+(?:\.\d+)?\s*[KMG]i?B/s)""", RegexOption.IGNORE_CASE)

    fun extractUrlsFromText(raw: String): List<String> {
        val results = linkedSetOf<String>()
        raw.lineSequence().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                URL_REGEX.findAll(trimmed).forEach { match ->
                    results.add(match.value.trimEnd('.', ',', ';', ')', ']'))
                }
            }
        }
        return results.toList()
    }

    fun isValidMediaUrl(input: String): Boolean {
        return extractUrlsFromText(input).isNotEmpty()
    }

    fun sanitizeFilename(name: String): String {
        val cleaned = name.replace(Regex("""[/\\:*?"<>|\r\n]+"""), "_").trim().trim('.')
        return cleaned.ifEmpty { "media_${System.currentTimeMillis()}" }
    }

    fun formatEta(seconds: Long): String {
        if (seconds <= 0L) return ""
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) {
            "%02d:%02d:%02d".format(h, m, s)
        } else {
            "%02d:%02d".format(m, s)
        }
    }

    fun extractSpeedFromLine(line: String): String? {
        return SPEED_REGEX.find(line)?.groupValues?.getOrNull(1)?.replace(" ", "")
    }

    fun buildDisplayNameFromUrl(url: String, format: DownloadFormat): String {
        val parsed = runCatching { java.net.URI(url) }.getOrNull()
        val host = parsed?.host?.removePrefix("www.")?.removePrefix("m.") ?: "media"
        val queryParamV = parsed?.rawQuery
            ?.split("&")
            ?.firstOrNull { it.startsWith("v=") }
            ?.substringAfter("v=")
            ?.takeIf { it.isNotBlank() }
        val lastSeg = parsed?.path
            ?.trimEnd('/')
            ?.substringAfterLast('/')
            ?.takeIf { it.isNotBlank() && it != "watch" }
        val hint = queryParamV ?: lastSeg ?: "download"
        val shortHint = hint.take(24)
        return "${host}_$shortHint.${format.ext}"
    }

    suspend fun readUrlsFromTxtUri(context: Context, uri: Uri): List<String> = withContext(Dispatchers.IO) {
        val rawText = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: ""
        extractUrlsFromText(rawText)
    }

    suspend fun scanLibrary(context: Context, saveLocationUri: String?): List<MediaFileItem> =
        withContext(Dispatchers.IO) {
            val items = mutableListOf<MediaFileItem>()
            val seenNames = HashSet<String>()

            if (saveLocationUri != null) {
                runCatching {
                    val treeUri = Uri.parse(saveLocationUri)
                    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                        treeUri,
                        DocumentsContract.getTreeDocumentId(treeUri)
                    )
                    val projection = arrayOf(
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_SIZE,
                        DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                        DocumentsContract.Document.COLUMN_MIME_TYPE
                    )
                    context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                        val idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                        val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                        val sizeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                        val dateCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                        val mimeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)

                        while (cursor.moveToNext()) {
                            val docId = if (idCol != -1) cursor.getString(idCol) else continue
                            val name = if (nameCol != -1) cursor.getString(nameCol) ?: "Unknown" else "Unknown"
                            val size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L
                            val date = if (dateCol != -1) cursor.getLong(dateCol) else 0L
                            val mime = if (mimeCol != -1) cursor.getString(mimeCol) ?: "" else ""

                            if (isSupportedMedia(name, mime)) {
                                val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                                val isVideo = isVideoMedia(name, mime)
                                val effectiveMime = resolveMimeType(name, mime, isVideo)
                                items.add(
                                    MediaFileItem(
                                        id = docId,
                                        name = name,
                                        sizeBytes = size,
                                        lastModified = date,
                                        isVideo = isVideo,
                                        mimeType = effectiveMime,
                                        contentUri = docUri
                                    )
                                )
                            }
                        }
                    }
                }.onFailure { Log.e(TAG, "Failed to scan SAF directory", it) }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    runCatching {
                        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                        val projection = arrayOf(
                            MediaStore.Downloads._ID,
                            MediaStore.Downloads.DISPLAY_NAME,
                            MediaStore.Downloads.SIZE,
                            MediaStore.Downloads.DATE_MODIFIED,
                            MediaStore.Downloads.MIME_TYPE,
                            MediaStore.Downloads.RELATIVE_PATH
                        )
                        val selection = "${MediaStore.Downloads.RELATIVE_PATH} LIKE ?"
                        val selectionArgs = arrayOf("%MediaDownloader%")
                        context.contentResolver.query(
                            collection,
                            projection,
                            selection,
                            selectionArgs,
                            "${MediaStore.Downloads.DATE_MODIFIED} DESC"
                        )?.use { cursor ->
                            val idCol = cursor.getColumnIndex(MediaStore.Downloads._ID)
                            val nameCol = cursor.getColumnIndex(MediaStore.Downloads.DISPLAY_NAME)
                            val sizeCol = cursor.getColumnIndex(MediaStore.Downloads.SIZE)
                            val dateCol = cursor.getColumnIndex(MediaStore.Downloads.DATE_MODIFIED)
                            val mimeCol = cursor.getColumnIndex(MediaStore.Downloads.MIME_TYPE)

                            while (cursor.moveToNext()) {
                                val id = if (idCol != -1) cursor.getLong(idCol) else continue
                                val name = if (nameCol != -1) cursor.getString(nameCol) ?: "Unknown" else "Unknown"
                                val size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L
                                val dateSec = if (dateCol != -1) cursor.getLong(dateCol) else 0L
                                val mime = if (mimeCol != -1) cursor.getString(mimeCol) ?: "" else ""

                                if (isSupportedMedia(name, mime) && seenNames.add(name)) {
                                    val uri = ContentUris.withAppendedId(collection, id)
                                    val isVideo = isVideoMedia(name, mime)
                                    items.add(
                                        MediaFileItem(
                                            id = "ms_$id",
                                            name = name,
                                            sizeBytes = size,
                                            lastModified = dateSec * 1000L,
                                            isVideo = isVideo,
                                            mimeType = resolveMimeType(name, mime, isVideo),
                                            contentUri = uri
                                        )
                                    )
                                }
                            }
                        }
                    }.onFailure { Log.e(TAG, "Failed to scan MediaStore Downloads", it) }
                }

                // Fallback scan for legacy files in Downloads/MediaDownloader
                runCatching {
                    val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val folder = File(downloadDir, "MediaDownloader")
                    if (folder.exists() && folder.isDirectory) {
                        folder.listFiles()?.forEach { file ->
                            if (file.isFile && isSupportedMedia(file.name, "") && seenNames.add(file.name)) {
                                val isVideo = isVideoMedia(file.name, "")
                                val contentUri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                                items.add(
                                    MediaFileItem(
                                        id = file.absolutePath,
                                        name = file.name,
                                        sizeBytes = file.length(),
                                        lastModified = file.lastModified(),
                                        isVideo = isVideo,
                                        mimeType = resolveMimeType(file.name, "", isVideo),
                                        contentUri = contentUri,
                                        filePath = file.absolutePath
                                    )
                                )
                            }
                        }
                    }
                }.onFailure { Log.e(TAG, "Failed to scan legacy Downloads folder", it) }
            }

            items.sortedByDescending { it.lastModified }
        }

    fun openMediaFile(context: Context, item: MediaFileItem): Boolean {
        return runCatching {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(item.contentUri, item.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open with").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            true
        }.getOrElse {
            Log.e(TAG, "No handler to open ${item.name}", it)
            false
        }
    }

    fun shareMediaFile(context: Context, item: MediaFileItem): Boolean {
        return runCatching {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = item.mimeType
                putExtra(Intent.EXTRA_STREAM, item.contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share ${item.name}").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            true
        }.getOrElse {
            Log.e(TAG, "Failed to share ${item.name}", it)
            false
        }
    }

    suspend fun deleteMediaFile(context: Context, item: MediaFileItem, isSaf: Boolean): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                when {
                    isSaf -> DocumentsContract.deleteDocument(context.contentResolver, item.contentUri)
                    item.filePath != null -> File(item.filePath).delete()
                    else -> context.contentResolver.delete(item.contentUri, null, null) > 0
                }
            }.getOrElse {
                Log.e(TAG, "Failed to delete ${item.name}", it)
                false
            }
        }

    private fun isSupportedMedia(name: String, mime: String): Boolean {
        val lower = name.lowercase()
        return mime.startsWith("video/") ||
            mime.startsWith("audio/") ||
            lower.endsWith(".mp4") ||
            lower.endsWith(".mp3") ||
            lower.endsWith(".webm") ||
            lower.endsWith(".m4a") ||
            lower.endsWith(".mkv") ||
            lower.endsWith(".opus")
    }

    private fun isVideoMedia(name: String, mime: String): Boolean {
        val lower = name.lowercase()
        return mime.startsWith("video/") ||
            lower.endsWith(".mp4") ||
            lower.endsWith(".webm") ||
            lower.endsWith(".mkv")
    }

    private fun resolveMimeType(name: String, mime: String, isVideo: Boolean): String {
        if (mime.isNotBlank() && mime != "application/octet-stream") return mime
        val lower = name.lowercase()
        return when {
            lower.endsWith(".mp3") -> "audio/mpeg"
            lower.endsWith(".m4a") -> "audio/mp4"
            lower.endsWith(".opus") -> "audio/opus"
            lower.endsWith(".webm") -> "video/webm"
            lower.endsWith(".mkv") -> "video/x-matroska"
            isVideo -> "video/mp4"
            else -> "audio/mpeg"
        }
    }
}
