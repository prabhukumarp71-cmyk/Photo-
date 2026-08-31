package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RestorationMode(
    val title: String,
    val subtitle: String,
    val iconName: String
) {
    AI_DEEP_FOCUS(
        title = "AI Focus Restoration",
        subtitle = "Restores out-of-focus blur exactly to original optical sharpness",
        iconName = "CenterFocusStrong"
    ),
    AI_ULTRA_ENHANCE(
        title = "AI Super Clarity & Denoise",
        subtitle = "Sharpens blurry micro-textures & removes sensor noise",
        iconName = "AutoAwesome"
    ),
    ALGO_DEBLUR_SHARPEN(
        title = "Optical Deblur & Edge Boost",
        subtitle = "Mathematical inverse blur filter & unsharp mask",
        iconName = "ShutterSpeed"
    ),
    ALGO_DEEP_DENOISE(
        title = "Edge-Preserving Denoise",
        subtitle = "Bilateral grain suppression keeping key contours sharp",
        iconName = "CleaningServices"
    ),
    MANUAL_STUDIO(
        title = "Pro Algorithmic Studio",
        subtitle = "Fine-tune sharpness, noise filter, clarity, & deblur radius",
        iconName = "Tune"
    )
}

data class QualityMetrics(
    val sharpnessScore: Float = 0f,       // Focus/Sharpness index (higher = sharper)
    val noiseScore: Float = 0f,           // Noise index (lower = cleaner)
    val blurRadiusEstimate: Float = 0f,   // Estimated PSF blur radius in px
    val diagnosticSummary: String = "",   // e.g. "Severe defocus blur detected"
    val resolution: String = ""
)

data class ManualEnhancementParams(
    val sharpenAmount: Float = 1.6f,      // 0.0 to 4.0
    val sharpenRadius: Float = 1.8f,      // 0.5 to 5.0
    val denoiseAmount: Float = 0.5f,      // 0.0 to 1.0
    val clarityContrast: Float = 1.25f,   // 0.8 to 2.0
    val deblurInverseStrength: Float = 0.6f // 0.0 to 1.0
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
