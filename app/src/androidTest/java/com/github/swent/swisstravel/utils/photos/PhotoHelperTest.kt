package com.github.swent.swisstravel.utils.photos

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// AI helped generate this class
@RunWith(AndroidJUnit4::class)
class PhotoHelperTest {

  private lateinit var context: Context

  @Before
  fun setUp() {
    // Retrieve the context of the instrumentation test app
    context = InstrumentationRegistry.getInstrumentation().targetContext
  }

  @Test
  fun getPhotoLocation_returnsCorrectCoordinatesAndName_whenImageHasGpsData() {
    // Create a temporary dummy image
    val file = createTempJpegFile(context)

    // Inject GPS data (Example: Eiffel Tower)
    // Lat: 48.8584 N, Long: 2.2945 E
    val lat = 48.8584
    val lon = 2.2945
    writeGeoLocationToImage(file, lat, lon)

    // Define a name for the location
    val locationName = "Eiffel Tower"

    // Call the helper function with the name
    val uri = Uri.fromFile(file)
    val location = context.getPhotoLocation(uri, locationName)

    // Assertions
    assertNotNull("Location should not be null", location)

    // Check if the name is correctly assigned
    assertEquals("Incorrect location name", locationName, location!!.name)

    // We use a delta (0.01) because EXIF conversion loses a tiny bit of precision
    assertEquals("Incorrect latitude", lat, location.coordinate.latitude, 0.01)
    assertEquals("Incorrect longitude", lon, location.coordinate.longitude, 0.01)

    // Cleanup
    file.delete()
  }

  @Test
  fun getPhotoLocation_returnsNull_whenImageHasNoGpsData() {
    // Create an image without EXIF data
    val file = createTempJpegFile(context)

    // Call the helper function with a dummy name
    val uri = Uri.fromFile(file)
    val location = context.getPhotoLocation(uri, "No GPS Location")

    // Verify that the result is null
    assertNull("Location should be null for an image without GPS data", location)

    // Cleanup
    file.delete()
  }

  // --- Test Utilities ---

  private fun createTempJpegFile(context: Context): File {
    val file = File(context.cacheDir, "test_image_${System.currentTimeMillis()}.jpg")
    val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out) }
    return file
  }

  private fun writeGeoLocationToImage(file: File, latitude: Double, longitude: Double) {
    val exif = ExifInterface(file.absolutePath)
    exif.setLatLong(latitude, longitude)
    exif.saveAttributes()
  }
}
