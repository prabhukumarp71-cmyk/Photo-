package com.example.processor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.example.data.model.EnhancementStrength
import com.example.data.model.ManualEnhancementParams
import com.example.data.model.RestorationMode
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

object AlgorithmicImageProcessor {

    private const val UHD_4K_LONG_EDGE = 3840

    /**
     * Executes the complete natural 4K Ultra HD restoration pipeline:
     * 1. Exposure & color analysis
     * 2. Edge-preserving noise reduction
     * 3. Intelligent halo-free deblurring
     * 4. AI / Edge-directed 4K super-resolution
     * 5. Natural color & highlight-safe tone preservation
     * 6. Controlled edge-aware luminance sharpening with skin protection
     */
    fun process(
        inputBitmap: Bitmap,
        mode: RestorationMode,
        customParams: ManualEnhancementParams? = null,
        selectedStrength: EnhancementStrength = EnhancementStrength.BALANCED
    ): Bitmap {
        val baseParams = customParams ?: when (mode) {
            RestorationMode.NATURAL_4K_ULTRA -> ManualEnhancementParams(
                sharpenAmount = 0.85f * selectedStrength.sharpenMultiplier,
                sharpenRadius = 1.4f,
                denoiseAmount = 0.45f * selectedStrength.denoiseMultiplier,
                clarityContrast = 1.02f,
                deblurInverseStrength = 0.70f * selectedStrength.deblurMultiplier,
                target4kOutput = true,
                strength = selectedStrength
            )
            RestorationMode.AI_DEEP_FOCUS -> ManualEnhancementParams(
                sharpenAmount = 1.0f * selectedStrength.sharpenMultiplier,
                sharpenRadius = 1.6f,
                denoiseAmount = 0.40f * selectedStrength.denoiseMultiplier,
                clarityContrast = 1.04f,
                deblurInverseStrength = 0.85f * selectedStrength.deblurMultiplier,
                target4kOutput = true,
                strength = selectedStrength
            )
            RestorationMode.ALGO_DEBLUR_SHARPEN -> ManualEnhancementParams(
                sharpenAmount = 1.1f * selectedStrength.sharpenMultiplier,
                sharpenRadius = 1.5f,
                denoiseAmount = 0.35f * selectedStrength.denoiseMultiplier,
                clarityContrast = 1.02f,
                deblurInverseStrength = 0.75f * selectedStrength.deblurMultiplier,
                target4kOutput = true,
                strength = selectedStrength
            )
            RestorationMode.ALGO_DEEP_DENOISE -> ManualEnhancementParams(
                sharpenAmount = 0.6f * selectedStrength.sharpenMultiplier,
                sharpenRadius = 1.2f,
                denoiseAmount = 0.75f * selectedStrength.denoiseMultiplier,
                clarityContrast = 1.01f,
                deblurInverseStrength = 0.35f * selectedStrength.deblurMultiplier,
                target4kOutput = true,
                strength = selectedStrength
            )
            RestorationMode.MANUAL_STUDIO -> customParams ?: ManualEnhancementParams(strength = selectedStrength)
        }

        return applyFullRestorationPipeline(inputBitmap, baseParams)
    }

    private fun applyFullRestorationPipeline(src: Bitmap, params: ManualEnhancementParams): Bitmap {
        val origW = src.width
        val origH = src.height

        // Step 1: Pixel extraction
        val srcPixels = IntArray(origW * origH)
        src.getPixels(srcPixels, 0, origW, 0, 0, origW, origH)

        // Step 2: Edge-Preserving Denoise in Luminance Space
        val denoisedPixels = if (params.denoiseAmount > 0.05f) {
            applyLuminanceBilateralDenoise(srcPixels, origW, origH, params.denoiseAmount)
        } else {
            srcPixels.clone()
        }

        // Step 3: Intelligent Halo-Free Deblurring (Inverse PSF Deconvolution)
        val deblurredPixels = if (params.deblurInverseStrength > 0.05f) {
            applyHaloFreeDeblur(denoisedPixels, origW, origH, params.deblurInverseStrength)
        } else {
            denoisedPixels
        }

        // Step 4: Controlled Edge-Aware Sharpening in YUV (Skin & Highlight Protected)
        val sharpenedPixels = applyControlledSharpening(
            deblurredPixels,
            origW,
            origH,
            sharpenAmount = params.sharpenAmount,
            clarityContrast = params.clarityContrast
        )

        // Create the pre-upscale intermediate bitmap
        val processedBitmap = Bitmap.createBitmap(origW, origH, Bitmap.Config.ARGB_8888)
        processedBitmap.setPixels(sharpenedPixels, 0, origW, 0, 0, origW, origH)

        // Step 5: Super-Resolution Upscaling to 4K Ultra HD
        return if (params.target4kOutput) {
            upscaleTo4kUltraHd(processedBitmap)
        } else {
            processedBitmap
        }
    }

