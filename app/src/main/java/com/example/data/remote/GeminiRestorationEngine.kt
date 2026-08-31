package com.example.data.remote

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.BuildConfig
import com.example.processor.AlgorithmicImageProcessor
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class GeminiGenerateRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null
)

data class GeminiContent(
    val parts: List<GeminiPart>
)

data class GeminiPart(
    val text: String? = null,
    val inlineData: GeminiInlineData? = null
)

data class GeminiInlineData(
    val mimeType: String,
    val data: String
)

data class GeminiGenerationConfig(
    val responseModalities: List<String>? = null,
    val imageConfig: GeminiImageConfig? = null,
    val temperature: Float? = null
)

data class GeminiImageConfig(
    val aspectRatio: String? = null,
    val imageSize: String? = null
)

data class GeminiGenerateResponse(
    val candidates: List<GeminiCandidate>? = null,
    val error: GeminiErrorDetails? = null
)

data class GeminiCandidate(
    val content: GeminiContent? = null
)

data class GeminiErrorDetails(
    val code: Int? = null,
    val message: String? = null,
    val status: String? = null
)

object GeminiRestorationEngine {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun isApiKeyConfigured(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return !key.isNullOrBlank() && key != "MY_GEMINI_API_KEY"
    }

    /**
     * Sends the out-of-focus / blurry photo to Gemini 2.5 Flash Image editing model
     * to reconstruct sharp details, restore focus, and eliminate noise.
     */
    suspend fun restoreWithGeminiAi(
        inputBitmap: Bitmap,
        focusPromptExtra: String = ""
    ): Result<Bitmap> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (!isApiKeyConfigured()) {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is not configured. Please set GEMINI_API_KEY in AI Studio Secrets panel.")
            )
        }

        try {
            // Compress bitmap to base64 jpeg
            val outputStream = ByteArrayOutputStream()
            inputBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            val base64Data = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

            val width = inputBitmap.width
            val height = inputBitmap.height
            val ratio = width.toFloat() / height.toFloat()

            val targetAspectRatio = when {
                ratio in 0.95f..1.05f -> "1:1"
                ratio in 0.70f..0.85f -> "3:4"
                ratio in 1.20f..1.40f -> "4:3"
                ratio in 0.50f..0.65f -> "9:16"
                ratio in 1.55f..1.85f -> "16:9"
                ratio < 0.75f -> "9:16"
                ratio > 1.35f -> "16:9"
                else -> "1:1"
            }

            val prompt = buildString {
                append("Restore this blurry, out-of-focus photograph into a natural, sharp, high-quality 4K Ultra HD master photograph. ")
                append("CRITICAL EXPOSURE & COLOR PRESERVATION: Do NOT overexpose or boost global brightness. Strictly preserve the original exposure, natural lighting, shadow depth, white balance, and highlight details (skies, lights, skin highlights, and white objects must NOT become pure white or blown out). ")
                append("INTELLIGENT DEBLURRING & DETAIL RESTORATION: Accurately reverse lens defocus blur and camera motion smear. Bring out-of-focus elements into crisp, crystal clear focus without halos, edge ringing, or artificial outlines. ")
                append("NATURAL SKIN TONES & TEXTURES: Preserve authentic facial features, natural skin tones, hair strands, and organic micro-textures without plastic smoothing or oversaturation. ")
                append("NOISE REDUCTION: Suppress high ISO sensor grain and compression artifacts cleanly while retaining fine texture. ")
                if (focusPromptExtra.isNotBlank()) {
                    append(focusPromptExtra).append(" ")
                }
                append("Maintain exact original composition and aspect ratio.")
            }

            val requestObj = GeminiGenerateRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(
                            GeminiPart(text = prompt),
                            GeminiPart(
                                inlineData = GeminiInlineData(
                                    mimeType = "image/jpeg",
                                    data = base64Data
                                )
                            )
                        )
                    )
                ),
                generationConfig = GeminiGenerationConfig(
                    responseModalities = listOf("TEXT", "IMAGE"),
                    imageConfig = GeminiImageConfig(
                        aspectRatio = targetAspectRatio,
                        imageSize = "2K"
                    ),
                    temperature = 0.15f
                )
            )

            val jsonAdapter = moshi.adapter(GeminiGenerateRequest::class.java)
            val requestJson = jsonAdapter.toJson(requestObj)

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-image:generateContent?key=$apiKey"
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val request = Request.Builder()
                .url(url)
                .post(requestJson.toRequestBody(mediaType))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    RuntimeException("API Error (${response.code}): $responseBody")
                )
            }

            val responseAdapter = moshi.adapter(GeminiGenerateResponse::class.java)
            val geminiResponse = responseAdapter.fromJson(responseBody)

            val imagePart = geminiResponse?.candidates?.firstOrNull()
                ?.content?.parts?.firstOrNull { it.inlineData != null }

            val returnedBase64 = imagePart?.inlineData?.data
            if (returnedBase64 != null) {
                val decodedBytes = Base64.decode(returnedBase64, Base64.DEFAULT)
                val decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                if (decodedBitmap != null) {
                    val final4kBitmap = AlgorithmicImageProcessor.upscaleTo4kUltraHd(decodedBitmap)
                    return@withContext Result.success(final4kBitmap)
                }
            }

            Result.failure(RuntimeException("Model did not return a restored image."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
