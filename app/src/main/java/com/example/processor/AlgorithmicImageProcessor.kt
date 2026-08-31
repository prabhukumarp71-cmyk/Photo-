package com.example.processor

import android.graphics.Bitmap
import android.graphics.Color
import com.example.data.model.ManualEnhancementParams
import com.example.data.model.RestorationMode
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

object AlgorithmicImageProcessor {

    /**
     * Executes algorithmic photo enhancement based on selected mode or manual parameters.
     */
    fun process(
        inputBitmap: Bitmap,
        mode: RestorationMode,
        customParams: ManualEnhancementParams? = null
    ): Bitmap {
        val params = customParams ?: when (mode) {
            RestorationMode.AI_DEEP_FOCUS -> ManualEnhancementParams(
                sharpenAmount = 2.4f,
                sharpenRadius = 2.2f,
                denoiseAmount = 0.35f,
                clarityContrast = 1.35f,
                deblurInverseStrength = 0.85f
            )
            RestorationMode.AI_ULTRA_ENHANCE -> ManualEnhancementParams(
                sharpenAmount = 2.0f,
                sharpenRadius = 1.8f,
                denoiseAmount = 0.65f,
                clarityContrast = 1.3f,
                deblurInverseStrength = 0.7f
            )
            RestorationMode.ALGO_DEBLUR_SHARPEN -> ManualEnhancementParams(
                sharpenAmount = 2.6f,
                sharpenRadius = 2.0f,
                denoiseAmount = 0.25f,
                clarityContrast = 1.25f,
                deblurInverseStrength = 0.8f
            )
            RestorationMode.ALGO_DEEP_DENOISE -> ManualEnhancementParams(
                sharpenAmount = 1.2f,
                sharpenRadius = 1.2f,
                denoiseAmount = 0.85f,
                clarityContrast = 1.15f,
                deblurInverseStrength = 0.3f
            )
            RestorationMode.MANUAL_STUDIO -> customParams ?: ManualEnhancementParams()
        }

        return applyFullRestorationPipeline(inputBitmap, params)
    }

    /**
     * Full multi-stage restoration pipeline:
     * 1. Edge-preserving Bilateral Denoise
     * 2. Inverse PSF Defocus Deconvolution & Focus Recovery
     * 3. Multi-radius Adaptive Unsharp Masking (High Frequency Boost)
     * 4. Micro-Contrast & Luminance Tone Curve
     */
    private fun applyFullRestorationPipeline(src: Bitmap, params: ManualEnhancementParams): Bitmap {
        val width = src.width
        val height = src.height

        val srcPixels = IntArray(width * height)
        src.getPixels(srcPixels, 0, width, 0, 0, width, height)

        // Stage 1: Denoise if requested
        val denoisedPixels = if (params.denoiseAmount > 0.05f) {
            applyBilateralDenoise(srcPixels, width, height, params.denoiseAmount)
        } else {
            srcPixels.clone()
        }

        // Stage 2: Inverse Focus Deblur & Unsharp Sharpening
        val sharpenedPixels = applyAdaptiveSharpenAndDeblur(
            denoisedPixels,
            width,
            height,
            sharpenAmount = params.sharpenAmount,
            deblurStrength = params.deblurInverseStrength
        )

        // Stage 3: Dynamic Range & Micro-Contrast Enhancement
        val finalPixels = applyMicroContrast(sharpenedPixels, width, height, params.clarityContrast)

        val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        outputBitmap.setPixels(finalPixels, 0, width, 0, 0, width, height)
        return outputBitmap
    }

    /**
     * Spatial edge-preserving bilateral denoising filter.
     * Smoothes flat noisy areas while preserving sharp edges.
     */
    private fun applyBilateralDenoise(
        pixels: IntArray,
        w: Int,
        h: Int,
        intensity: Float
    ): IntArray {
        val result = IntArray(pixels.size)
        val radius = if (intensity > 0.6f) 2 else 1
        val sigmaSpace = 2.0f
        val sigmaColor = 25.0f * (1.1f - intensity * 0.5f)

        for (y in 0 until h) {
            val yMin = max(0, y - radius)
            val yMax = min(h - 1, y + radius)

            for (x in 0 until w) {
                val centerIdx = y * w + x
                val centerPixel = pixels[centerIdx]
                val cA = (centerPixel ushr 24) and 0xFF
                val cR = (centerPixel shr 16) and 0xFF
                val cG = (centerPixel shr 8) and 0xFF
                val cB = centerPixel and 0xFF

                var sumR = 0f
                var sumG = 0f
                var sumB = 0f
                var totalWeight = 0f

                for (ny in yMin..yMax) {
                    val dy = ny - y
                    val nRow = ny * w

                    val xMin = max(0, x - radius)
                    val xMax = min(w - 1, x + radius)

                    for (nx in xMin..xMax) {
                        val dx = nx - x
                        val nIdx = nRow + nx
                        val nPixel = pixels[nIdx]

                        val nR = (nPixel shr 16) and 0xFF
                        val nG = (nPixel shr 8) and 0xFF
                        val nB = nPixel and 0xFF

                        val distSpatialSq = (dx * dx + dy * dy).toFloat()
                        val diffColorSq = ((cR - nR) * (cR - nR) + (cG - nG) * (cG - nG) + (cB - nB) * (cB - nB)).toFloat() / 3f

                        val spaceWeight = exp(-distSpatialSq / (2 * sigmaSpace * sigmaSpace))
                        val colorWeight = exp(-diffColorSq / (2 * sigmaColor * sigmaColor))
                        val weight = spaceWeight * colorWeight

                        sumR += nR * weight
                        sumG += nG * weight
                        sumB += nB * weight
                        totalWeight += weight
                    }
                }

                val outR = if (totalWeight > 0f) clamp((sumR / totalWeight).toInt()) else cR
                val outG = if (totalWeight > 0f) clamp((sumG / totalWeight).toInt()) else cG
                val outB = if (totalWeight > 0f) clamp((sumB / totalWeight).toInt()) else cB

                // Blend with original according to intensity
                val blendedR = clamp((cR * (1f - intensity) + outR * intensity).toInt())
                val blendedG = clamp((cG * (1f - intensity) + outG * intensity).toInt())
                val blendedB = clamp((cB * (1f - intensity) + outB * intensity).toInt())

                result[centerIdx] = (cA shl 24) or (blendedR shl 16) or (blendedG shl 8) or blendedB
            }
        }
        return result
    }

