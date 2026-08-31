package com.example.processor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class SamplePhotoItem(
    val id: String,
    val title: String,
    val description: String,
    val problemTag: String,
    val defaultMode: String
)

object SamplePhotoGenerator {

    val sampleList = listOf(
        SamplePhotoItem(
            id = "out_of_focus_portrait",
            title = "Out-of-Focus Portrait",
            description = "Subject shot with shallow depth of field miss. Face details are soft and out of focus.",
            problemTag = "Defocus Lens Blur",
            defaultMode = "AI_DEEP_FOCUS"
        ),
        SamplePhotoItem(
            id = "high_iso_night_city",
            title = "Low-Light Night City",
            description = "High ISO low-light urban scene with heavy chromatic noise and sensor grain.",
            problemTag = "Sensor Grain & Noise",
            defaultMode = "NATURAL_4K_ULTRA"
        ),
        SamplePhotoItem(
            id = "motion_blur_pet",
            title = "Action Motion Blur",
            description = "Quick movement created directional shutter blur on fine fur and whiskers.",
            problemTag = "Motion Smear",
            defaultMode = "ALGO_DEBLUR_SHARPEN"
        ),
        SamplePhotoItem(
            id = "blurry_document_text",
            title = "Out-of-Focus Text Document",
            description = "Close-up document capture with soft optical focus causing unreadable typography.",
            problemTag = "Soft Optics",
            defaultMode = "AI_DEEP_FOCUS"
        ),
        SamplePhotoItem(
            id = "macro_nature_flower",
            title = "Macro Blossom Petals",
            description = "Macro botanic shot with soft petal edges and hazy micro-contrast.",
            problemTag = "Hazy Contrast",
            defaultMode = "MANUAL_STUDIO"
        )
    )

    /**
     * Generates a realistic synthetic test image containing the designated optical imperfection.
     */
    fun generateSample(sampleId: String): Bitmap {
        val width = 720
        val height = 720
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (sampleId) {
            "out_of_focus_portrait" -> {
                drawOutOfFocusPortrait(canvas, width, height, paint)
                applyFastBlur(bitmap, radius = 7)
            }
            "high_iso_night_city" -> {
                drawHighIsoNightCity(canvas, width, height, paint)
                applySensorNoise(bitmap, noiseAmount = 35)
                applyFastBlur(bitmap, radius = 2)
            }
            "motion_blur_pet" -> {
                drawMotionBlurPet(canvas, width, height, paint)
                applyMotionBlur(bitmap, streakLength = 14)
            }
            "blurry_document_text" -> {
                drawBlurryDocument(canvas, width, height, paint)
                applyFastBlur(bitmap, radius = 6)
            }
            "macro_nature_flower" -> {
                drawMacroFlower(canvas, width, height, paint)
                applyFastBlur(bitmap, radius = 5)
            }
            else -> {
                drawOutOfFocusPortrait(canvas, width, height, paint)
                applyFastBlur(bitmap, radius = 6)
            }
        }

        return bitmap
    }

