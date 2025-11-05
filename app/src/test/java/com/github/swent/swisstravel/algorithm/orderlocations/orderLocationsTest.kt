package com.github.swent.swisstravel.algorithm.orderlocations

import android.content.Context
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.slot
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * This test file verifies the real implementation of the `orderLocations` function. It uses mocking
 * to intercept the creation of `DurationMatrix`, preventing actual network calls and allowing the
 * tests to provide a controlled, pre-defined duration matrix. This ensures the function's internal
 * logic (data preparation, TSP solving, result construction) is tested correctly.
 *
 * Tests written with the help of ChatGPT.
 */
class OrderLocationsTest {

  // Mock context
  private lateinit var mockContext: Context

  // Test locations
  private val GVA = Location(Coordinate(46.199901, 6.149249), "Genève")
  private val LSN = Location(Coordinate(46.520984, 6.630845), "Lausanne")
  private val MTX = Location(Coordinate(46.433298, 6.916694), "Montreux")
  private val ITK = Location(Coordinate(46.68315, 7.850478), "Interlaken")
  private val BSL = Location(Coordinate(47.566941, 7.583029), "Bâle")
  private val ZRH = Location(Coordinate(47.37779, 8.541518), "Zurich")
  private val BRN = Location(Coordinate(46.949169, 7.447221), "Berne")
  private val ZRT = Location(Coordinate(46.026536, 7.751462), "Zermatt")
  private val LCN = Location(Coordinate(47.05, 8.3), "Lucerne")

  // The full list of locations, matching the order in the test JSON file
  private val allLocationsForJson = listOf(GVA, LSN, MTX, ITK, BSL, ZRH, BRN, ZRT, LCN)

  // Pre-parsed duration matrix from the JSON file
  private lateinit var sampleDurations: Array<DoubleArray>

  @Before
  fun setUp() {
    mockContext = mockk(relaxed = true)
    every { mockContext.getString(R.string.mapbox_access_token) } returns "pk.dummy_mapbox_token"

    // Parse the durations from the JSON file once for all tests
    val jsonResponsePath = "src/test/resources/orderlocations/MapboxMatrixResponse.json"
    val jsonContent = File(jsonResponsePath).readText()
    val durationsString =
        jsonContent.substringAfter("\"durations\":").substringBefore("]],").plus("]]")
    sampleDurations =
        durationsString
            .replace("[[", "")
            .replace("]]", "")
            .split("],[")
            .map { row -> row.split(",").map { it.trim().toDouble() }.toDoubleArray() }
            .toTypedArray()

    // Intercept the constructor of DurationMatrix for all tests in this class
    mockkConstructor(DurationMatrix::class)
  }

  /**
   * Helper function to mock the behavior of the `getDurations` method. It simulates the async
   * callback from DurationMatrix, using a subset of the pre-loaded `sampleDurations` matrix that
   * corresponds to the locations being tested.
   */
  private fun mockDurationMatrixCallback(locationsForTest: List<Location>) {
    // Create the sub-matrix that the real `DurationMatrix` would fetch
    val indices =
        locationsForTest.map { loc -> allLocationsForJson.indexOfFirst { it.name == loc.name } }
    val subMatrix =
        Array(locationsForTest.size) { i ->
          println()
          DoubleArray(locationsForTest.size) { j ->
            print(" | ${sampleDurations[indices[i]][indices[j]]/60}")
            sampleDurations[indices[i]][indices[j]]
          }
        }

    // Capture the callback function passed to getDurations
    val callbackSlot = slot<(Array<DoubleArray>?) -> Unit>()
    every { anyConstructed<DurationMatrix>().getDurations(any(), capture(callbackSlot)) } answers
        {
          // When getDurations is called, immediately invoke the captured callback with our test
          // data
          callbackSlot.captured.invoke(subMatrix)
        }
  }

