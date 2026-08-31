package com.example.processor

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImageFileManager {

    /**
     * Saves a bitmap to internal app storage and returns the absolute file path.
     */
    suspend fun saveBitmapToInternalStorage(
        context: Context,
        bitmap: Bitmap,
        prefix: String = "photo"
    ): String = withContext(Dispatchers.IO) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val fileName = "${prefix}_${timeStamp}.jpg"
        val storageDir = File(context.filesDir, "restorations").apply { if (!exists()) mkdirs() }
        val file = File(storageDir, fileName)

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 96, out)
        }
        file.absolutePath
    }

    /**
     * Loads a bitmap from an internal storage file path.
     */
    suspend fun loadBitmapFromFile(filePath: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Loads a bitmap from a content Uri (e.g. Gallery / Photo picker).
     */
    suspend fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Exports a restored bitmap to the public Android Pictures/PhotoQuality gallery.
     */
    suspend fun exportToGallery(context: Context, bitmap: Bitmap, title: String = "Restored_Photo"): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val fileName = "${title}_${timeStamp}.jpg"

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PhotoQualityAI")
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(uri, contentValues, null, null)
                    }
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        }
}
