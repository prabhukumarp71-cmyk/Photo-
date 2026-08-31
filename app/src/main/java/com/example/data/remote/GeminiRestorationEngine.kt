package com.example.data.remote

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.BuildConfig
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

            val prompt = buildString {
                append("Restore this blurry, out-of-focus photograph with extreme fidelity. ")
                append("De-blur camera motion blur and reverse optical lens defocus blur. ")
                append("Bring out-of-focus elements into crisp, crystal clear focus while strictly matching and preserving the exact original scene geometry, facial identity, natural skin texture, hair, color palette, lighting, and composition. ")
                append("Eliminate high ISO sensor noise, chroma grain, and compression artifacts. ")
                if (focusPromptExtra.isNotBlank()) {
                    append(focusPromptExtra).append(" ")
                }
                append("Output high resolution restored photo.")
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
                        aspectRatio = "1:1",
                        imageSize = "1K"
                    ),
                    temperature = 0.2f
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
                    return@withContext Result.success(decodedBitmap)
                }
            }

            Result.failure(RuntimeException("Model did not return a restored image."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
