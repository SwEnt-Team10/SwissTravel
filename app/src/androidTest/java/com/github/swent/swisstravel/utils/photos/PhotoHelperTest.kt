package com.github.swent.swisstravel.utils.photos

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
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
  fun getPhotoLocationReturnsCorrectCoordinatesAndNameWhenImageHasGpsData() {
    // Create a temporary dummy image
    val file = createTempJpegFile(context)

    // Inject GPS data (Example: Eiffel Tower)
    val lat = 48.8584
    val lon = 2.2945
    writeGeoLocationToImage(file, lat, lon)

    val locationName = "Eiffel Tower"

    // Call the helper function
    val uri = Uri.fromFile(file)
    val location = context.getPhotoLocation(uri, locationName)

    // Assertions
    assertNotNull("Location should not be null", location)
    assertEquals("Incorrect location name", locationName, location!!.name)
    assertEquals("Incorrect latitude", lat, location.coordinate.latitude, 0.01)
    assertEquals("Incorrect longitude", lon, location.coordinate.longitude, 0.01)

    // Cleanup
    file.delete()
  }

  @Test
  fun getPhotoLocationReturnsNullWhenImageHasNoGpsData() {
    val file = createTempJpegFile(context)
    val uri = Uri.fromFile(file)
    val location = context.getPhotoLocation(uri, "No GPS Location")

    assertNull("Location should be null for an image without GPS data", location)
    file.delete()
  }

  @Test
  fun getPhotoLocationReturnsNullWhenSecurityExceptionOccurs() {
    // 1. Create a relaxed mock
    val mockContext = mockk<Context>(relaxed = true)
    val mockContentResolver = mockk<ContentResolver>()
    val uri = Uri.parse("content://fake/uri")

    // 2. EXPLICITLY MOCK cacheDir to avoid NullPointerException in File constructor
    // We use the system's temporary directory as a dummy cache dir
    val tempDir = File(System.getProperty("java.io.tmpdir"))
    every { mockContext.cacheDir } returns tempDir

    // 3. Setup the mocked ContentResolver
    every { mockContext.contentResolver } returns mockContentResolver

    // 4. Force a SecurityException when opening the stream
    every { mockContentResolver.openInputStream(uri) } throws SecurityException("Permission denied")

    // Test execution
    val location = mockContext.getPhotoLocation(uri, "Test Security")

    // Verification
    assertNull("Should handle SecurityException gracefully (catch block)", location)
  }

  @Test
  fun getPhotoLocationReturnsNullWhenIOExceptionOccurs() {
    // 1. Create a relaxed mock
    val mockContext = mockk<Context>(relaxed = true)
    val mockContentResolver = mockk<ContentResolver>()
    val uri = Uri.parse("content://fake/uri")

    // 2. EXPLICITLY MOCK cacheDir here as well
    val tempDir = File(System.getProperty("java.io.tmpdir"))
    every { mockContext.cacheDir } returns tempDir

    // 3. Setup the mocked ContentResolver
    every { mockContext.contentResolver } returns mockContentResolver

    // 4. Force an IOException when opening the stream
    every { mockContentResolver.openInputStream(uri) } throws IOException("Disk error simulation")

    // Test execution
    val location = mockContext.getPhotoLocation(uri, "Test IO")

    // Verification
    assertNull("Should handle IOException gracefully (catch block)", location)
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
