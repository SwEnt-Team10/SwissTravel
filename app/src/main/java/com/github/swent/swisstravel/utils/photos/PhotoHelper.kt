package com.github.swent.swisstravel.utils.photos

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import java.io.File
import java.io.FileOutputStream

// Code done by an AI
/**
 * A function for Context that get the meta data from a photo's uri and converts it to a Location.
 *
 * @param uri the uri of the image you want the location
 * @param name the name you want to assign to the location
 */
fun Context.getPhotoLocation(uri: Uri, name: String): Location? {
  val tempFile = File(cacheDir, "temp_gps_check.jpg")

  try {
    // 1. ON COPIE LE FICHIER CHEZ NOUS (C'est la clé !)
    // Cela contourne les problèmes de droits de lecture directe sur l'URI
    contentResolver.openInputStream(uri)?.use { input ->
      FileOutputStream(tempFile).use { output -> input.copyTo(output) }
    }

    // 2. ON LIT LES DONNÉES DEPUIS NOTRE COPIE LOCALE
    // Ici, plus de censure Android, c'est notre fichier.
    val exif = ExifInterface(tempFile.absolutePath)
    val latLong = FloatArray(2)

    if (exif.getLatLong(latLong)) {
      // Vérification anti-zéros
      if (latLong[0] == 0f && latLong[1] == 0f) {
        return null
      }

      return Location(
          coordinate =
              Coordinate(latitude = latLong[0].toDouble(), longitude = latLong[1].toDouble()),
          name = name)
    }
  } catch (e: Exception) {
    e.printStackTrace()
  } finally {
    // 3. MENAGE : On supprime le fichier temporaire
    if (tempFile.exists()) {
      tempFile.delete()
    }
  }
  return null
}
