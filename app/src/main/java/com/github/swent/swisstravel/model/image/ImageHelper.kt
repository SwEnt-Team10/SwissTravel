package com.github.swent.swisstravel.model.image

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri

/** Helper object for working with images. This object was made with the help of AI */
object ImageHelper {
  const val DEFAULT_IMAGE_QUALITY = 70

  /**
   * Converts a URI to a base64-encoded string.
   *
   * @param context The application context.
   * @param uri The URI to convert.
   * @return The base64-encoded string.
   */
  suspend fun uriToBase64(context: Context, uri: Uri): String {}

  /**
   * Converts a base64-encoded string to a bitmap.
   *
   * @param base64 The base64-encoded string to convert.
   * @return The converted bitmap.
   */
  fun base64ToBitmap(base64: String): Bitmap {}

  /**
   * Loads a bitmap from a URI.
   *
   * @param context The application context.
   * @param uri The URI to load the bitmap from.
   * @return The loaded bitmap.
   */
  private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {}

  /**
   * Compresses a bitmap to a base64-encoded string.
   *
   * @param bitmap The bitmap to compress.
   * @param quality The compression quality (0-100).
   * @return The compressed byte array.
   */
  private fun compressBitmap(bitmap: Bitmap, quality: Int = DEFAULT_IMAGE_QUALITY): ByteArray {}

  /**
   * Encodes a byte array to a base64-encoded string.
   *
   * @param bytes The byte array to encode.
   * @return The byte array to encode.
   */
  private fun encodeBase64(bytes: ByteArray): String {}

  /**
   * Decodes a base64-encoded string to a byte array.
   *
   * @param base64 The base64-encoded string to decode.
   * @return The decoded byte array.
   */
  private fun decodeBase64(base64: String): ByteArray {}
}
