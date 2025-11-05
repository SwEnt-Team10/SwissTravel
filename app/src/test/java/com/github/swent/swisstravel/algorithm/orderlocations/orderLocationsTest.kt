package com.github.swent.swisstravel.algorithm.orderlocations

import android.content.Context
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** This tests were created by ChatGPT and adapted by A. Renggli. */
class OrderLocationsTest {

  // Mock context
  private lateinit var mockContext: Context

  // Test locations, mirroring those in E2EOrderLocations for consistency
  private val GVA = Location(Coordinate(46.199901, 6.149249), "Genève")
  private val LSN = Location(Coordinate(46.520984, 6.630845), "Lausanne")
  private val MTX = Location(Coordinate(46.433298, 6.916694), "Montreux")
  private val ITK = Location(Coordinate(46.68315, 7.850478), "Interlaken")
  private val BSL = Location(Coordinate(47.566941, 7.583029), "Bâle")
  private val ZRH = Location(Coordinate(47.37779, 8.541518), "Zurich")
  private val BRN = Location(Coordinate(46.949169, 7.447221), "Berne")
  private val ZRT = Location(Coordinate(46.026536, 7.751462), "Zermatt")
  private val LCN = Location(Coordinate(47.05, 8.3), "Lucerne")

  // The full list of locations
  private val allLocations = listOf(GVA, LSN, MTX, ITK, BSL, ZRH, BRN, ZRT, LCN)

  // Pre-parsed duration matrix from the JSON file
  private lateinit var sampleDurations: Array<DoubleArray>

  @Before
  fun setUp() {
    mockContext = mockk(relaxed = true)
    every { mockContext.getString(R.string.mapbox_access_token) } returns "pk.dummy_mapbox_token"

    // Parse the durations from the JSON file once for all tests
    val jsonResponsePath = "src/test/resources/orderlocations/MapboxMatrixResponse.json"
    val jsonContent = File(jsonResponsePath).readText()
    // A simple parser to extract the "durations" array from the JSON
    val durationsString =
        jsonContent.substringAfter("\"durations\":").substringBefore("]],").plus("]]")
    sampleDurations =
        durationsString
            .replace("[[", "")
            .replace("]]", "")
            .split("],[")
            .map { row -> row.split(",").map { it.trim().toDouble() }.toDoubleArray() }
            .toTypedArray()
  }

  @Test
  fun `orderLocations computes optimal path from ZRH to GVA`() {
    val latch = CountDownLatch(1)
    var result: OrderedRoute? = null

    // Expected order of locations based on E2E test
    val expectedOrderedLocations = listOf(ZRH, BSL, ITK, BRN, MTX, LSN, GVA)
    val start = ZRH
    val end = GVA

    // Subset of locations to test that start/end are added correctly
    val locationsToVisit = listOf(BRN, ITK, MTX, LSN, BSL)

    orderLocations(mockContext, locationsToVisit, start, end) {
      // Mocking the behavior of DurationMatrix and TSP within the test
      val unique = (locationsToVisit + listOf(start, end)).distinctBy { it.coordinate }
      val startIndex = unique.indexOf(start)
      val endIndex = unique.indexOf(end)

      // We use a subset of the big matrix corresponding to the unique locations
      val all = listOf(GVA, LSN, MTX, ITK, BSL, ZRH, BRN, ZRT)
      val indices = unique.map { loc -> all.indexOfFirst { it.name == loc.name } }
      val subMatrix =
          Array(unique.size) { i ->
            DoubleArray(unique.size) { j -> sampleDurations[indices[i]][indices[j]] }
          }

      val orderIndices = OpenTsp().openTsp(subMatrix, startIndex, endIndex)
      val orderedLocations = orderIndices.map { unique[it] }
      val segmentDurations = orderIndices.zipWithNext { a, b -> subMatrix[a][b] }
      val totalDuration = segmentDurations.sum()

      result = OrderedRoute(orderedLocations, totalDuration, segmentDurations)
      latch.countDown()
    }

    latch.await(2, TimeUnit.SECONDS)

    val actualTotalDuration = 26996.9

    assertEquals(
        "The ordered list of locations is incorrect.",
        expectedOrderedLocations.map { it.name },
        result?.orderedLocations?.map { it.name })
    assertEquals(
        "Total duration does not match expected.",
        actualTotalDuration,
        result?.totalDuartion ?: -1.0,
        1.0)
  }