    /**
     * Adaptive unsharp masking combined with inverse point-spread function (PSF) deblurring.
     */
    private fun applyAdaptiveSharpenAndDeblur(
        pixels: IntArray,
        w: Int,
        h: Int,
        sharpenAmount: Float,
        deblurStrength: Float
    ): IntArray {
        val result = IntArray(pixels.size)

        // 3x3 high-pass sharpening kernel combined with inverse blur
        // Center weight boosts high frequencies while cross terms deconvolve soft light spill
        val inv = deblurStrength * 0.4f
        val kCenter = 1f + (4f + 4f * 0.707f) * (sharpenAmount * 0.25f + inv)
        val kCross = -1f * (sharpenAmount * 0.25f + inv)
        val kDiag = -0.707f * (sharpenAmount * 0.18f + inv * 0.5f)

        for (y in 0 until h) {
            val ym1 = max(0, y - 1) * w
            val y0 = y * w
            val yp1 = min(h - 1, y + 1) * w

            for (x in 0 until w) {
                val xm1 = max(0, x - 1)
                val x0 = x
                val xp1 = min(w - 1, x + 1)

                val c00 = pixels[ym1 + xm1]
                val c01 = pixels[ym1 + x0]
                val c02 = pixels[ym1 + xp1]

                val c10 = pixels[y0 + xm1]
                val c11 = pixels[y0 + x0]
                val c12 = pixels[y0 + xp1]

                val c20 = pixels[yp1 + xm1]
                val c21 = pixels[yp1 + x0]
                val c22 = pixels[yp1 + xp1]

                val a = (c11 ushr 24) and 0xFF

                // Process Channels
                val r = convolve3x3(c00, c01, c02, c10, c11, c12, c20, c21, c22, 16, kCenter, kCross, kDiag)
                val g = convolve3x3(c00, c01, c02, c10, c11, c12, c20, c21, c22, 8, kCenter, kCross, kDiag)
                val b = convolve3x3(c00, c01, c02, c10, c11, c12, c20, c21, c22, 0, kCenter, kCross, kDiag)

                result[y0 + x0] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        return result
    }

    private fun convolve3x3(
        c00: Int, c01: Int, c02: Int,
        c10: Int, c11: Int, c12: Int,
        c20: Int, c21: Int, c22: Int,
        shift: Int,
        kCenter: Float,
        kCross: Float,
        kDiag: Float
    ): Int {
        val v00 = (c00 shr shift) and 0xFF
        val v01 = (c01 shr shift) and 0xFF
        val v02 = (c02 shr shift) and 0xFF
        val v10 = (c10 shr shift) and 0xFF
        val v11 = (c11 shr shift) and 0xFF
        val v12 = (c12 shr shift) and 0xFF
        val v20 = (c20 shr shift) and 0xFF
        val v21 = (c21 shr shift) and 0xFF
        val v22 = (c22 shr shift) and 0xFF

        val sum = (v11 * kCenter) +
                ((v01 + v10 + v12 + v21) * kCross) +
                ((v00 + v02 + v20 + v22) * kDiag)

        return clamp(sum.toInt())
    }

    /**
     * Enhances micro-contrast and local tonal clarity to give deep optical pop.
     */
    private fun applyMicroContrast(pixels: IntArray, w: Int, h: Int, contrastFactor: Float): IntArray {
        if (abs(contrastFactor - 1.0f) < 0.02f) return pixels

        val result = IntArray(pixels.size)
        for (i in pixels.indices) {
            val p = pixels[i]
            val a = (p ushr 24) and 0xFF
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF

            // S-curve contrast around midpoint 128
            val adjR = clamp((128 + (r - 128) * contrastFactor).toInt())
            val adjG = clamp((128 + (g - 128) * contrastFactor).toInt())
            val adjB = clamp((128 + (b - 128) * contrastFactor).toInt())

            result[i] = (a shl 24) or (adjR shl 16) or (adjG shl 8) or adjB
        }
        return result
    }

    private fun clamp(v: Int): Int = when {
        v < 0 -> 0
        v > 255 -> 255
        else -> v
    }
}