    private fun drawOutOfFocusPortrait(canvas: Canvas, w: Int, h: Int, paint: Paint) {
        // Background studio gradient
        val bgShader = RadialGradient(
            w * 0.5f, h * 0.4f, w * 0.7f,
            intArrayOf(Color.parseColor("#38274C"), Color.parseColor("#1B1228"), Color.parseColor("#0D0814")),
            null, Shader.TileMode.CLAMP
        )
        paint.shader = bgShader
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        paint.shader = null

        // Soft bokeh orbs in background
        val random = Random(42)
        for (i in 0..14) {
            val bx = random.nextFloat() * w
            val by = random.nextFloat() * h
            val br = 25f + random.nextFloat() * 45f
            paint.color = Color.argb(40, 180 + random.nextInt(75), 140 + random.nextInt(100), 255)
            canvas.drawCircle(bx, by, br, paint)
        }

        // Portrait Body & Shoulders
        paint.color = Color.parseColor("#2D3748")
        canvas.drawRoundRect(RectF(w * 0.15f, h * 0.65f, w * 0.85f, h * 1.1f), 120f, 120f, paint)

        // Neck
        paint.color = Color.parseColor("#DDA585")
        canvas.drawRect(w * 0.42f, h * 0.48f, w * 0.58f, h * 0.68f, paint)

        // Face Oval
        val faceRect = RectF(w * 0.28f, h * 0.20f, w * 0.72f, h * 0.56f)
        paint.color = Color.parseColor("#F5C3A5")
        canvas.drawOval(faceRect, paint)

        // Hair
        paint.color = Color.parseColor("#3B2219")
        canvas.drawArc(RectF(w * 0.22f, h * 0.12f, w * 0.78f, h * 0.45f), 170f, 200f, true, paint)
        canvas.drawRoundRect(RectF(w * 0.22f, h * 0.24f, w * 0.32f, h * 0.60f), 30f, 30f, paint)
        canvas.drawRoundRect(RectF(w * 0.68f, h * 0.24f, w * 0.78f, h * 0.60f), 30f, 30f, paint)

        // Eyes (Soft & Out of Focus)
        paint.color = Color.parseColor("#4A2E2B")
        canvas.drawOval(RectF(w * 0.37f, h * 0.35f, w * 0.45f, h * 0.39f), paint)
        canvas.drawOval(RectF(w * 0.55f, h * 0.35f, w * 0.63f, h * 0.39f), paint)

        // Eyebrows
        paint.color = Color.parseColor("#3B2219")
        paint.strokeWidth = 6f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(w * 0.36f, h * 0.33f, w * 0.46f, h * 0.32f, paint)
        canvas.drawLine(w * 0.54f, h * 0.32f, w * 0.64f, h * 0.33f, paint)
        paint.style = Paint.Style.FILL

        // Nose shadow
        paint.color = Color.parseColor("#E2A98A")
        canvas.drawOval(RectF(w * 0.47f, h * 0.38f, w * 0.53f, h * 0.44f), paint)

        // Lips
        paint.color = Color.parseColor("#D96B6B")
        canvas.drawRoundRect(RectF(w * 0.43f, h * 0.47f, w * 0.57f, h * 0.51f), 15f, 15f, paint)
    }

    private fun drawHighIsoNightCity(canvas: Canvas, w: Int, h: Int, paint: Paint) {
        // Dark Night Sky
        val skyShader = LinearGradient(0f, 0f, 0f, h.toFloat(), Color.parseColor("#070B19"), Color.parseColor("#171D38"), Shader.TileMode.CLAMP)
        paint.shader = skyShader
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        paint.shader = null

        // Distant Skyline silhouettes
        paint.color = Color.parseColor("#131B2E")
        canvas.drawRect(w * 0.05f, h * 0.40f, w * 0.25f, h.toFloat(), paint)
        canvas.drawRect(w * 0.22f, h * 0.30f, w * 0.45f, h.toFloat(), paint)
        canvas.drawRect(w * 0.42f, h * 0.22f, w * 0.65f, h.toFloat(), paint)
        canvas.drawRect(w * 0.62f, h * 0.35f, w * 0.82f, h.toFloat(), paint)
        canvas.drawRect(w * 0.80f, h * 0.45f, w * 0.95f, h.toFloat(), paint)

        // Glowing Windows
        val rand = Random(101)
        for (y in (h * 0.32).toInt()..(h * 0.75).toInt() step 24) {
            for (x in (w * 0.1).toInt()..(w * 0.9).toInt() step 20) {
                if (rand.nextFloat() > 0.45f) {
                    paint.color = if (rand.nextBoolean()) Color.parseColor("#FDE047") else Color.parseColor("#38BDF8")
                    paint.alpha = 140 + rand.nextInt(110)
                    canvas.drawRect(x.toFloat(), y.toFloat(), x + 10f, y + 14f, paint)
                }
            }
        }

        // Bridge & Street Lights
        paint.color = Color.parseColor("#F97316")
        for (i in 0..8) {
            val lx = w * 0.1f + i * (w * 0.8f / 8)
            val ly = h * 0.78f
            canvas.drawCircle(lx, ly, 7f, paint)
        }
    }

