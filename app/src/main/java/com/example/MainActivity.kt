package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppTab
import com.example.data.DownloadMode
import com.example.data.TaskState
import com.example.engine.EngineStatus
import com.example.ui.DownloaderViewModel
import com.example.ui.components.SettingsSheet
import com.example.ui.screens.ActivityScreen
import com.example.ui.screens.BrowserScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val downloaderViewModel: DownloaderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)

        setContent {
            MyApplicationTheme {
                DownloaderAppRoot(viewModel = downloaderViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                downloaderViewModel.handleSharedText(sharedText)
            }
        }
    }
}

@Composable
fun DownloaderAppRoot(viewModel: DownloaderViewModel = viewModel()) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val engineStatus by viewModel.engineStatus.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val urlInput by viewModel.urlInput.collectAsStateWithLifecycle()
    val downloadMode by viewModel.downloadMode.collectAsStateWithLifecycle()
    val selectedFormat by viewModel.selectedFormat.collectAsStateWithLifecycle()
    val batchUrls by viewModel.batchUrls.collectAsStateWithLifecycle()
    val batchFileName by viewModel.batchFileName.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val libraryFiles by viewModel.filteredLibraryFiles.collectAsStateWithLifecycle()
    val isLoadingLibrary by viewModel.isLoadingLibrary.collectAsStateWithLifecycle()
    val librarySearch by viewModel.librarySearchQuery.collectAsStateWithLifecycle()
    val libraryFilter by viewModel.libraryFilter.collectAsStateWithLifecycle()
    val activityLogs by viewModel.activityLogs.collectAsStateWithLifecycle()
    val browserUrl by viewModel.browserUrl.collectAsStateWithLifecycle()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var hasVisitedBrowser by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(currentTab) {
        if (currentTab == AppTab.BROWSER) {
            hasVisitedBrowser = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val dirPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { viewModel.setSaveLocation(it) }
    }

    val batchTxtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importBatchTxt(it) }
    }

    val cookiesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importCookies(it) }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Proceed regardless; notifications are optional */ }

    var pendingDownloadAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val legacyStorageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingDownloadAction?.invoke()
        } else {
            dirPickerLauncher.launch(null)
        }
        pendingDownloadAction = null
    }

    fun launchWithPermissions(action: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotif = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasNotif) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && settings.saveLocationUri == null) {
            val hasStorage = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasStorage) {
                pendingDownloadAction = action
                legacyStorageLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                return
            }
        }
        action()
    }

    BackHandler(enabled = currentTab != AppTab.HOME) {
        viewModel.selectTab(AppTab.HOME)
    }

    val activeTaskCount = remember(tasks) {
        tasks.count { it.state == TaskState.RUNNING || it.state == TaskState.QUEUED }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useNavigationRail) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxHeight()
                        .navigationBarsPadding()
                ) {
                    Spacer(Modifier.size(16.dp))
                    AppTab.entries.forEach { tab ->
                        val selected = tab == currentTab
                        NavigationRailItem(
                            selected = selected,
                            onClick = { viewModel.selectTab(tab) },
                            icon = {
                                TabIcon(
                                    tab = tab,
                                    selected = selected,
                                    badgeCount = if (tab == AppTab.HOME) activeTaskCount else 0
                                )
                            },
                            label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }

            Scaffold(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .imePadding(),
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    DownloaderTopBar(
                        engineStatus = engineStatus,
                        onOpenSettings = { viewModel.setSettingsOpen(true) }
                    )
                },
                bottomBar = {
                    if (!useNavigationRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            AppTab.entries.forEach { tab ->
                                val selected = tab == currentTab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { viewModel.selectTab(tab) },
                                    icon = {
                                        TabIcon(
                                            tab = tab,
                                            selected = selected,
                                            badgeCount = if (tab == AppTab.HOME) activeTaskCount else 0
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.label,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                ) {
                    if (hasVisitedBrowser) {
                        BrowserScreen(
                            isVisible = currentTab == AppTab.BROWSER,
                            initialUrl = browserUrl,
                            onUrlUpdated = viewModel::updateBrowserUrl,
                            onDownloadPage = { url, format ->
                                launchWithPermissions {
                                    viewModel.downloadFromBrowser(url, format)
                                }
                            },
                            onSyncCookies = viewModel::syncBrowserCookies,
                            modifier = Modifier
                                .fillMaxSize()
                                .zIndex(if (currentTab == AppTab.BROWSER) 2f else 0f)
                        )
                    }

                    if (currentTab != AppTab.BROWSER) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                .zIndex(3f)
                        ) {
                            when (currentTab) {
                                AppTab.HOME -> HomeScreen(
                                    urlInput = urlInput,
                                    downloadMode = downloadMode,
                                    selectedFormat = selectedFormat,
                                    batchUrls = batchUrls,
                                    batchFileName = batchFileName,
                                    settings = settings,
                                    tasks = tasks,
                                    onUrlChange = viewModel::updateUrlInput,
                                    onModeSelect = viewModel::selectDownloadMode,
                                    onFormatSelect = viewModel::selectFormat,
                                    onVideoQualitySelect = viewModel::setVideoQuality,
                                    onAudioQualitySelect = viewModel::setAudioQuality,
                                    onPickFolder = { dirPickerLauncher.launch(null) },
                                    onPickCookies = {
                                        cookiesLauncher.launch(arrayOf("text/plain", "*/*"))
                                    },
                                    onPickBatchFile = {
                                        batchTxtLauncher.launch(arrayOf("text/plain"))
                                    },
                                    onClearBatchFile = viewModel::clearBatchSelection,
                                    onStartDownload = {
                                        launchWithPermissions {
                                            if (downloadMode == DownloadMode.SINGLE) {
                                                viewModel.startSingleDownload()
                                            } else {
                                                viewModel.startBatchDownload()
                                            }
                                        }
                                    },
                                    onCancelTask = { viewModel.cancelTask(it.id) },
                                    onRetryTask = {
                                        launchWithPermissions {
                                            viewModel.retryUrl(it.url)
                                        }
                                    },
                                    onDismissTask = { viewModel.dismissTask(it.id) },
                                    onClearFinished = viewModel::clearFinishedTasks
                                )
                                AppTab.LIBRARY -> LibraryScreen(
                                    files = libraryFiles,
                                    isLoading = isLoadingLibrary,
                                    searchQuery = librarySearch,
                                    activeFilter = libraryFilter,
                                    saveLocationName = settings.saveLocationName,
                                    onSearchChange = viewModel::updateLibrarySearch,
                                    onFilterChange = viewModel::updateLibraryFilter,
                                    onRefresh = viewModel::refreshLibrary,
                                    onOpenFile = viewModel::openMediaItem,
                                    onShareFile = viewModel::shareMediaItem,
                                    onDeleteFile = viewModel::deleteMediaItem
                                )
                                AppTab.ACTIVITY -> ActivityScreen(
                                    engineStatus = engineStatus,
                                    logs = activityLogs,
                                    onUpdateEngine = viewModel::triggerEngineUpdate,
                                    onRetryUrl = { url ->
                                        launchWithPermissions {
                                            viewModel.retryUrl(url)
                                        }
                                    },
                                    onClearLogs = viewModel::clearActivityLogs
                                )
                                AppTab.BROWSER -> Unit
                            }
                        }
                    }
                }
            }
        }
    }

    if (isSettingsOpen) {
        SettingsSheet(
            settings = settings,
            engineStatus = engineStatus,
            onDismiss = { viewModel.setSettingsOpen(false) },
            onDefaultFormatChange = viewModel::setDefaultFormat,
            onVideoQualityChange = viewModel::setVideoQuality,
            onAudioQualityChange = viewModel::setAudioQuality,
            onUseAria2cChange = viewModel::setUseAria2c,
            onPickFolder = { dirPickerLauncher.launch(null) },
            onResetFolder = viewModel::resetSaveLocation,
            onImportCookies = { cookiesLauncher.launch(arrayOf("text/plain", "*/*")) },
            onSyncBrowserCookies = viewModel::syncBrowserCookies,
            onClearCookies = viewModel::clearCookies,
            onUpdateEngine = viewModel::triggerEngineUpdate
        )
    }
}

