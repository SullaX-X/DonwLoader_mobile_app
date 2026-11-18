package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cookie
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.DownloadFormat
import com.example.ui.theme.MonoFontFamily

private val QuickSites = listOf(
    "YouTube" to "https://m.youtube.com",
    "Vimeo" to "https://vimeo.com",
    "SoundCloud" to "https://m.soundcloud.com",
    "Dailymotion" to "https://www.dailymotion.com"
)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    isVisible: Boolean,
    initialUrl: String,
    onUrlUpdated: (String) -> Unit,
    onDownloadPage: (String, DownloadFormat) -> Unit,
    onSyncCookies: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var addressInput by remember { mutableStateOf(initialUrl) }
    var currentPageUrl by remember { mutableStateOf(initialUrl) }
    var pageProgress by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val focusManager = LocalFocusManager.current

    fun navigateTo(raw: String) {
        focusManager.clearFocus()
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return
        val target = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else if (trimmed.contains(".") && !trimmed.contains(" ")) {
            "https://$trimmed"
        } else {
            "https://duckduckgo.com/?q=${android.net.Uri.encode(trimmed)}"
        }
        addressInput = target
        currentPageUrl = target
        onUrlUpdated(target)
        webViewRef?.loadUrl(target)
    }

    BackHandler(enabled = isVisible && canGoBack) {
        webViewRef?.goBack()
    }

    DisposableEffect(isVisible) {
        if (isVisible) {
            webViewRef?.onResume()
        } else {
            webViewRef?.onPause()
        }
        onDispose { }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Browser Navigation & Address Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { webViewRef?.goBack() },
                            enabled = canGoBack,
                            modifier = Modifier.testTag("browser_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back"
                            )
                        }

                        IconButton(
                            onClick = { webViewRef?.goForward() },
                            enabled = canGoForward,
                            modifier = Modifier.testTag("browser_forward_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = "Forward"
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Language,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                BasicTextField(
                                    value = addressInput,
                                    onValueChange = { addressInput = it },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("browser_address_input"),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = MonoFontFamily,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Uri,
                                        imeAction = ImeAction.Go
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onGo = { navigateTo(addressInput) }
                                    )
                                )
                                if (addressInput.isNotEmpty()) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Clear address",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(RoundedCornerShape(9.dp))
                                            .clickable { addressInput = "" }
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = {
                                if (pageProgress in 1..99) {
                                    webViewRef?.stopLoading()
                                } else {
                                    webViewRef?.reload()
                                }
                            },
                            modifier = Modifier.testTag("browser_refresh_button")
                        ) {
                            Icon(
                                imageVector = if (pageProgress in 1..99) Icons.Rounded.Close else Icons.Rounded.Refresh,
                                contentDescription = if (pageProgress in 1..99) "Stop" else "Refresh"
                            )
                        }
                    }

                    // Quick Bookmarks & Cookie Sync Strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = onSyncCookies,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Cookie,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            },
                            label = { Text("Sync Cookies", style = MaterialTheme.typography.labelMedium) },
                            modifier = Modifier.testTag("browser_sync_cookies_chip")
                        )
                        QuickSites.forEach { (label, siteUrl) ->
                            AssistChip(
                                onClick = { navigateTo(siteUrl) },
                                label = { Text(label, style = MaterialTheme.typography.labelMedium) }
                            )
                        }
                    }

                    if (pageProgress in 1..99) {
                        LinearProgressIndicator(
                            progress = { pageProgress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }

            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                pageProgress = newProgress
                            }
                        }
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                return false
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                if (url != null) {
                                    addressInput = url
                                    currentPageUrl = url
                                    onUrlUpdated(url)
                                }
                                canGoBack = view?.canGoBack() == true
                                canGoForward = view?.canGoForward() == true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                runCatching { android.webkit.CookieManager.getInstance().flush() }
                                if (url != null) {
                                    addressInput = url
                                    currentPageUrl = url
                                    onUrlUpdated(url)
                                }
                                canGoBack = view?.canGoBack() == true
                                canGoForward = view?.canGoForward() == true
                            }
                        }
                        webViewRef = this
                        loadUrl(initialUrl)
                    }
                }
            )
        }

        // Quick Download Floating Actions
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SmallFloatingActionButton(
                onClick = { onDownloadPage(currentPageUrl, DownloadFormat.MP3) },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.testTag("browser_download_mp3_fab")
            ) {
                Icon(
                    imageVector = Icons.Rounded.AudioFile,
                    contentDescription = "Download MP3 audio from current page"
                )
            }

            ExtendedFloatingActionButton(
                onClick = { onDownloadPage(currentPageUrl, DownloadFormat.MP4) },
                icon = { Icon(Icons.Rounded.Download, contentDescription = null) },
                text = { Text("Download Video", style = MaterialTheme.typography.labelLarge) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("browser_download_mp4_fab")
            )
        }
    }
}
