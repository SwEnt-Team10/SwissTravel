package com.github.swent.swisstravel.algorithm.orderlocations

import android.content.Context
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.mapbox.api.matrix.v1.MapboxMatrix
import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

/** This test was created with the help of Gemini */
class E2EOrderLocations {

  // Locations for the test
  private val GVA = Location(Coordinate(46.2, 6.15), "Genève")
  private val LSN = Location(Coordinate(46.521111, 6.631111), "Lausanne")
  private val MTX = Location(Coordinate(46.433333, 6.916667), "Montreux")
  private val ITK = Location(Coordinate(46.683333, 7.85), "Interlaken")
  private val BSL = Location(Coordinate(47.566944, 7.583056), "Bâle")
  private val ZRH = Location(Coordinate(47.377778, 8.541111), "Zurich")
  private val BRN = Location(Coordinate(46.949167, 7.447222), "Berne")
  private val ZRT = Location(Coordinate(46.02, 7.746944), "Zermatt")

  // token used in the test
  private val token = "pk.this_is_a_dummy_token"

  private lateinit var mockWebServer: MockWebServer

  @Before
  fun setUp() {
    mockWebServer = MockWebServer()
    mockWebServer.start()
  }

  @After
  fun tearDown() {
    mockWebServer.shutdown()
  }

  @Test
  fun `full flow from asking matrix to computing best path`() {
    // 1. Prepare the mock response from the JSON file
    val jsonResponsePath = "src/test/resources/orderlocations/MapboxMatrixResponseE2E.json"
    val jsonContent = File(jsonResponsePath).readText()
    val mockResponse = MockResponse().setResponseCode(200).setBody(jsonContent)
    mockWebServer.enqueue(mockResponse)

    // 2. Mock the Android Context to return a dummy access token
    val mockContext = mockk<Context>(relaxed = true)
    every { mockContext.getString(R.string.mapbox_access_token) } returns token
    every { mockContext.getString(R.string.app_name) } returns "SwissTravelTest"

    // 3. Create a custom OkHttpClient. This will be our call factory.
    val testOkHttpClient = OkHttpClient.Builder().build()

    // 4. Create an anonymous object to override buildClient
    val mockedDurationMatrix =
        object : DurationMatrix(mockContext) {
          override fun buildClient(points: List<com.mapbox.geojson.Point>): MapboxMatrix {
            val matrixClient =
                MapboxMatrix.builder()
                    .accessToken(token)
                    .profile("driving")
                    .coordinates(points)
                    .baseUrl(mockWebServer.url("/").toString()) // Point to the mock server
                    .build()
            // Inject the test client here, AFTER building
            matrixClient.callFactory = testOkHttpClient
            return matrixClient
          }
        }

    // 5. Define the input coordinates and execute the method under test
    val locations = listOf(GVA, LSN, MTX, ITK, BSL, ZRH, BRN, ZRT)
    val coordinates = locations.map { it.coordinate }

    var resultDurations: Array<DoubleArray>? = null
    val latch = CountDownLatch(1) // To handle async callback

    mockedDurationMatrix.getDurations(coordinates) { durations ->
      resultDurations = durations
      latch.countDown() // Signal that the callback has completed
    }

    // Wait for the async callback to finish
    latch.await(2, TimeUnit.SECONDS)

    // 6. Verify the request sent to the mock server
    val recordedRequest = mockWebServer.takeRequest()
    assertNotNull("Request was not made to the mock server", recordedRequest)

    val expectedPath =
        "/directions-matrix/v1/mapbox/driving/6.15,46.2;6.631111,46.521111;6.916667,46.433333;7.85,46.683333;7.583056,47.566944;8.541111,47.377778;7.447222,46.949167;7.746944,46.02?access_token=${token}"
    assertEquals(expectedPath, recordedRequest.path)

    // 7. Assert the results from the callback
    assertNotNull("The returned duration matrix should not be null", resultDurations)
    assertEquals("The matrix should have 8 rows", 8, resultDurations?.size)
    assertEquals("The matrix should have 8 columns", 8, resultDurations?.get(0)?.size)

    // Spot-check a few values from the JSON to confirm parsing is correct
    val expectedFirstRow =
        doubleArrayOf(0.0, 4147.3, 5307.9, 9558.4, 12183.8, 12853.8, 7411.7, 11619.3)
    assertArrayEquals(
        "The first row of durations does not match", expectedFirstRow, resultDurations?.get(0), 0.1)

    val expectedSecondRow =
        doubleArrayOf(4340.3, 0.0, 2524.2, 7155.4, 9780.8, 10450.8, 5008.7, 8835.6)
    assertArrayEquals(
        "The second row of durations does not match",
        expectedSecondRow,
        resultDurations?.get(1),
        0.1)

    // 8. Define start and end points
    val startIndex = locations.indexOf(ZRH) // Zurich is at index 5
    val endIndex = locations.indexOf(GVA) // Geneva is at index 0
    assertEquals("Start index for Zurich should be 5", 5, startIndex)
    assertEquals("End index for Geneva should be 0", 0, endIndex)

    // 9. Compute the optimal path using OpenTsp
    val tsp = OpenTsp()
    val optimalPathIndices = tsp.openTsp(resultDurations!!, startIndex, endIndex)

    // 10. Verify the computed path
    val expectedPathIndices =
        listOf(
            locations.indexOf(ZRH), // 5
            locations.indexOf(BSL), // 4
            locations.indexOf(BRN), // 6
            locations.indexOf(ITK), // 3
            locations.indexOf(ZRT), // 7
            locations.indexOf(MTX), // 2
            locations.indexOf(LSN), // 1
            locations.indexOf(GVA) // 0
            )

    assertEquals(
        "The computed TSP path does not match the expected path.",
        expectedPathIndices,
        optimalPathIndices)
  }
}
