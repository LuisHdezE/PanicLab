package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.home.HomeScreen
import com.example.ui.theme.PanicLabTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun homeScreen_screenshot() {
    composeTestRule.setContent {
      PanicLabTheme {
        HomeScreen(
          recentReports = emptyList(),
          kbVersion = "1.0.0",
          onNavigateToImportFile = {},
          onNavigateToPasteLog = {},
          onNavigateToHistory = {},
          onNavigateToKnowledgeBase = {},
          onNavigateToSettings = {},
          onNavigateToReport = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/home.png")
  }
}
