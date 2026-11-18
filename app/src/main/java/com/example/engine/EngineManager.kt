package com.example.engine

import android.content.Context
import android.util.Log
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed interface EngineStatus {
    data object Idle : EngineStatus
    data object Initializing : EngineStatus
    data object Updating : EngineStatus
    data object Ready : EngineStatus
    data class Error(val message: String) : EngineStatus
}

object EngineManager {
    private const val TAG = "EngineManager"
    private const val PREFS_NAME = "engine_manager_prefs"
    private const val KEY_ENGINE_UPDATED_ONCE = "engine_updated_once"

    private val mutex = Mutex()

    @Volatile
    private var initialized = false

    @Volatile
    private var updatedInSession = false

    private val _status = MutableStateFlow<EngineStatus>(EngineStatus.Idle)
    val status: StateFlow<EngineStatus> = _status.asStateFlow()

    suspend fun ensureInitialized(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        mutex.withLock {
            if (!initialized) {
                _status.value = EngineStatus.Initializing
                try {
                    YoutubeDL.getInstance().init(appContext)
                    FFmpeg.getInstance().init(appContext)
                    Aria2c.getInstance().init(appContext)
                    initialized = true
                    Log.i(TAG, "Native libraries initialized")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to initialize engine", e)
                    _status.value = EngineStatus.Error(e.localizedMessage ?: "Engine initialization failed")
                    return@withContext Result.failure(e)
                }
            }

            // Always ensure yt-dlp is updated at least once so the old bundled binary
            // does not fail with YouTube "Precondition check failed" (HTTP 400).
            val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val hasEverUpdated = prefs.getBoolean(KEY_ENGINE_UPDATED_ONCE, false)

            if (!hasEverUpdated || !updatedInSession) {
                _status.value = EngineStatus.Updating
                val updatedOk = performUpdateLocked(appContext)
                if (updatedOk) {
                    prefs.edit().putBoolean(KEY_ENGINE_UPDATED_ONCE, true).apply()
                    updatedInSession = true
                }
            }

            _status.value = EngineStatus.Ready
            Result.success(Unit)
        }
    }

    suspend fun updateEngine(context: Context): Result<String> = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        mutex.withLock {
            if (!initialized) {
                _status.value = EngineStatus.Initializing
                try {
                    YoutubeDL.getInstance().init(appContext)
                    FFmpeg.getInstance().init(appContext)
                    Aria2c.getInstance().init(appContext)
                    initialized = true
                } catch (e: Exception) {
                    _status.value = EngineStatus.Error(e.localizedMessage ?: "Init failed")
                    return@withContext Result.failure(e)
                }
            }

            _status.value = EngineStatus.Updating
            return@withLock try {
                val status = try {
                    YoutubeDL.getInstance().updateYoutubeDL(
                        appContext,
                        YoutubeDL.UpdateChannel.NIGHTLY
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Nightly update failed, falling back to STABLE", e)
                    YoutubeDL.getInstance().updateYoutubeDL(
                        appContext,
                        YoutubeDL.UpdateChannel.STABLE
                    )
                }

                appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(KEY_ENGINE_UPDATED_ONCE, true)
                    .apply()
                updatedInSession = true
                _status.value = EngineStatus.Ready

                val versionName = runCatching { YoutubeDL.getInstance().version(appContext) }
                    .getOrNull()
                    ?.takeIf { it.isNotBlank() }

                val label = when (status) {
                    YoutubeDL.UpdateStatus.DONE ->
                        if (versionName != null) "Updated yt-dlp to $versionName"
                        else "Engine updated to latest release"
                    YoutubeDL.UpdateStatus.ALREADY_UP_TO_DATE ->
                        if (versionName != null) "Engine up to date ($versionName)"
                        else "Engine is already up to date"
                    else -> "Engine ready"
                }
                Log.i(TAG, label)
                Result.success(label)
            } catch (e: Exception) {
                Log.e(TAG, "Engine update failed", e)
                _status.value = EngineStatus.Ready
                Result.failure(e)
            }
        }
    }

    private fun performUpdateLocked(appContext: Context): Boolean {
        return try {
            try {
                YoutubeDL.getInstance().updateYoutubeDL(
                    appContext,
                    YoutubeDL.UpdateChannel.NIGHTLY
                )
            } catch (e: Exception) {
                Log.w(TAG, "Nightly channel update failed, trying STABLE", e)
                YoutubeDL.getInstance().updateYoutubeDL(
                    appContext,
                    YoutubeDL.UpdateChannel.STABLE
                )
            }
            Log.i(TAG, "yt-dlp binary updated successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Automatic yt-dlp update failed", e)
            false
        }
    }
}