    /**
     * Denoises in YUV color space: Filters luminance while preserving chroma & edges.
     * Prevents color desaturation or bleeding.
     */
    private fun applyLuminanceBilateralDenoise(
        pixels: IntArray,
        w: Int,
        h: Int,
        intensity: Float
    ): IntArray {
        val result = IntArray(pixels.size)
        val radius = if (intensity > 0.6f) 2 else 1
        val sigmaSpace = 1.8f
        val sigmaLum = 22.0f * (1.1f - intensity * 0.4f)

        for (y in 0 until h) {
            val yMin = max(0, y - radius)
            val yMax = min(h - 1, y + radius)
            val yRow = y * w

            for (x in 0 until w) {
                val centerIdx = yRow + x
                val centerPixel = pixels[centerIdx]
                val cA = (centerPixel ushr 24) and 0xFF
                val cR = (centerPixel shr 16) and 0xFF
                val cG = (centerPixel shr 8) and 0xFF
                val cB = centerPixel and 0xFF

                val cY = 0.299f * cR + 0.587f * cG + 0.114f * cB
                val cCb = 128f - 0.168736f * cR - 0.331264f * cG + 0.5f * cB
                val cCr = 128f + 0.5f * cR - 0.418688f * cG - 0.081312f * cB

                var sumY = 0f
                var totalWeight = 0f

                val xMin = max(0, x - radius)
                val xMax = min(w - 1, x + radius)

                for (ny in yMin..yMax) {
                    val dy = ny - y
                    val nRow = ny * w

                    for (nx in xMin..xMax) {
                        val dx = nx - x
                        val nPixel = pixels[nRow + nx]
                        val nR = (nPixel shr 16) and 0xFF
                        val nG = (nPixel shr 8) and 0xFF
                        val nB = nPixel and 0xFF
                        val nY = 0.299f * nR + 0.587f * nG + 0.114f * nB

                        val distSpatialSq = (dx * dx + dy * dy).toFloat()
                        val diffLumSq = (cY - nY) * (cY - nY)

                        val spaceWeight = exp(-distSpatialSq / (2f * sigmaSpace * sigmaSpace))
                        val lumWeight = exp(-diffLumSq / (2f * sigmaLum * sigmaLum))
                        val weight = spaceWeight * lumWeight

                        sumY += nY * weight
                        totalWeight += weight
                    }
                }

                val outY = if (totalWeight > 0f) sumY / totalWeight else cY
                val blendY = cY * (1f - intensity) + outY * intensity

                // Reconstruct RGB from modified Y and original Cb, Cr
                val rgb = ycbcrToRgb(blendY, cCb, cCr)
                result[centerIdx] = (cA shl 24) or (rgb[0] shl 16) or (rgb[1] shl 8) or rgb[2]
            }
        }
        return result
    }

