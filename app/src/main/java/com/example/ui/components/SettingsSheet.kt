package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cookie
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.AppSettings
import com.example.data.AudioQuality
import com.example.data.DownloadFormat
import com.example.data.VideoQuality
import com.example.engine.EngineStatus
import com.example.ui.theme.MonoFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: AppSettings,
    engineStatus: EngineStatus,
    onDismiss: () -> Unit,
    onDefaultFormatChange: (DownloadFormat) -> Unit,
    onVideoQualityChange: (VideoQuality) -> Unit,
    onAudioQualityChange: (AudioQuality) -> Unit,
    onUseAria2cChange: (Boolean) -> Unit,
    onPickFolder: () -> Unit,
    onResetFolder: () -> Unit,
    onImportCookies: () -> Unit,
    onSyncBrowserCookies: () -> Unit = {},
    onClearCookies: () -> Unit,
    onUpdateEngine: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "Download Settings",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Default Format
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "DEFAULT FORMAT",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DownloadFormat.entries.forEach { fmt ->
                        FilterChip(
                            selected = fmt == settings.defaultFormat,
                            onClick = { onDefaultFormatChange(fmt) },
                            label = { Text(fmt.label) }
                        )
                    }
                }
            }

            // Video Quality
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "VIDEO QUALITY",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VideoQuality.entries.forEach { q ->
                        FilterChip(
                            selected = q == settings.videoQuality,
                            onClick = { onVideoQualityChange(q) },
                            label = { Text(q.label) }
                        )
                    }
                }
            }

            // Audio Quality
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "AUDIO BITRATE (MP3)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AudioQuality.entries.forEach { q ->
                        FilterChip(
                            selected = q == settings.audioQuality,
                            onClick = { onAudioQualityChange(q) },
                            label = { Text(q.label) }
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Multi-Connection Aria2c Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Speed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Multi-connection accelerator",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Use aria2c for faster parallel stream downloads",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.useAria2c,
                    onCheckedChange = onUseAria2cChange,
                    modifier = Modifier.testTag("settings_aria2c_switch")
                )
            }

            // Save Location Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Save Folder", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = settings.saveLocationName,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFontFamily),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onPickFolder,
                            modifier = Modifier.testTag("settings_pick_folder_button")
                        ) {
                            Text("Choose Folder")
                        }
                        if (settings.saveLocationUri != null) {
                            TextButton(onClick = onResetFolder) {
                                Text("Use Default")
                            }
                        }
                    }
                }
            }

            // Cookies Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Cookie,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Authentication Cookies", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = settings.cookiesFileName ?: "None loaded (public videos only)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onSyncBrowserCookies,
                            modifier = Modifier.testTag("settings_sync_browser_cookies_button")
                        ) {
                            Text("Sync Browser")
                        }
                        OutlinedButton(
                            onClick = onImportCookies,
                            modifier = Modifier.testTag("settings_import_cookies_button")
                        ) {
                            Text("Import .txt")
                        }
                        if (settings.cookiesFilePath != null) {
                            TextButton(onClick = onClearCookies) {
                                Text("Clear", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            // Engine Update
            Button(
                onClick = onUpdateEngine,
                enabled = engineStatus !is EngineStatus.Updating && engineStatus !is EngineStatus.Initializing,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("settings_update_engine_button")
            ) {
                if (engineStatus is EngineStatus.Updating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Rounded.SystemUpdate, contentDescription = null)
                }
                Spacer(Modifier.size(8.dp))
                Text(
                    if (engineStatus is EngineStatus.Updating) "Updating yt-dlp..."
                    else "Update Download Engine"
                )
            }

            Text(
                text = "Media Downloader Pro • v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}