    private fun drawMotionBlurPet(canvas: Canvas, w: Int, h: Int, paint: Paint) {
        // Green Grass Field
        val bgShader = LinearGradient(0f, 0f, 0f, h.toFloat(), Color.parseColor("#2E5A27"), Color.parseColor("#1B3C16"), Shader.TileMode.CLAMP)
        paint.shader = bgShader
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        paint.shader = null

        // Pet Body (Golden fur)
        paint.color = Color.parseColor("#E5A93C")
        canvas.drawOval(RectF(w * 0.25f, h * 0.35f, w * 0.75f, h * 0.70f), paint)

        // Head
        paint.color = Color.parseColor("#F5BC53")
        canvas.drawCircle(w * 0.65f, h * 0.40f, w * 0.18f, paint)

        // Ears
        paint.color = Color.parseColor("#C48421")
        val leftEar = Path().apply {
            moveTo(w * 0.56f, h * 0.28f)
            lineTo(w * 0.50f, h * 0.14f)
            lineTo(w * 0.64f, h * 0.24f)
            close()
        }
        val rightEar = Path().apply {
            moveTo(w * 0.70f, h * 0.26f)
            lineTo(w * 0.78f, h * 0.12f)
            lineTo(w * 0.80f, h * 0.28f)
            close()
        }
        canvas.drawPath(leftEar, paint)
        canvas.drawPath(rightEar, paint)

        // Snout & Nose
        paint.color = Color.parseColor("#FAF5E4")
        canvas.drawOval(RectF(w * 0.68f, h * 0.40f, w * 0.84f, h * 0.52f), paint)
        paint.color = Color.parseColor("#1F1510")
        canvas.drawCircle(w * 0.80f, h * 0.44f, 12f, paint)

        // Eyes
        paint.color = Color.parseColor("#27160C")
        canvas.drawCircle(w * 0.68f, h * 0.36f, 10f, paint)
    }

    private fun drawBlurryDocument(canvas: Canvas, w: Int, h: Int, paint: Paint) {
        // Paper background
        paint.color = Color.parseColor("#F8F9FA")
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

        // Document header line
        paint.color = Color.parseColor("#1E293B")
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textSize = 34f
        canvas.drawText("PROJECT SPECIFICATION & AUDIT", w * 0.08f, h * 0.15f, paint)

        paint.color = Color.parseColor("#3B82F6")
        canvas.drawRect(w * 0.08f, h * 0.18f, w * 0.92f, h * 0.19f, paint)

        // Body Text lines
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 22f
        paint.color = Color.parseColor("#334155")

        val lines = listOf(
            "Section 1.0: Optical Image Quality & Focus Diagnostics",
            "This document establishes the precision mathematical threshold for",
            "detecting lens blur point spread functions (PSF) in digital sensors.",
            "High-frequency detail preservation requires deconvolution filtering,",
            "adaptive unsharp masking, and neural super-resolution matrices.",
            "",
            "Section 2.0: Noise Reduction & Bilateral Kernel Synthesis",
            "Chroma grain in low-luminance zones must be suppressed without",
            "compromising sharp edge boundaries or typography legibility.",
            "Algorithm verified: Bilateral Spatial Filter v2.4 + Gemini Vision."
        )

        var curY = h * 0.26f
        for (line in lines) {
            if (line.isNotEmpty()) {
                canvas.drawText(line, w * 0.08f, curY, paint)
            }
            curY += 38f
        }
    }

