package com.github.swent.swisstravel.model.image

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyOrder
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

/** Some tests were made with the help of AI */
@OptIn(ExperimentalCoroutinesApi::class)
class ImageHelperTest {

  private lateinit var mockContext: Context
  private lateinit var mockContentResolver: ContentResolver
  private lateinit var mockUri: Uri
  private lateinit var mockBitmap: Bitmap
  private lateinit var mockResizedBitmap: Bitmap

  private val testDispatcher = UnconfinedTestDispatcher()

  @Before
  fun setup() {
    // 1. Mock Android Static Classes
    mockkStatic(Bitmap::class)
    mockkStatic(BitmapFactory::class)
    mockkStatic(Base64::class)
    mockkStatic(Log::class)

    // 2. Setup common mocks
    mockContext = mockk()
    mockContentResolver = mockk()
    mockUri = mockk()
    mockBitmap = mockk(relaxed = true)
    mockResizedBitmap = mockk(relaxed = true)

    every { mockContext.contentResolver } returns mockContentResolver
    every { Log.e(any(), any(), any()) } returns 0
  }

  @After
  fun teardown() {
    unmockkAll()
  }

  /** Helper for setup */
  private fun setupMockBitmapProcess(original: Bitmap, resized: Bitmap) {
    // Stream Setup
    val dummyStream = ByteArrayInputStream(byteArrayOf(1))
    every { mockContentResolver.openInputStream(mockUri) } returns dummyStream

    // Decode Setup
    every { BitmapFactory.decodeStream(any()) } returns original

    // Scale Setup
    // If original is small enough, createScaledBitmap might not be called in logic,
    // but if it is called, return resized.
    every { Bitmap.createScaledBitmap(any(), any(), any(), any()) } returns resized

    // Compress Setup (Default success)
    every { resized.compress(Bitmap.CompressFormat.JPEG, any(), any()) } answers
        {
          // Write a small byte array so loop finishes immediately
          arg<OutputStream>(2).write(byteArrayOf(1, 2, 3))
          true
        }

    // Base64 Setup
    every { Base64.encodeToString(any(), Base64.NO_WRAP) } returns "default_base64"
  }

  @Test
  fun uriCompressedToBase64ReturnsNullIfLoadingFails() =
      runTest(testDispatcher) {
        // Given: ContentResolver throws exception
        every { mockContentResolver.openInputStream(mockUri) } throws Exception("File not found")

        // When
        val result = ImageHelper.uriCompressedToBase64(mockContext, mockUri, testDispatcher)

        // Then
        assertNull(result)
        verify { Log.e("ImageHelper", "Error loading bitmap from URI", any()) }
      }

  @Test
  fun uriCompressedToBase64ReturnsNullIfDecodingStreamReturnsNull() =
      runTest(testDispatcher) {
        // Given: Stream opens, but BitmapFactory returns null
        val dummyStream = ByteArrayInputStream(byteArrayOf(1, 2, 3))
        every { mockContentResolver.openInputStream(mockUri) } returns dummyStream
        every { BitmapFactory.decodeStream(any<InputStream>()) } returns null

        // When
        val result = ImageHelper.uriCompressedToBase64(mockContext, mockUri, testDispatcher)

        // Then
        assertNull(result)
      }

  @Test
  fun uriCompressedToBase64RecyclesOriginalBitmapIfResized() =
      runTest(testDispatcher) {
        // Given
        every { mockBitmap.width } returns 2000
        every { mockBitmap.height } returns 2000
        setupMockBitmapProcess(original = mockBitmap, resized = mockResizedBitmap)

        // When
        ImageHelper.uriCompressedToBase64(mockContext, mockUri, testDispatcher)

        // Then
        verify { mockBitmap.recycle() } // Should recycle the original
        verify(exactly = 0) { mockResizedBitmap.recycle() } // Should NOT recycle the result
      }

  @Test
  fun uriCompressedToBase64CompressesAndEncodesCorrectly() =
      runTest(testDispatcher) {
        // Given
        setupMockBitmapProcess(
            original = mockBitmap, resized = mockBitmap) // No resize needed scenario

        // Mock Base64
        every { Base64.encodeToString(any(), Base64.NO_WRAP) } returns "success_base64"

        // When
        val result = ImageHelper.uriCompressedToBase64(mockContext, mockUri, testDispatcher)

        // Then
        assertEquals("success_base64", result)
      }