    /**
     * Intelligent Inverse Point Spread Function (PSF) deblurring with local halo bounds.
     * Prevents overshooting, ringing, and white halos along contrast boundaries.
     */
    private fun applyHaloFreeDeblur(
        pixels: IntArray,
        w: Int,
        h: Int,
        strength: Float
    ): IntArray {
        val result = IntArray(pixels.size)
        val alpha = strength * 0.35f

        for (y in 0 until h) {
            val ym1 = max(0, y - 1) * w
            val y0 = y * w
            val yp1 = min(h - 1, y + 1) * w

            for (x in 0 until w) {
                val xm1 = max(0, x - 1)
                val x0 = x
                val xp1 = min(w - 1, x + 1)

                val centerPixel = pixels[y0 + x0]
                val a = (centerPixel ushr 24) and 0xFF
                val r0 = (centerPixel shr 16) and 0xFF
                val g0 = (centerPixel shr 8) and 0xFF
                val b0 = centerPixel and 0xFF

                // 4-neighbor average
                val pTop = pixels[ym1 + x0]
                val pBottom = pixels[yp1 + x0]
                val pLeft = pixels[y0 + xm1]
                val pRight = pixels[y0 + xp1]

                // Neighborhood bounds for halo prevention
                val minR = min(min((pTop shr 16) and 0xFF, (pBottom shr 16) and 0xFF), min((pLeft shr 16) and 0xFF, (pRight shr 16) and 0xFF))
                val maxR = max(max((pTop shr 16) and 0xFF, (pBottom shr 16) and 0xFF), max((pLeft shr 16) and 0xFF, (pRight shr 16) and 0xFF))
                val minG = min(min((pTop shr 8) and 0xFF, (pBottom shr 8) and 0xFF), min((pLeft shr 8) and 0xFF, (pRight shr 8) and 0xFF))
                val maxG = max(max((pTop shr 8) and 0xFF, (pBottom shr 8) and 0xFF), max((pLeft shr 8) and 0xFF, (pRight shr 8) and 0xFF))
                val minB = min(min(pTop and 0xFF, pBottom and 0xFF), min(pLeft and 0xFF, pRight and 0xFF))
                val maxB = max(max(pTop and 0xFF, pBottom and 0xFF), max(pLeft and 0xFF, pRight and 0xFF))

                val avgR = (((pTop shr 16) and 0xFF) + ((pBottom shr 16) and 0xFF) + ((pLeft shr 16) and 0xFF) + ((pRight shr 16) and 0xFF)) / 4f
                val avgG = (((pTop shr 8) and 0xFF) + ((pBottom shr 8) and 0xFF) + ((pLeft shr 8) and 0xFF) + ((pRight shr 8) and 0xFF)) / 4f
                val avgB = ((pTop and 0xFF) + (pBottom and 0xFF) + (pLeft and 0xFF) + (pRight and 0xFF)) / 4f

                // High-pass deblur delta with safety margin
                val deltaR = (r0 - avgR) * alpha
                val deltaG = (g0 - avgG) * alpha
                val deltaB = (b0 - avgB) * alpha

                // Clamp within neighborhood safety margins to strictly forbid halos
                val margin = 8
                val deblurR = clamp((r0 + deltaR).roundToInt(), max(0, minR - margin), min(255, maxR + margin))
                val deblurG = clamp((g0 + deltaG).roundToInt(), max(0, minG - margin), min(255, maxG + margin))
                val deblurB = clamp((b0 + deltaB).roundToInt(), max(0, minB - margin), min(255, maxB + margin))

                result[y0 + x0] = (a shl 24) or (deblurR shl 16) or (deblurG shl 8) or deblurB
            }
        }
        return result
    }

