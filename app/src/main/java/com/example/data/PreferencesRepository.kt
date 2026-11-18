package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "downloader_prefs")

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val SAVE_LOCATION_URI = stringPreferencesKey("save_location_uri")
        val SAVE_LOCATION_NAME = stringPreferencesKey("save_location_name")
        val DEFAULT_FORMAT = stringPreferencesKey("default_format")
        val VIDEO_QUALITY = stringPreferencesKey("video_quality")
        val AUDIO_QUALITY = stringPreferencesKey("audio_quality")
        val USE_ARIA2C = booleanPreferencesKey("use_aria2c")
        val COOKIES_FILE_PATH = stringPreferencesKey("cookies_file_path")
        val COOKIES_FILE_NAME = stringPreferencesKey("cookies_file_name")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val cookiesPath = prefs[Keys.COOKIES_FILE_PATH]?.takeIf { File(it).exists() }
        AppSettings(
            saveLocationUri = prefs[Keys.SAVE_LOCATION_URI],
            saveLocationName = prefs[Keys.SAVE_LOCATION_NAME] ?: "Downloads/MediaDownloader",
            defaultFormat = DownloadFormat.entries.firstOrNull { it.ext == prefs[Keys.DEFAULT_FORMAT] }
                ?: DownloadFormat.MP4,
            videoQuality = VideoQuality.entries.firstOrNull { it.id == prefs[Keys.VIDEO_QUALITY] }
                ?: VideoQuality.BEST,
            audioQuality = AudioQuality.entries.firstOrNull { it.bitrate == prefs[Keys.AUDIO_QUALITY] }
                ?: AudioQuality.K192,
            useAria2c = prefs[Keys.USE_ARIA2C] ?: true,
            cookiesFilePath = cookiesPath,
            cookiesFileName = if (cookiesPath != null) prefs[Keys.COOKIES_FILE_NAME] else null
        )
    }

    suspend fun setSaveLocation(uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Some providers do not grant persistable permissions; still store URI if usable
        }
        val displayName = uri.lastPathSegment
            ?.split(":")
            ?.lastOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: "Custom Folder"

        context.dataStore.edit { prefs ->
            prefs[Keys.SAVE_LOCATION_URI] = uri.toString()
            prefs[Keys.SAVE_LOCATION_NAME] = displayName
        }
    }

    suspend fun resetSaveLocationToDefault() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.SAVE_LOCATION_URI)
            prefs[Keys.SAVE_LOCATION_NAME] = "Downloads/MediaDownloader"
        }
    }

    suspend fun setDefaultFormat(format: DownloadFormat) {
        context.dataStore.edit { it[Keys.DEFAULT_FORMAT] = format.ext }
    }

    suspend fun setVideoQuality(quality: VideoQuality) {
        context.dataStore.edit { it[Keys.VIDEO_QUALITY] = quality.id }
    }

    suspend fun setAudioQuality(quality: AudioQuality) {
        context.dataStore.edit { it[Keys.AUDIO_QUALITY] = quality.bitrate }
    }

    suspend fun setUseAria2c(enabled: Boolean) {
        context.dataStore.edit { it[Keys.USE_ARIA2C] = enabled }
    }

    suspend fun importCookiesFile(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val fileName = resolveDisplayName(uri) ?: "cookies.txt"
            val targetFile = File(context.filesDir, "cookies.txt")
            context.contentResolver.openInputStream(uri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(IllegalStateException("Unable to read selected file"))

            if (targetFile.length() == 0L) {
                targetFile.delete()
                return@withContext Result.failure(IllegalArgumentException("Selected cookies file is empty"))
            }

            context.dataStore.edit { prefs ->
                prefs[Keys.COOKIES_FILE_PATH] = targetFile.absolutePath
                prefs[Keys.COOKIES_FILE_NAME] = fileName
            }
            Result.success(fileName)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncCookiesFromBrowser(extraUrl: String? = null): Result<Int> = withContext(Dispatchers.IO) {
        val targetFile = File(context.filesDir, "cookies.txt")
        val exported = WebViewCookieExporter.exportFromWebView(
            context = context,
            targetFile = targetFile,
            extraUrl = extraUrl,
            requireAuthenticatedForYoutube = false
        ) ?: return@withContext Result.failure(
            IllegalStateException("No browser cookies found yet. Open a site in the Browser tab first.")
        )

        val (file, count) = exported
        context.dataStore.edit { prefs ->
            prefs[Keys.COOKIES_FILE_PATH] = file.absolutePath
            prefs[Keys.COOKIES_FILE_NAME] = "Browser ($count cookies)"
        }
        Result.success(count)
    }

    suspend fun clearCookiesFile() = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, "cookies.txt")
        if (file.exists()) file.delete()
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.COOKIES_FILE_PATH)
            prefs.remove(Keys.COOKIES_FILE_NAME)
        }
    }

    private fun resolveDisplayName(uri: Uri): String? {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx != -1) return cursor.getString(idx)
                }
            }
        return uri.lastPathSegment?.substringAfterLast('/')
    }
}
