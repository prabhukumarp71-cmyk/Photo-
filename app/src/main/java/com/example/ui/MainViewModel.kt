package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.RestorationRepository
import com.example.data.model.ManualEnhancementParams
import com.example.data.model.QualityMetrics
import com.example.data.model.RestorationMode
import com.example.data.model.RestorationRecord
import com.example.data.remote.GeminiRestorationEngine
import com.example.processor.AlgorithmicImageProcessor
import com.example.processor.ImageFileManager
import com.example.processor.PhotoQualityAnalyzer
import com.example.processor.SamplePhotoGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MainUiState(
    val originalBitmap: Bitmap? = null,
    val restoredBitmap: Bitmap? = null,
    val isProcessing: Boolean = false,
    val processingMessage: String = "",
    val selectedMode: RestorationMode = RestorationMode.AI_DEEP_FOCUS,
    val manualParams: ManualEnhancementParams = ManualEnhancementParams(),
    val beforeMetrics: QualityMetrics? = null,
    val afterMetrics: QualityMetrics? = null,
    val currentPhotoTitle: String = "Test Photo",
    val toastMessage: String? = null,
    val isGeminiAvailable: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RestorationRepository
    val historyList: StateFlow<List<RestorationRecord>>

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = RestorationRepository(database.restorationDao())
        historyList = repository.allRestorations.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        _uiState.value = _uiState.value.copy(
            isGeminiAvailable = GeminiRestorationEngine.isApiKeyConfigured()
        )

        // Load the default out-of-focus portrait sample initially so the app opens with a ready-to-test photo!
        loadSamplePhoto("out_of_focus_portrait")
    }

    fun loadSamplePhoto(sampleId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true, processingMessage = "Loading test sample...")
            val sampleItem = SamplePhotoGenerator.sampleList.find { it.id == sampleId }
            val bitmap = withContext(Dispatchers.Default) {
                SamplePhotoGenerator.generateSample(sampleId)
            }
            val metrics = withContext(Dispatchers.Default) {
                PhotoQualityAnalyzer.analyze(bitmap)
            }

            val defaultMode = when (sampleItem?.defaultMode) {
                "AI_DEEP_FOCUS" -> RestorationMode.AI_DEEP_FOCUS
                "AI_ULTRA_ENHANCE" -> RestorationMode.AI_ULTRA_ENHANCE
                "ALGO_DEBLUR_SHARPEN" -> RestorationMode.ALGO_DEBLUR_SHARPEN
                "ALGO_DEEP_DENOISE" -> RestorationMode.ALGO_DEEP_DENOISE
                "MANUAL_STUDIO" -> RestorationMode.MANUAL_STUDIO
                else -> RestorationMode.AI_DEEP_FOCUS
            }

            _uiState.value = _uiState.value.copy(
                originalBitmap = bitmap,
                restoredBitmap = null,
                beforeMetrics = metrics,
                afterMetrics = null,
                selectedMode = defaultMode,
                currentPhotoTitle = sampleItem?.title ?: "Sample Photo",
                isProcessing = false,
                processingMessage = ""
            )
        }
    }

    fun loadPhotoFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true, processingMessage = "Loading photo from device...")
            val bitmap = ImageFileManager.loadBitmapFromUri(context, uri)
            if (bitmap != null) {
                val metrics = withContext(Dispatchers.Default) {
                    PhotoQualityAnalyzer.analyze(bitmap)
                }
                _uiState.value = _uiState.value.copy(
                    originalBitmap = bitmap,
                    restoredBitmap = null,
                    beforeMetrics = metrics,
                    afterMetrics = null,
                    currentPhotoTitle = "Imported Photo",
                    isProcessing = false,
                    processingMessage = ""
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    toastMessage = "Failed to load selected photo"
                )
            }
        }
    }

    fun setMode(mode: RestorationMode) {
        _uiState.value = _uiState.value.copy(selectedMode = mode)
    }

    fun updateManualParams(params: ManualEnhancementParams) {
        _uiState.value = _uiState.value.copy(manualParams = params)
    }

    fun enhancePhoto(context: Context) {
        val original = _uiState.value.originalBitmap ?: return
        val currentMode = _uiState.value.selectedMode
        val manualParams = _uiState.value.manualParams

        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            _uiState.value = _uiState.value.copy(
                isProcessing = true,
                processingMessage = "Analyzing optical point spread function..."
            )

            delay(300)

            var restored: Bitmap? = null

            // Try AI if an AI mode is selected and API key is available
            if ((currentMode == RestorationMode.AI_DEEP_FOCUS || currentMode == RestorationMode.AI_ULTRA_ENHANCE) &&
                GeminiRestorationEngine.isApiKeyConfigured()
            ) {
                _uiState.value = _uiState.value.copy(processingMessage = "Executing Gemini AI Focus & Denoise Engine...")
                val extraPrompt = if (currentMode == RestorationMode.AI_DEEP_FOCUS) {
                    "Prioritize resolving out-of-focus optical blur, sharp eye details, iris reflections, and clean facial contours."
                } else {
                    "Prioritize deep sensor grain suppression, micro-texture recovery, and crisp contrast."
                }

                val aiResult = GeminiRestorationEngine.restoreWithGeminiAi(original, extraPrompt)
                if (aiResult.isSuccess) {
                    restored = aiResult.getOrNull()
                }
            }

            // Fallback or explicit Algorithmic pipeline
            if (restored == null) {
                _uiState.value = _uiState.value.copy(
                    processingMessage = "Applying high-precision optical deblur, bilateral denoise & unsharp mask..."
                )
                delay(200)
                restored = withContext(Dispatchers.Default) {
                    AlgorithmicImageProcessor.process(original, currentMode, manualParams)
                }
            }

            // Analyze after metrics
            val afterMetrics = withContext(Dispatchers.Default) {
                PhotoQualityAnalyzer.analyze(restored)
            }

            val processingTime = System.currentTimeMillis() - startTime

            // Save record to local Room database and file system
            try {
                val origPath = ImageFileManager.saveBitmapToInternalStorage(context, original, "orig")
                val restPath = ImageFileManager.saveBitmapToInternalStorage(context, restored, "restored")

                val record = RestorationRecord(
                    title = _uiState.value.currentPhotoTitle,
                    originalImagePath = origPath,
                    restoredImagePath = restPath,
                    mode = currentMode.title,
                    sharpnessBefore = _uiState.value.beforeMetrics?.sharpnessScore ?: 0f,
                    sharpnessAfter = afterMetrics.sharpnessScore,
                    noiseBefore = _uiState.value.beforeMetrics?.noiseScore ?: 0f,
                    noiseAfter = afterMetrics.noiseScore,
                    processingTimeMs = processingTime
                )
                repository.insert(record)
            } catch (e: Exception) {
                // Ignore save error
            }

            _uiState.value = _uiState.value.copy(
                restoredBitmap = restored,
                afterMetrics = afterMetrics,
                isProcessing = false,
                processingMessage = "",
                toastMessage = "✨ Photo restored successfully (+${((afterMetrics.sharpnessScore - (_uiState.value.beforeMetrics?.sharpnessScore ?: 1f))).toInt()}% sharpness)"
            )
        }
    }

    fun exportToGallery(context: Context) {
        val bitmap = _uiState.value.restoredBitmap ?: return
        viewModelScope.launch {
            val success = ImageFileManager.exportToGallery(context, bitmap, _uiState.value.currentPhotoTitle)
            _uiState.value = _uiState.value.copy(
                toastMessage = if (success) "Saved to Pictures/PhotoQualityAI!" else "Failed to export image"
            )
        }
    }

    fun loadFromHistoryRecord(record: RestorationRecord) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true, processingMessage = "Loading restored photo...")
            val orig = ImageFileManager.loadBitmapFromFile(record.originalImagePath)
            val rest = ImageFileManager.loadBitmapFromFile(record.restoredImagePath)

            if (orig != null && rest != null) {
                val beforeMetrics = PhotoQualityAnalyzer.analyze(orig)
                val afterMetrics = PhotoQualityAnalyzer.analyze(rest)

                _uiState.value = _uiState.value.copy(
                    originalBitmap = orig,
                    restoredBitmap = rest,
                    beforeMetrics = beforeMetrics,
                    afterMetrics = afterMetrics,
                    currentPhotoTitle = record.title,
                    isProcessing = false,
                    processingMessage = ""
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    toastMessage = "Unable to load record files"
                )
            }
        }
    }

    fun deleteHistoryRecord(record: RestorationRecord) {
        viewModelScope.launch {
            repository.delete(record)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun clearToast() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }
}
