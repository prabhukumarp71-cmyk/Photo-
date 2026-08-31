package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.EnhancementStrength
import com.example.data.model.RestorationMode
import com.example.processor.AlgorithmicImageProcessor
import com.example.processor.PhotoQualityAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Photo Quality AI", appName)
  }

  @Test
  fun `test natural 4K restoration pipeline`() {
    // Create a 100x100 sample bitmap with soft gradients and details
    val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    for (x in 0 until 100) {
      for (y in 0 until 100) {
        val r = (x * 2).coerceIn(0, 240)
        val g = (y * 2).coerceIn(0, 240)
        val b = 128
        bitmap.setPixel(x, y, Color.rgb(r, g, b))
      }
    }

    val metricsBefore = PhotoQualityAnalyzer.analyze(bitmap)
    assertNotNull(metricsBefore)

    val enhanced = AlgorithmicImageProcessor.process(
      inputBitmap = bitmap,
      mode = RestorationMode.NATURAL_4K_ULTRA,
      selectedStrength = EnhancementStrength.BALANCED
    )

    assertNotNull(enhanced)
    // Verify 4K dimensions
    assertTrue(enhanced.width >= 3840 || enhanced.height >= 3840)

    val metricsAfter = PhotoQualityAnalyzer.analyze(enhanced)
    assertNotNull(metricsAfter)
  }
}

