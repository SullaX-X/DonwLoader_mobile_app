package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import com.example.data.AppSettings
import com.example.data.DownloadFormat
import com.example.data.DownloadMode
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun homeScreen_rendersCoreControls() {
    composeTestRule.setContent {
      MyApplicationTheme {
        HomeScreen(
          urlInput = "https://example.com/video",
          downloadMode = DownloadMode.SINGLE,
          selectedFormat = DownloadFormat.MP4,
          batchUrls = emptyList(),
          batchFileName = null,
          settings = AppSettings(),
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

    composeTestRule.onNodeWithTag("url_input").assertIsDisplayed()
    composeTestRule.onNodeWithTag("pick_folder_card").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("start_download_button").performScrollTo().assertIsDisplayed()
  }
}
