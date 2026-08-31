package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.RestorationMode
import com.example.ui.components.QualityMetricsCard
import com.example.data.model.QualityMetrics
import com.example.ui.theme.MyApplicationTheme
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
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun quality_metrics_card_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        QualityMetricsCard(
          beforeMetrics = QualityMetrics(
            sharpnessScore = 22f,
            noiseScore = 48f,
            blurRadiusEstimate = 4.5f,
            diagnosticSummary = "Severe out-of-focus blur with high sensor noise",
            resolution = "720 × 720 px"
          ),
          afterMetrics = QualityMetrics(
            sharpnessScore = 86f,
            noiseScore = 12f,
            blurRadiusEstimate = 0.4f,
            diagnosticSummary = "High clarity & well-defined optical focus",
            resolution = "720 × 720 px"
          )
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}

