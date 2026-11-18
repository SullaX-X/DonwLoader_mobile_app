package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Cookie
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AppSettings
import com.example.data.AudioQuality
import com.example.data.DownloadFormat
import com.example.data.DownloadMode
import com.example.data.DownloadTaskItem
import com.example.data.TaskState
import com.example.data.VideoQuality
import com.example.ui.theme.MonoFontFamily

@Composable
fun HomeScreen(
    urlInput: String,
    downloadMode: DownloadMode,
    selectedFormat: DownloadFormat,
    batchUrls: List<String>,
    batchFileName: String?,
    settings: AppSettings,
    tasks: List<DownloadTaskItem>,
    onUrlChange: (String) -> Unit,
    onModeSelect: (DownloadMode) -> Unit,
    onFormatSelect: (DownloadFormat) -> Unit,
    onVideoQualitySelect: (VideoQuality) -> Unit,
    onAudioQualitySelect: (AudioQuality) -> Unit,
    onPickFolder: () -> Unit,
    onPickCookies: () -> Unit,
    onPickBatchFile: () -> Unit,
    onClearBatchFile: () -> Unit,
    onStartDownload: () -> Unit,
    onCancelTask: (DownloadTaskItem) -> Unit,
    onRetryTask: (DownloadTaskItem) -> Unit,
    onDismissTask: (DownloadTaskItem) -> Unit,
    onClearFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTwoPane = maxWidth >= 840.dp

        if (isTwoPane) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DownloaderControlPanel(
                        urlInput = urlInput,
                        downloadMode = downloadMode,
                        selectedFormat = selectedFormat,
                        batchUrls = batchUrls,
                        batchFileName = batchFileName,
                        settings = settings,
                        onUrlChange = onUrlChange,
                        onModeSelect = onModeSelect,
                        onFormatSelect = onFormatSelect,
                        onVideoQualitySelect = onVideoQualitySelect,
                        onAudioQualitySelect = onAudioQualitySelect,
                        onPickFolder = onPickFolder,
                        onPickCookies = onPickCookies,
                        onPickBatchFile = onPickBatchFile,
                        onClearBatchFile = onClearBatchFile,
                        onStartDownload = onStartDownload
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QueueSection(
                        tasks = tasks,
                        onCancelTask = onCancelTask,
                        onRetryTask = onRetryTask,
                        onDismissTask = onDismissTask,
                        onClearFinished = onClearFinished,
                        showEmptyPlaceholder = true
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DownloaderControlPanel(
                        urlInput = urlInput,
                        downloadMode = downloadMode,
                        selectedFormat = selectedFormat,
                        batchUrls = batchUrls,
                        batchFileName = batchFileName,
                        settings = settings,
                        onUrlChange = onUrlChange,
                        onModeSelect = onModeSelect,
                        onFormatSelect = onFormatSelect,
                        onVideoQualitySelect = onVideoQualitySelect,
                        onAudioQualitySelect = onAudioQualitySelect,
                        onPickFolder = onPickFolder,
                        onPickCookies = onPickCookies,
                        onPickBatchFile = onPickBatchFile,
                        onClearBatchFile = onClearBatchFile,
                        onStartDownload = onStartDownload
                    )

                    AnimatedVisibility(
                        visible = tasks.isNotEmpty(),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        QueueSection(
                            tasks = tasks,
                            onCancelTask = onCancelTask,
                            onRetryTask = onRetryTask,
                            onDismissTask = onDismissTask,
                            onClearFinished = onClearFinished,
                            showEmptyPlaceholder = false
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun DownloaderControlPanel(
    urlInput: String,
    downloadMode: DownloadMode,
    selectedFormat: DownloadFormat,
    batchUrls: List<String>,
    batchFileName: String?,
    settings: AppSettings,
    onUrlChange: (String) -> Unit,
    onModeSelect: (DownloadMode) -> Unit,
    onFormatSelect: (DownloadFormat) -> Unit,
    onVideoQualitySelect: (VideoQuality) -> Unit,
    onAudioQualitySelect: (AudioQuality) -> Unit,
    onPickFolder: () -> Unit,
    onPickCookies: () -> Unit,
    onPickBatchFile: () -> Unit,
    onClearBatchFile: () -> Unit,
    onStartDownload: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Mode Switcher
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            DownloadMode.entries.forEach { mode ->
                val selected = mode == downloadMode
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = if (selected) 1.dp else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onModeSelect(mode) }
                        .testTag("mode_${mode.name.lowercase()}")
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Main Link & Format Card
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Link,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (downloadMode == DownloadMode.SINGLE) "Media URL" else "Batch Link File",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (downloadMode == DownloadMode.SINGLE) {
                            "Paste any video or audio link"
                        } else {
                            "Import a .txt file with one link per line"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (downloadMode == DownloadMode.SINGLE) {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = onUrlChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("url_input"),
                    placeholder = {
                        Text(
                            "https://youtube.com/watch?v=...",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFontFamily),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFontFamily),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            onStartDownload()
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    trailingIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            if (urlInput.isNotEmpty()) {
                                IconButton(
                                    onClick = { onUrlChange("") },
                                    modifier = Modifier.testTag("clear_url_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Clear URL",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        val clip = clipboardManager.getText()?.text
                                        if (!clip.isNullOrBlank()) {
                                            onUrlChange(clip.trim())
                                        }
                                    }
                                    .testTag("paste_url_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ContentPaste,
                                        contentDescription = "Paste link",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        "PASTE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    }
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    border = BorderStroke(
                        1.dp,
                        if (batchUrls.isNotEmpty()) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onPickBatchFile() }
                        .testTag("batch_file_picker")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FolderOpen,
                                contentDescription = null,
                                tint = if (batchUrls.isNotEmpty()) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = batchFileName ?: "Tap to select links.txt",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (batchUrls.isNotEmpty()) {
                                        "${batchUrls.size} valid link(s) ready to queue"
                                    } else {
                                        "Supports plain text files with http/https links"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (batchUrls.isNotEmpty()) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (batchFileName != null) {
                                IconButton(onClick = onClearBatchFile) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Clear batch file"
                                    )
                                }
                            }
                        }
                        if (batchUrls.isNotEmpty()) {
                            batchUrls.take(3).forEach { link ->
                                Text(
                                    text = "• $link",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFontFamily),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (batchUrls.size > 3) {
                                Text(
                                    text = "+${batchUrls.size - 3} more",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Format Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DownloadFormat.entries.forEach { fmt ->
                    val isSelected = fmt == selectedFormat
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(
                            1.5.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 68.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onFormatSelect(fmt) }
                            .testTag("format_${fmt.ext}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (fmt == DownloadFormat.MP4) Icons.Rounded.VideoFile else Icons.Rounded.AudioFile,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = fmt.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (fmt == DownloadFormat.MP4) settings.videoQuality.id else settings.audioQuality.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Quick Quality Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectedFormat == DownloadFormat.MP4) {
                    VideoQuality.entries.forEach { q ->
                        FilterChip(
                            selected = q == settings.videoQuality,
                            onClick = { onVideoQualitySelect(q) },
                            label = { Text(q.label, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                } else {
                    AudioQuality.entries.forEach { q ->
                        FilterChip(
                            selected = q == settings.audioQuality,
                            onClick = { onAudioQualitySelect(q) },
                            label = { Text(q.label, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
        }
    }

    // Destination & Cookies Cards
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f)),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 68.dp)
                .clip(RoundedCornerShape(18.dp))
                .clickable { onPickFolder() }
                .testTag("pick_folder_card")
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SAVE TO",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = settings.saveLocationName,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        val hasCookies = settings.cookiesFilePath != null
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (hasCookies) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
            },
            border = BorderStroke(
                1.dp,
                if (hasCookies) MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 68.dp)
                .clip(RoundedCornerShape(18.dp))
                .clickable { onPickCookies() }
                .testTag("cookies_card")
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            if (hasCookies) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Cookie,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "COOKIES",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hasCookies) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = settings.cookiesFileName ?: "Import .txt",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    // Primary Start Button
    val isReady = if (downloadMode == DownloadMode.SINGLE) urlInput.isNotBlank() else batchUrls.isNotEmpty()
    Button(
        onClick = onStartDownload,
        enabled = isReady,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        contentPadding = PaddingValues(vertical = 16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("start_download_button")
    ) {
        Icon(Icons.Rounded.Download, contentDescription = null)
        Spacer(Modifier.width(10.dp))
        Text(
            text = if (downloadMode == DownloadMode.BATCH && batchUrls.isNotEmpty()) {
                "Download ${batchUrls.size} Links"
            } else {
                "Start Download"
            },
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun QueueSection(
    tasks: List<DownloadTaskItem>,
    onCancelTask: (DownloadTaskItem) -> Unit,
    onRetryTask: (DownloadTaskItem) -> Unit,
    onDismissTask: (DownloadTaskItem) -> Unit,
    onClearFinished: () -> Unit,
    showEmptyPlaceholder: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "DOWNLOAD QUEUE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${tasks.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            if (tasks.any { it.state in setOf(TaskState.SUCCEEDED, TaskState.FAILED, TaskState.CANCELLED) }) {
                TextButton(
                    onClick = onClearFinished,
                    modifier = Modifier.testTag("clear_finished_button")
                ) {
                    Text("Clear done", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        if (tasks.isEmpty() && showEmptyPlaceholder) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.tertiaryContainer
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "Queue is idle",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Active and recent downloads appear here with live speed and ETA.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            tasks.forEach { task ->
                DownloadTaskCard(
                    task = task,
                    onCancel = { onCancelTask(task) },
                    onRetry = { onRetryTask(task) },
                    onDismiss = { onDismissTask(task) }
                )
            }
        }
    }
}

@Composable
private fun DownloadTaskCard(
    task: DownloadTaskItem,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_card_${task.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val iconBg = when (task.state) {
                    TaskState.SUCCEEDED -> MaterialTheme.colorScheme.secondaryContainer
                    TaskState.FAILED -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.primaryContainer
                }
                val iconTint = when (task.state) {
                    TaskState.SUCCEEDED -> MaterialTheme.colorScheme.secondary
                    TaskState.FAILED -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.primary
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (task.state) {
                            TaskState.SUCCEEDED -> Icons.Rounded.CheckCircle
                            TaskState.FAILED -> Icons.Rounded.ErrorOutline
                            else -> if (task.format == DownloadFormat.MP4) Icons.Rounded.VideoFile else Icons.Rounded.AudioFile
                        },
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = task.statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (task.state == TaskState.FAILED) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                when (task.state) {
                    TaskState.QUEUED, TaskState.RUNNING -> {
                        IconButton(
                            onClick = onCancel,
                            modifier = Modifier.testTag("cancel_task_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Cancel download",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    TaskState.FAILED -> {
                        Row {
                            if (task.url.isNotBlank()) {
                                IconButton(
                                    onClick = onRetry,
                                    modifier = Modifier.testTag("retry_task_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = "Retry download",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Dismiss task"
                                )
                            }
                        }
                    }
                    TaskState.SUCCEEDED, TaskState.CANCELLED -> {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Dismiss task",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (task.state == TaskState.RUNNING || task.state == TaskState.QUEUED) {
                if (task.state == TaskState.RUNNING && task.progress > 0f) {
                    LinearProgressIndicator(
                        progress = { task.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = buildString {
                            append(task.format.ext.uppercase())
                            if (task.qualityLabel.isNotBlank()) append(" • ${task.qualityLabel}")
                            if (task.speedText.isNotBlank()) append(" • ${task.speedText}")
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = buildString {
                            if (task.etaText.isNotBlank()) append("ETA ${task.etaText} • ")
                            append("${(task.progress * 100).toInt()}%")
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