@Composable
private fun DownloaderTopBar(
    engineStatus: EngineStatus,
    onOpenSettings: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = when (engineStatus) {
                            EngineStatus.Initializing -> "ENGINE INITIALIZING..."
                            EngineStatus.Updating -> "UPDATING ENGINE..."
                            EngineStatus.Ready -> "ENGINE READY • V${BuildConfig.VERSION_NAME}"
                            is EngineStatus.Error -> "ENGINE ERROR"
                            EngineStatus.Idle -> "MEDIA UTILITY • V${BuildConfig.VERSION_NAME}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.testTag("top_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Open settings",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun TabIcon(
    tab: AppTab,
    selected: Boolean,
    badgeCount: Int
) {
    val icon = when (tab) {
        AppTab.HOME -> if (selected) Icons.Rounded.Home else Icons.Outlined.Home
        AppTab.BROWSER -> if (selected) Icons.Rounded.Language else Icons.Outlined.Language
        AppTab.LIBRARY -> if (selected) Icons.Rounded.Folder else Icons.Outlined.Folder
        AppTab.ACTIVITY -> if (selected) Icons.Rounded.History else Icons.Outlined.History
    }

    if (badgeCount > 0) {
        BadgedBox(
            badge = {
                Badge { Text(badgeCount.toString()) }
            }
        ) {
            Icon(imageVector = icon, contentDescription = tab.label)
        }
    } else {
        Icon(imageVector = icon, contentDescription = tab.label)
    }
}

@Preview(showBackground = true)
@Composable
fun DownloaderAppPreview() {
    MyApplicationTheme {
        HomeScreen(
            urlInput = "https://youtube.com/watch?v=demo",
            downloadMode = DownloadMode.SINGLE,
            selectedFormat = com.example.data.DownloadFormat.MP4,
            batchUrls = emptyList(),
            batchFileName = null,
            settings = com.example.data.AppSettings(),
            tasks = emptyList(),
            onUrlChange = {},
            onModeSelect = {},
            onFormatSelect = {},
            onVideoQualitySelect = {},
            onAudioQualitySelect = {},
            onPickFolder = {},
            onPickCookies = {},
            onPickBatchFile = {},
            onClearBatchFile = {},
            onStartDownload = {},
            onCancelTask = {},
            onRetryTask = {},
            onDismissTask = {},
            onClearFinished = {}
        )
    }
}
