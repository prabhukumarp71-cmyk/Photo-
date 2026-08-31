package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class EnhancementStrength(
    val title: String,
    val description: String,
    val deblurMultiplier: Float,
    val sharpenMultiplier: Float,
    val denoiseMultiplier: Float
) {
    NATURAL(
        title = "Natural",
        description = "Subtle deblur & noise cleanup with 100% original exposure & skin tone preservation",
        deblurMultiplier = 0.55f,
        sharpenMultiplier = 0.6f,
        denoiseMultiplier = 0.45f
    ),
    BALANCED(
        title = "Balanced (Default)",
        description = "Optimal 4K clarity, edge-aware deblurring, highlight protection & natural contrast",
        deblurMultiplier = 0.85f,
        sharpenMultiplier = 0.95f,
        denoiseMultiplier = 0.65f
    ),
    STRONG(
        title = "Strong",
        description = "Maximum motion & defocus blur recovery with deep high-frequency detail reconstruction",
        deblurMultiplier = 1.25f,
        sharpenMultiplier = 1.35f,
        denoiseMultiplier = 0.85f
    )
}

enum class RestorationMode(
    val title: String,
    val subtitle: String,
    val iconName: String
) {
    NATURAL_4K_ULTRA(
        title = "Natural 4K Ultra HD",
        subtitle = "Intelligent deblurring & super-resolution with zero overexposure",
        iconName = "AutoAwesome"
    ),
    AI_DEEP_FOCUS(
        title = "AI Motion & Defocus Deblur",
        subtitle = "Restores out-of-focus blur into crystal clear focus without halos",
        iconName = "CenterFocusStrong"
    ),
    ALGO_DEBLUR_SHARPEN(
        title = "Edge-Aware Optical Deblur",
        subtitle = "Inverse PSF deconvolution & luminance-only unsharp masking",
        iconName = "ShutterSpeed"
    ),
    ALGO_DEEP_DENOISE(
        title = "Natural Noise Reduction",
        subtitle = "Suppresses sensor grain while preserving authentic textures & skin tones",
        iconName = "CleaningServices"
    ),
    MANUAL_STUDIO(
        title = "Pro Studio Controls",
        subtitle = "Precision sliders for deblur, noise reduction, and edge sharpness",
        iconName = "Tune"
    )
}

data class QualityMetrics(
    val sharpnessScore: Float = 0f,       // Focus/Sharpness index (higher = sharper)
    val noiseScore: Float = 0f,           // Noise index (lower = cleaner)
    val blurRadiusEstimate: Float = 0f,   // Estimated PSF blur radius in px
    val diagnosticSummary: String = "",   // e.g. "Severe defocus blur detected"
    val resolution: String = "",
    val exposureBalance: String = "Balanced (Safe)"
)

data class ManualEnhancementParams(
    val sharpenAmount: Float = 1.0f,          // 0.2 to 3.0 (controlled, non-ringing)
    val sharpenRadius: Float = 1.5f,          // 0.5 to 3.0
    val denoiseAmount: Float = 0.45f,         // 0.0 to 1.0
    val clarityContrast: Float = 1.05f,       // 0.9 to 1.3 (safe range, no highlight blowouts)
    val deblurInverseStrength: Float = 0.65f, // 0.0 to 1.0
    val target4kOutput: Boolean = true,       // Upscale to 4K UHD
    val strength: EnhancementStrength = EnhancementStrength.BALANCED
)

@Entity(tableName = "restorations")
data class RestorationRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val originalImagePath: String,
    val restoredImagePath: String,
    val mode: String,
    val sharpnessBefore: Float,
    val sharpnessAfter: Float,
    val noiseBefore: Float,
    val noiseAfter: Float,
    val processingTimeMs: Long,
    val isFavorite: Boolean = false
)
