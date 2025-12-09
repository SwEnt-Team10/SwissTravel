package com.github.swent.swisstravel.utils.photos

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import java.io.IOException

// Code done by an AI
fun Context.getPhotoLocation(uri: Uri, name: String): Location? {
  return try {
    // Open the data stream from the URI
    this.contentResolver.openInputStream(uri)?.use { inputStream ->
      val exif = ExifInterface(inputStream)

      // Array to store the results [lat, long]
      val latLong = FloatArray(2)

      // getLatLong returns true if the data exists
      if (exif.getLatLong(latLong)) {
        Location(
            coordinate =
                Coordinate(latitude = latLong[0].toDouble(), longitude = latLong[1].toDouble()),
            name = name)
      } else {
        null // No GPS data found
      }
    }
  } catch (e: IOException) {
    e.printStackTrace()
    null
  } catch (e: SecurityException) {
    e.printStackTrace()
    null
  }
}
