package com.github.swent.swisstravel.utils.photos

import android.content.Context
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException


// This file has been done by an AI

/**
 * Data class to store the meta data from an uri
 */
data class PhotoMetadata(
    val dateTime: String? = null,
    val orientation: Int? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val make: String? = null,
    val model: String? = null
)

/**
 * A helper class to extract meta data from an uri.
 *
 * @param context the context of the application
 */
class PhotoMetadataHelper(private val context: Context) {

    /**
     * Reads metadata from the given Uri asynchronously.
     *
     * @param uri the uri we want the meta data
     */
    suspend fun getMetadata(uri: Uri): PhotoMetadata? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)

                val latLong = FloatArray(2)
                val hasLatLong = exif.getLatLong(latLong)

                PhotoMetadata(
                    dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME),
                    orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_UNDEFINED
                    ),
                    latitude = if (hasLatLong) latLong[0].toDouble() else null,
                    longitude = if (hasLatLong) latLong[1].toDouble() else null,
                    make = exif.getAttribute(ExifInterface.TAG_MAKE),
                    model = exif.getAttribute(ExifInterface.TAG_MODEL)
                )
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}