  @Test
  fun compressBitmapReducesQualityUntilSizeIsSmallEnough() =
      runTest(testDispatcher) {
        // 1. Setup the Bitmap to be processed
        every { mockBitmap.width } returns 2000
        every { mockBitmap.height } returns 2000
        setupMockBitmapProcess(original = mockBitmap, resized = mockResizedBitmap)

        // 2. Define the Byte Arrays to simulate compression results
        val hugeArray = ByteArray(800_000) { 1 } // 800KB (Too big)
        val smallArray = ByteArray(500_000) { 2 } // 500KB (Good)

        // 3. Mock the compress behavior
        // First call (Quality 75): Write huge array
        every {
          mockResizedBitmap.compress(Bitmap.CompressFormat.JPEG, 75, any<OutputStream>())
        } answers
            {
              val stream = arg<OutputStream>(2)
              stream.write(hugeArray)
              true
            }

        // Second call (Quality 60): Write small array
        every {
          mockResizedBitmap.compress(Bitmap.CompressFormat.JPEG, 60, any<OutputStream>())
        } answers
            {
              val stream = arg<OutputStream>(2)
              stream.write(smallArray)
              true
            }

        // Mock Base64 to return a string based on input
        val slot = slot<ByteArray>()
        every { Base64.encodeToString(capture(slot), Base64.NO_WRAP) } returns "final_string"

        // When
        ImageHelper.uriCompressedToBase64(mockContext, mockUri, testDispatcher)

        // Then
        // Verify it was encoded with the SMALL array, not the huge one
        assertEquals(smallArray.size, slot.captured.size)
        // Verify we tried quality 75 then 60
        verifyOrder {
          mockResizedBitmap.compress(Bitmap.CompressFormat.JPEG, 75, any())
          mockResizedBitmap.compress(Bitmap.CompressFormat.JPEG, 60, any())
        }
      }

  @Test
  fun base64ToBitmapDecodesStringToBitmap() =
      runTest(testDispatcher) {
        // Given
        val base64Str = "someBase64Data"
        val decodedBytes = byteArrayOf(10, 20, 30)
        val expectedBitmap = mockk<Bitmap>()

        every { Base64.decode(base64Str, Base64.NO_WRAP) } returns decodedBytes
        every { BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size) } returns
            expectedBitmap

        // When
        val result = ImageHelper.base64ToBitmap(base64Str, testDispatcher)

        // Then
        assertEquals(expectedBitmap, result)
      }

  @Test
  fun scaleBitmapCalculatesCorrectDimensionsForLargeLandscapeImage() =
      runTest(testDispatcher) {
        // Given: 2000x1000 image (Landscape)
        val bigLandscape = mockk<Bitmap>()
        every { bigLandscape.width } returns 2000
        every { bigLandscape.height } returns 1000
        every { bigLandscape.recycle() } just Runs
        setupMockBitmapProcess(original = bigLandscape, resized = mockResizedBitmap)

        // When
        ImageHelper.uriCompressedToBase64(mockContext, mockUri, testDispatcher)

        // New Width = 1024. New Height = 1024 / 2 = 512. (Ratio = 2000/1000 = 2)
        verify { Bitmap.createScaledBitmap(bigLandscape, 1024, 512, true) }
      }

  @Test
  fun scaleBitmapCalculatesCorrectDimensionsForLargePortraitImage() =
      runTest(testDispatcher) {
        // Given: 1000x2000 image (Portrait)
        val bigPortrait = mockk<Bitmap>()
        every { bigPortrait.width } returns 1000
        every { bigPortrait.height } returns 2000
        every { bigPortrait.recycle() } just Runs
        setupMockBitmapProcess(original = bigPortrait, resized = mockResizedBitmap)

        // When
        ImageHelper.uriCompressedToBase64(mockContext, mockUri, testDispatcher)

        // Then: Max dim is 1024.
        // New Height = 1024. New Width = 512.
        verify { Bitmap.createScaledBitmap(bigPortrait, 512, 1024, true) }
      }
}