    private fun drawMacroFlower(canvas: Canvas, w: Int, h: Int, paint: Paint) {
        // Soft gradient background
        val bgShader = RadialGradient(w * 0.5f, h * 0.5f, w * 0.7f,
            intArrayOf(Color.parseColor("#1B2A1E"), Color.parseColor("#0F1711")), null, Shader.TileMode.CLAMP)
        paint.shader = bgShader
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        paint.shader = null

        // Flower Petals (Magenta / Coral)
        val cx = w * 0.5f
        val cy = h * 0.5f
        val numPetals = 8
        for (i in 0 until numPetals) {
            val angle = i * (2 * Math.PI / numPetals)
            val px = cx + (cos(angle) * 110).toFloat()
            val py = cy + (sin(angle) * 110).toFloat()

            paint.color = if (i % 2 == 0) Color.parseColor("#EC4899") else Color.parseColor("#F43F5E")
            canvas.drawCircle(px, py, 95f, paint)
        }

        // Flower Center (Pollen Core)
        paint.color = Color.parseColor("#FBBF24")
        canvas.drawCircle(cx, cy, 55f, paint)

        paint.color = Color.parseColor("#D97706")
        for (i in 0..20) {
            val r = 10f + (i % 5) * 8f
            val a = i * 0.6f
            val dotX = cx + (cos(a) * r).toFloat()
            val dotY = cy + (sin(a) * r).toFloat()
            canvas.drawCircle(dotX, dotY, 4f, paint)
        }
    }

    /**
     * 2-pass separable box blur for realistic optical defocus.
     */
    private fun applyFastBlur(bitmap: Bitmap, radius: Int) {
        if (radius <= 0) return
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val temp = IntArray(w * h)

        // Horizontal pass
        for (y in 0 until h) {
            val yOffset = y * w
            for (x in 0 until w) {
                var r = 0; var g = 0; var b = 0; var count = 0
                val minX = kotlin.math.max(0, x - radius)
                val maxX = kotlin.math.min(w - 1, x + radius)
                for (ix in minX..maxX) {
                    val p = pixels[yOffset + ix]
                    r += (p shr 16) and 0xFF
                    g += (p shr 8) and 0xFF
                    b += p and 0xFF
                    count++
                }
                val a = (pixels[yOffset + x] ushr 24) and 0xFF
                temp[yOffset + x] = (a shl 24) or ((r / count) shl 16) or ((g / count) shl 8) or (b / count)
            }
        }

        // Vertical pass
        for (x in 0 until w) {
            for (y in 0 until h) {
                var r = 0; var g = 0; var b = 0; var count = 0
                val minY = kotlin.math.max(0, y - radius)
                val maxY = kotlin.math.min(h - 1, y + radius)
                for (iy in minY..maxY) {
                    val p = temp[iy * w + x]
                    r += (p shr 16) and 0xFF
                    g += (p shr 8) and 0xFF
                    b += p and 0xFF
                    count++
                }
                val a = (temp[y * w + x] ushr 24) and 0xFF
                pixels[y * w + x] = (a shl 24) or ((r / count) shl 16) or ((g / count) shl 8) or (b / count)
            }
        }

        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    /**
     * Applies directional horizontal motion smear to simulate camera/subject shake.
     */
    private fun applyMotionBlur(bitmap: Bitmap, streakLength: Int) {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        val result = IntArray(w * h)

        for (y in 0 until h) {
            val yOffset = y * w
            for (x in 0 until w) {
                var r = 0; var g = 0; var b = 0; var count = 0
                val maxX = kotlin.math.min(w - 1, x + streakLength)
                for (ix in x..maxX) {
                    val p = pixels[yOffset + ix]
                    r += (p shr 16) and 0xFF
                    g += (p shr 8) and 0xFF
                    b += p and 0xFF
                    count++
                }
                val a = (pixels[yOffset + x] ushr 24) and 0xFF
                result[yOffset + x] = (a shl 24) or ((r / count) shl 16) or ((g / count) shl 8) or (b / count)
            }
        }
        bitmap.setPixels(result, 0, w, 0, 0, w, h)
    }

    /**
     * Adds high ISO sensor grain.
     */
    private fun applySensorNoise(bitmap: Bitmap, noiseAmount: Int) {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        val rand = Random(456)

        for (i in pixels.indices) {
            val p = pixels[i]
            val a = (p ushr 24) and 0xFF
            var r = (p shr 16) and 0xFF
            var g = (p shr 8) and 0xFF
            var b = p and 0xFF

            val noise = rand.nextInt(-noiseAmount, noiseAmount)
            r = (r + noise).coerceIn(0, 255)
            g = (g + noise).coerceIn(0, 255)
            b = (b + noise + rand.nextInt(-5, 5)).coerceIn(0, 255)

            pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
    }
}