  @Test
  fun `orderLocations computes optimal path from ZRH to GVA`() {
    val latch = CountDownLatch(1)
    var result: OrderedRoute? = null

    val start = ZRH
    val end = GVA
    val locationsToVisit = listOf(LSN)
    val allTestLocations = (locationsToVisit + listOf(start, end)).distinctBy { it.coordinate }

    // Setup the mock to return the correct duration matrix for these specific locations
    mockDurationMatrixCallback(allTestLocations)

    // Expected order and duration based on the TSP algorithm's output with our test data
    val expectedOrderedLocations = listOf(ZRH, LSN, GVA)
    val expectedTotalDuration = 23442.2

    // Call the REAL orderLocations function
    orderLocations(mockContext, locationsToVisit, start, end) {
      result = it
      latch.countDown()
    }

    latch.await(2, TimeUnit.SECONDS)

    assertEquals(
        "The ordered list of locations is incorrect.",
        expectedOrderedLocations.map { it.name },
        result?.orderedLocations?.map { it.name })
    assertEquals(
        "Total duration does not match expected.",
        expectedTotalDuration,
        result?.totalDuartion ?: -1.0,
        0.1)
  }

  @Test
  fun `orderLocations handles round trip when start and end are the same`() {
    val latch = CountDownLatch(1)
    var result: OrderedRoute? = null

    val startAndEnd = ZRH
    val locationsToVisit = listOf(LSN)
    val allTestLocations = (locationsToVisit + startAndEnd).distinctBy { it.coordinate }

    mockDurationMatrixCallback(allTestLocations)

    // Expected order based on TSP output for a round trip
    val expectedOrder = listOf(ZRH, LSN, ZRH)
    val expectedDuration = 20859.3

    // Call the REAL orderLocations function
    orderLocations(mockContext, locationsToVisit, startAndEnd, startAndEnd) {
      result = it
      latch.countDown()
    }

    latch.await(2, TimeUnit.SECONDS)

    assertEquals(
        "The round trip path is not correctly ordered.",
        expectedOrder.map { it.name },
        result?.orderedLocations?.map { it.name })
    assertEquals(
        "The start and end of the loop should be the same.",
        result?.orderedLocations?.first()?.name,
        result?.orderedLocations?.last()?.name)
    assertEquals(expectedDuration, result?.totalDuartion ?: -1.0, 0.1)
  }

  @Test
  fun `orderLocations handles empty location list`() {
    val latch = CountDownLatch(1)
    var result: OrderedRoute? = null
    val start = GVA
    val end = ZRH

    mockDurationMatrixCallback(listOf(start, end))

    // Call the REAL orderLocations function
    orderLocations(mockContext, emptyList(), start, end) {
      result = it
      latch.countDown()
    }

    latch.await(2, TimeUnit.SECONDS)

    // For a direct trip, the order is fixed and the duration comes from the matrix
    assertEquals(listOf(GVA, ZRH).map { it.name }, result?.orderedLocations?.map { it.name })
    assertEquals(12859.5, result?.totalDuartion ?: -1.0, 0.1)
  }

  @Test
  fun `orderLocations returns an empty route for a single location trip`() {
    val latch = CountDownLatch(1)
    var result: OrderedRoute? = null
    val location = GVA

    // No need to mock DurationMatrix as it should not be called for a single-location trip
    every { anyConstructed<DurationMatrix>().getDurations(any(), any()) } returns Unit

    // Call the REAL orderLocations function with an empty list and same start/end
    orderLocations(mockContext, emptyList(), location, location) {
      result = it
      latch.countDown()
    }

    latch.await(2, TimeUnit.SECONDS)

    assertEquals(listOf(location), result?.orderedLocations)
    assertEquals(0.0, result?.totalDuartion ?: -1.0, 0.0)
    assertEquals(emptyList<Double>(), result?.segmentDuration)
  }

  @Test
  fun `orderLocations returns an error state on null duration matrix`() {
    val latch = CountDownLatch(1)
    var result: OrderedRoute? = null

    // Mock the matrix call to return null, simulating an API failure
    val callbackSlot = slot<(Array<DoubleArray>?) -> Unit>()
    every { anyConstructed<DurationMatrix>().getDurations(any(), capture(callbackSlot)) } answers
        {
          callbackSlot.captured.invoke(null)
        }

    // Call the REAL orderLocations function
    orderLocations(mockContext, listOf(LSN), GVA, ZRH) {
      result = it
      latch.countDown()
    }

    latch.await(2, TimeUnit.SECONDS)

    // The function should propagate the error as an OrderedRoute with a totalDuration of -1.0
    assertEquals(-1.0, result?.totalDuartion ?: 0.0, 0.0)
    assertEquals(listOf(LSN), result?.orderedLocations)
  }
}
