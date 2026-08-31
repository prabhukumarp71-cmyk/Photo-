package com.example.processor

import android.graphics.Bitmap
import android.graphics.Color
import com.example.data.model.QualityMetrics
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object PhotoQualityAnalyzer {

    /**
     * Analyzes image sharpness, noise levels, and optical focus fidelity.
     */
    fun analyze(bitmap: Bitmap): QualityMetrics {
        val width = bitmap.width
        val height = bitmap.height

        // Downscale slightly for fast analysis if large
        val sampleScale = max(1, max(width, height) / 400)
        val sampleW = width / sampleScale
        val sampleH = height / sampleScale

        val sampleBitmap = if (sampleScale > 1) {
            Bitmap.createScaledBitmap(bitmap, sampleW, sampleH, true)
        } else {
            bitmap
        }

        val pixels = IntArray(sampleW * sampleH)
        sampleBitmap.getPixels(pixels, 0, sampleW, 0, 0, sampleW, sampleH)

        // Convert to Luminance
        val lum = FloatArray(sampleW * sampleH)
        for (i in pixels.indices) {
            val c夺 = pixels[i]
            val r = (c夺 shr 16) and 0xFF
            val g = (c夺 shr 8) and 0xFF
            val b = c夺 and 0xFF
            lum[i] = 0.299f * r + 0.587f * g + 0.114f * b
        }

        // Calculate Laplacian Variance (Focus / Sharpness measure)
        // Kernel: [0, 1, 0], [1, -4, 1], [0, 1, 0]
        var lapSum = 0.0
        var lapSumSq = 0.0
        var lapCount = 0

        // Noise estimation using small high-pass difference in non-edge regions
        var noiseSum = 0.0
        var noiseCount = 0

        for (y in 1 until sampleH - 1) {
            for (x in 1 until sampleW - 1) {
                val idx = y * sampleW + x
                val center = lum[idx]
                val top = lum[idx - sampleW]
                val bottom = lum[idx + sampleW]
                val left = lum[idx - 1]
                val right = lum[idx + 1]

                val lap = (top + bottom + left + right) - 4f * center
                lapSum += lap
                lapSumSq += lap * lap
                lapCount++

                // If this is a relatively flat area, measure high-frequency variation as noise
                val localRange = max(max(top, bottom), max(left, right)) - min(min(top, bottom), min(left, right))
                if (localRange < 20f) {
                    val noiseDelta = abs(center - (top + bottom + left + right) / 4f)
                    noiseSum += noiseDelta * noiseDelta
                    noiseCount++
                }
            }
        }

        val meanLap = if (lapCount > 0) lapSum / lapCount else 0.0
        val varianceLap = if (lapCount > 0) (lapSumSq / lapCount) - (meanLap * meanLap) else 0.0
        val sharpnessScore = min(100f, max(5f, (sqrt(max(0.0, varianceLap)).toFloat() * 2.2f)))

        val noiseVariance = if (noiseCount > 0) noiseSum / noiseCount else 0.0
        val noiseScore = min(100f, max(2f, (sqrt(noiseVariance).toFloat() * 4.5f)))

        // Estimate blur radius
        val blurRadius = when {
            sharpnessScore < 20f -> 4.5f
            sharpnessScore < 40f -> 3.0f
            sharpnessScore < 60f -> 1.8f
            sharpnessScore < 80f -> 1.0f
            else -> 0.4f
        }

        val diagnosis = when {
            sharpnessScore < 25f && noiseScore > 40f -> "Severe out-of-focus blur with high sensor noise"
            sharpnessScore < 30f -> "Noticeable lens defocus & soft blurry details"
            sharpnessScore < 50f && noiseScore > 35f -> "Mild motion blur with chromatic ISO grain"
            sharpnessScore < 55f -> "Moderate softness — edges need AI sharpening"
            noiseScore > 40f -> "Heavy image noise & compression artifacts"
            else -> "High clarity & well-defined optical focus"
        }

        return QualityMetrics(
            sharpnessScore = sharpnessScore,
            noiseScore = noiseScore,
            blurRadiusEstimate = blurRadius,
            diagnosticSummary = diagnosis,
            resolution = "${width} × ${height} px"
        )
    }
}