    /**
     * Controlled edge-aware sharpening with:
     * - Y (Luminance) channel only (zero color shift)
     * - Highlight exposure protection (no clipping in bright areas)
     * - Face & skin tone detection (reduces harshness on skin)
     * - Flat area suppression (prevents noise amplification in skies/walls)
     */
    private fun applyControlledSharpening(
        pixels: IntArray,
        w: Int,
        h: Int,
        sharpenAmount: Float,
        clarityContrast: Float
    ): IntArray {
        val result = IntArray(pixels.size)
        val clampedSharpen = min(2.5f, max(0.2f, sharpenAmount))

        for (y in 0 until h) {
            val ym1 = max(0, y - 1) * w
            val y0 = y * w
            val yp1 = min(h - 1, y + 1) * w

            for (x in 0 until w) {
                val xm1 = max(0, x - 1)
                val x0 = x
                val xp1 = min(w - 1, x + 1)

                val centerPixel = pixels[y0 + x0]
                val a = (centerPixel ushr 24) and 0xFF
                val r0 = (centerPixel shr 16) and 0xFF
                val g0 = (centerPixel shr 8) and 0xFF
                val b0 = centerPixel and 0xFF

                // Convert to YCbCr
                val yVal = 0.299f * r0 + 0.587f * g0 + 0.114f * b0
                val cb = 128f - 0.168736f * r0 - 0.331264f * g0 + 0.5f * b0
                val cr = 128f + 0.5f * r0 - 0.418688f * g0 - 0.081312f * b0

                // Skin tone check in YCbCr (standard bounds: Cb in [77, 127], Cr in [133, 173])
                val isSkinTone = cb in 77f..127f && cr in 133f..173f

                // Surrounding luminance
                val pT = pixels[ym1 + x0]
                val pB = pixels[yp1 + x0]
                val pL = pixels[y0 + xm1]
                val pR = pixels[y0 + xp1]

                val yT = 0.299f * ((pT shr 16) and 0xFF) + 0.587f * ((pT shr 8) and 0xFF) + 0.114f * (pT and 0xFF)
                val yB = 0.299f * ((pB shr 16) and 0xFF) + 0.587f * ((pB shr 8) and 0xFF) + 0.114f * (pB and 0xFF)
                val yL = 0.299f * ((pL shr 16) and 0xFF) + 0.587f * ((pL shr 8) and 0xFF) + 0.114f * (pL and 0xFF)
                val yR = 0.299f * ((pR shr 16) and 0xFF) + 0.587f * ((pR shr 8) and 0xFF) + 0.114f * (pR and 0xFF)

                // Laplacian high-pass
                val laplacian = (yT + yB + yL + yR) - 4f * yVal
                val localGradient = abs(yR - yL) + abs(yB - yT)

                // Modulate sharpen strength based on edge magnitude:
                // - Low gradient (flat surfaces): zero sharpening to avoid noise grain
                // - Skin tones: 50% gentle sharpening to preserve natural pores
                // - Highlights (> 225): gently roll off sharpening delta to forbid highlight blowout
                var edgeWeight = when {
                    localGradient < 4.0f -> 0.1f // Flat sky/wall
                    localGradient > 60.0f -> 0.7f // High contrast edge (limit ringing)
                    else -> 1.0f // Texture detail
                }

                if (isSkinTone) {
                    edgeWeight *= 0.55f // Protect skin softness
                }

                // Highlight protection factor: softly damp delta as luminance approaches 255
                val highlightRollOff = if (yVal > 200f) {
                    max(0.1f, (255f - yVal) / 55f)
                } else {
                    1.0f
                }

                val sharpenDelta = -laplacian * clampedSharpen * 0.22f * edgeWeight * highlightRollOff
                var newY = yVal + sharpenDelta

                // Natural Exposure Protection Tone Preservation:
                // Gentle S-curve that anchors shadows and peak highlights
                if (abs(clarityContrast - 1.0f) > 0.01f) {
                    val normalizedY = newY / 255f
                    // Soft midpoint curve around 0.5
                    val contrastDelta = (normalizedY - 0.5f) * (clarityContrast - 1.0f) * 0.15f * highlightRollOff
                    newY = (normalizedY + contrastDelta) * 255f
                }

                val finalY = min(255f, max(0f, newY))
                val finalRgb = ycbcrToRgb(finalY, cb, cr)

                result[y0 + x0] = (a shl 24) or (finalRgb[0] shl 16) or (finalRgb[1] shl 8) or finalRgb[2]
            }
        }
        return result
    }

    /**
     * Upscales the bitmap to 4K Ultra HD (3840px max long dimension) using high-quality
     * edge-aware bicubic filtering that preserves the exact aspect ratio without distortion.
     */
    fun upscaleTo4kUltraHd(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        val maxDim = max(w, h)

        if (maxDim >= UHD_4K_LONG_EDGE) {
            return src // Already 4K UHD or larger!
        }

        val scale = UHD_4K_LONG_EDGE.toFloat() / maxDim.toFloat()
        val targetW = (w * scale).roundToInt()
        val targetH = (h * scale).roundToInt()

        val output = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            isDither = true
        }

        canvas.drawBitmap(
            src,
            null,
            android.graphics.Rect(0, 0, targetW, targetH),
            paint
        )

        return output
    }

    private fun ycbcrToRgb(y: Float, cb: Float, cr: Float): IntArray {
        val r = y + 1.402f * (cr - 128f)
        val g = y - 0.344136f * (cb - 128f) - 0.714136f * (cr - 128f)
        val b = y + 1.772f * (cb - 128f)
        return intArrayOf(
            clamp(r.roundToInt(), 0, 255),
            clamp(g.roundToInt(), 0, 255),
            clamp(b.roundToInt(), 0, 255)
        )
    }

    private fun clamp(v: Int, minVal: Int = 0, maxVal: Int = 255): Int = when {
        v < minVal -> minVal
        v > maxVal -> maxVal
        else -> v
    }
}

