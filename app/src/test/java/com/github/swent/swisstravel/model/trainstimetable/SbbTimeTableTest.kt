package com.github.swent.swisstravel.model.trainstimetable

import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import java.io.InputStreamReader
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SbbTimetable using MockWebServer to fake API responses.
 *
 * These tests where created with the help of Gemini.
 */
class SbbTimetableTest {

  private lateinit var timetable: SbbTimetable
  private lateinit var mockWebServer: MockWebServer

  // Define test locations
  private val zurichHB =
      Location(name = "Zürich HB", coordinate = Coordinate(latitude = 47.3779, longitude = 8.5402))
  private val bern =
      Location(name = "Bern", coordinate = Coordinate(latitude = 46.9480, longitude = 7.4474))
  private val invalidLocation =
      Location(name = "Invalid", coordinate = Coordinate(latitude = 0.0, longitude = 0.0))

  @Before
  fun setUp() {
    mockWebServer = MockWebServer()
    mockWebServer.start()

    // Create an instance of SbbTimetable that points to our mock server
    timetable = SbbTimetable(mockWebServer.url("/").toString())
  }

  @After
  fun tearDown() {
    mockWebServer.shutdown()
  }

  /**
   * Helper function to read a mock response file from the test resources for a local JVM test.
   *
   * @param fileName The name of the file in the resource directory (e.g.,
   *   "sbb/zurich-bern-mock.xml").
   * @return The content of the file as a String.
   */
  private fun readResourceFile(fileName: String): String {
    // Use the class loader to get the resource stream, which works in local JVM tests
    val inputStream = this.javaClass.classLoader?.getResourceAsStream(fileName)
    return InputStreamReader(inputStream).use { it.readText() }
  }

  @Test
  fun getFastestRoute_withValidLocations_returnsCorrectDuration() = runBlocking {
    val mockResponse =
        MockResponse().setResponseCode(200).setBody(readResourceFile("sbb/zurich-bern-mock.xml"))
    mockWebServer.enqueue(mockResponse)

    val durationSeconds = timetable.getFastestRoute(zurichHB, bern)

    // The duration in the mock file is PT56M, which is 56 * 60 = 3360 seconds.
    val expectedSeconds = 3360
    assertEquals(expectedSeconds, durationSeconds)
  }

  @Test
  fun getFastestRoute_withApiError_returnsMinusOne() = runBlocking {
    val mockResponse =
        MockResponse()
            .setResponseCode(200) // The API can return 200 OK but with an error payload
            .setBody(readResourceFile("sbb/error-response.xml"))
    mockWebServer.enqueue(mockResponse)

    val durationSeconds = timetable.getFastestRoute(zurichHB, invalidLocation)

    assertEquals(-1, durationSeconds)
  }

  @Test
  fun getFastestRoute_withHttpError_returnsMinusOne() = runBlocking {
    val mockResponse = MockResponse().setResponseCode(500).setBody("Server Error")
    mockWebServer.enqueue(mockResponse)

    val durationSeconds = timetable.getFastestRoute(zurichHB, bern)

    assertEquals(-1, durationSeconds)
  }

  @Test
  fun getDurationMatrix_computesCorrectly() = runBlocking {
    // Prepare mock responses for all required calls in the matrix
    val successResponse = readResourceFile("sbb/zurich-bern-mock.xml")

    // Mock API calls for a 2x2 matrix:
    // 0 -> 0 (skipped)
    // 0 -> 1 (Zurich -> Bern, success)
    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(successResponse))
    // 1 -> 0 (Bern -> Zurich, success)
    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(successResponse))
    // 1 -> 1 (skipped)

    val locations = listOf(zurichHB, bern)
    val matrix = timetable.getDurationMatrix(locations)

    // Assertions
    // Expected duration is 3360 seconds from the mock file
    val expectedDuration = 3360.0

    assertEquals("Matrix should have 2 rows.", 2, matrix.size)
    assertEquals("Diagonal element should be 0.0.", 0.0, matrix[0][0], 0.001)
    assertEquals(
        "Zurich to Bern duration should be correct.", expectedDuration, matrix[0][1], 0.001)
    assertEquals(
        "Bern to Zurich duration should be correct.", expectedDuration, matrix[1][0], 0.001)
  }
}