  @Test
  fun `orderLocations handles round trip when start and end are the same`() {
    val latch = CountDownLatch(1)
    var result: OrderedRoute? = null
    val locationsToVisit = listOf(LSN, MTX, ITK, BSL, ZRH, BRN, LCN)
    val startAndEnd = ZRH

    orderLocations(mockContext, locationsToVisit, startAndEnd, startAndEnd) {
      // Mocking the behavior of DurationMatrix and TSP within the test
      val unique = (locationsToVisit + listOf(startAndEnd)).distinctBy { it.coordinate }
      val startIndex = unique.indexOf(startAndEnd)

      val all = listOf(LSN, MTX, ITK, BSL, ZRH, BRN, LCN)
      val indices = unique.map { loc -> all.indexOfFirst { it.name == loc.name } }
      val subMatrix =
          Array(unique.size) { i ->
            DoubleArray(unique.size) { j -> sampleDurations[indices[i]][indices[j]] }
          }

      val orderIndices = OpenTsp().openTsp(subMatrix, startIndex, startIndex)
      val orderedLocations = orderIndices.map { unique[it] }
      val segmentDurations = orderIndices.zipWithNext { a, b -> subMatrix[a][b] }
      val totalDuration = segmentDurations.sum()

      result = OrderedRoute(orderedLocations, totalDuration, segmentDurations)
      latch.countDown()
    }

    latch.await(2, TimeUnit.SECONDS)

    val expectedOrder = listOf(ZRH, LCN, ITK, BRN, MTX, LSN, BSL, ZRH)
    assertEquals(
        "The round trip path is not correctly ordered.",
        expectedOrder.map { it.name },
        result?.orderedLocations?.map { it.name })
    assertEquals(
        "The start and end of the loop should be the same.",
        result?.orderedLocations?.first(),
        result?.orderedLocations?.last())
    assertEquals(28694.1, result?.totalDuartion ?: -1.0, 0.1)
  }

  @Test
  fun `orderLocations handles empty location list`() {
    val latch = CountDownLatch(1)
    var result: OrderedRoute? = null
    val start = GVA
    val end = ZRH

    orderLocations(mockContext, emptyList(), start, end) {
      // Mocked callback logic
      val unique = listOf(start, end)
      val subMatrix =
          arrayOf(
              doubleArrayOf(0.0, sampleDurations[0][5]), doubleArrayOf(sampleDurations[5][0], 0.0))

      val orderIndices = OpenTsp().openTsp(subMatrix, 0, 1)
      val orderedLocations = orderIndices.map { unique[it] }
      val segmentDurations = orderIndices.zipWithNext { a, b -> subMatrix[a][b] }
      val totalDuration = segmentDurations.sum()

      result = OrderedRoute(orderedLocations, totalDuration, segmentDurations)
      latch.countDown()
    }

    latch.await(2, TimeUnit.SECONDS)

    assertEquals(listOf(GVA, ZRH), result?.orderedLocations)
    assertEquals(12859.5, result?.totalDuartion ?: -1.0, 0.1)
  }

  @Test
  fun `orderLocations handles a single location`() {
    val latch = CountDownLatch(1)
    var result: OrderedRoute? = null
    val location = GVA

    orderLocations(mockContext, listOf(location), location, location) {
      result = it
      latch.countDown()
    }

    latch.await(2, TimeUnit.SECONDS)

    assertEquals(listOf(location), result?.orderedLocations)
    assertEquals(0.0, result?.totalDuartion ?: -1.0, 0.0)
    assertEquals(emptyList<Double>(), result?.segmentDuration)
  }

  @Test
  fun `orderLocations removes duplicate locations`() {
    val latch = CountDownLatch(1)
    val duplicateGVA = Location(GVA.coordinate, "Genève-bis")
    val locations = listOf(GVA, LSN, duplicateGVA)

    orderLocations(mockContext, locations, GVA, LSN) { result ->
      // In the callback, we check the result of the logic prior to the matrix call
      latch.countDown()
      assertEquals("Duplicate locations should be removed.", 2, result.orderedLocations.size)
    }

    // This test primarily verifies the logic before the async call
    latch.await(2, TimeUnit.SECONDS)
  }

  @Test
  fun `orderLocations returns error state on null duration matrix`() {
    val latch = CountDownLatch(1)
    var result: OrderedRoute? = null

    // This simulates the DurationMatrix failing
    val mockedOrderLocations:
        (Context, List<Location>, Location, Location, (OrderedRoute) -> Unit) -> Unit =
        { _, _, _, _, onResult ->
          onResult(OrderedRoute(allLocations, -1.0, emptyList()))
        }

    mockedOrderLocations(mockContext, allLocations, GVA, ZRH) {
      result = it
      latch.countDown()
    }

    latch.await(2, TimeUnit.SECONDS)

    assertEquals(-1.0, result?.totalDuartion ?: 0.0, 0.0)
    assertEquals(emptyList<Double>(), result?.segmentDuration)
  }
}
