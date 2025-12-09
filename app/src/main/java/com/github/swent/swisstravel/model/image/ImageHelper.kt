package com.github.swent.swisstravel.model.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.core.graphics.scale
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Helper object for working with images. This object was partially made with the help of AI */
object ImageHelper {
  // These are to ensure we stay under the 1MB Firestore limit
  const val DEFAULT_MAX_DIMENSION = 1024
  // Base64 adds ~33% overhead, so our byte array must be smaller than ~750KB.
  // We set a safe limit of 700KB.
  private const val MAX_BYTE_SIZE = 716800

  /**
   * Converts a URI to a base64-encoded string. Performs Loading -> Resizing -> Compressing ->
   * Encoding.
   *
   * @param context The application context.
   * @param uri The URI to convert.
   * @param dispatcher The dispatcher to use for the conversion. Default is Dispatchers.IO.
   * @return The base64-encoded string.
   */
  suspend fun uriCompressedToBase64(
      context: Context,
      uri: Uri,
      dispatcher: CoroutineDispatcher = Dispatchers.IO
  ): String? =
      withContext(dispatcher) {
        val originalBitmap = loadBitmapFromUri(context, uri) ?: return@withContext null
        val resizedBitmap = scaleBitmap(originalBitmap)
        // To avoid taking up unnecessary memory
        if (originalBitmap != resizedBitmap) {
          originalBitmap.recycle()
        }
        val compressedBytes = compressBitmap(resizedBitmap)
        encodeBase64(compressedBytes)
      }

  /**
   * Converts a base64-encoded string to a bitmap.
   *
   * @param base64 The base64-encoded string to convert.
   * @param dispatcher The dispatcher to use for the conversion. Default is Dispatchers.Default.
   * @return The converted bitmap, or null if decoding fails.
   * @throws Exception if decoding fails.
   */
  suspend fun base64ToBitmap(
      base64: String,
      dispatcher: CoroutineDispatcher = Dispatchers.Default
  ): Bitmap? =
      withContext(dispatcher) {
        try {
          val decodedBytes = decodeBase64(base64)
          BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
          Log.e("ImageHelper", "Error converting base64 to bitmap", e)
          null
        }
      }

  /**
   * Loads a bitmap from a URI.
   *
   * @param context The application context.
   * @param uri The URI to load the bitmap from.
   * @return The loaded bitmap or null if loading fails.
   * @throws Exception if loading fails.
   */
  private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
      context.contentResolver.openInputStream(uri)?.use { inputStream ->
        BitmapFactory.decodeStream(inputStream)
      }
    } catch (e: Exception) {
      Log.e("ImageHelper", "Error loading bitmap from URI", e)
      null
    }
  }

  /**
   * Resizes the bitmap so strictly neither side is larger than MAX_DIMENSION
   *
   * @param bitmap The bitmap to resize
   * @param maxDimension The maximum dimension to resize to
   * @return The resized bitmap
   */
  private fun scaleBitmap(bitmap: Bitmap, maxDimension: Int = DEFAULT_MAX_DIMENSION): Bitmap {
    val width = bitmap.width
    val height = bitmap.height

    // If image is already small do nothing.
    if (width <= maxDimension && height <= maxDimension) return bitmap

    val ratio = width.toFloat() / height.toFloat()
    val newWidth: Int
    val newHeight: Int

    if (width > height) {
      newWidth = maxDimension
      newHeight = (newWidth / ratio).toInt()
    } else {
      newHeight = maxDimension
      newWidth = (newHeight * ratio).toInt()
    }

    return bitmap.scale(newWidth, newHeight)
  }

  /**
   * Compresses a bitmap to a base64-encoded string.
   *
   * @param bitmap The bitmap to compress.
   * @return The compressed byte array.
   * @throws Exception if the image cannot be compressed enough to fit the limit.
   */
  private fun compressBitmap(bitmap: Bitmap): ByteArray {
    var quality = 75 // Start with decent quality
    val stream = ByteArrayOutputStream()

    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
    var byteArray = stream.toByteArray()

    // If it's too big, drop quality by 15 and try again
    while (byteArray.size > MAX_BYTE_SIZE && quality > 15) {
      quality -= 15

      // Reset stream to avoid writing appended data
      stream.reset()
      bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
      byteArray = stream.toByteArray()
    }
    // If image is still too big after dropping quality, give up and throw.
    if (byteArray.size > MAX_BYTE_SIZE) {
      throw Exception("Image is too large (${byteArray.size} bytes) to be stored.")
    }
    return byteArray
  }

  /**
   * Encodes a byte array to a base64-encoded string.
   *
   * @param bytes The byte array to encode.
   * @return The byte array to encode.
   */
  private fun encodeBase64(bytes: ByteArray): String {
    // NO_WRAP to avoid newlines
    return Base64.encodeToString(bytes, Base64.NO_WRAP)
  }

  /**
   * Decodes a base64-encoded string to a byte array.
   *
   * @param base64 The base64-encoded string to decode.
   * @return The decoded byte array.
   */
  private fun decodeBase64(base64: String): ByteArray {
    return Base64.decode(base64, Base64.NO_WRAP)
  }
}
