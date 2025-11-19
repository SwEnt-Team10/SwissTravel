package com.github.swent.swisstravel.model.trainstimetable

import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Instrumented test for SbbTimetable, performing real API calls.
 *
 * These tests are slow and depend on a live network connection and the availability of the Open
 * Transport Data API. They are meant for integration verification rather than regular unit testing.
 *
 * Pre-requisite: A valid API token must be present in local.properties as
 * `OPEN_TRANSPORT_DATA_TOKEN="your_token"`.
 */
class SbbTimetableTest {

  private lateinit var timetable: SbbTimetable

  // Define test locations
  private val zurichHB =
      Location(name = "Zürich HB", coordinate = Coordinate(latitude = 47.3779, longitude = 8.5402))
  private val bern =
      Location(name = "Bern", coordinate = Coordinate(latitude = 46.9480, longitude = 7.4474))
  private val geneva =
      Location(name = "Genève", coordinate = Coordinate(latitude = 46.2044, longitude = 6.1432))

  // An invalid location to test error handling
  private val middleOfLakeGeneva =
      Location(
          name = "Middle of Lake Geneva",
          coordinate = Coordinate(latitude = 46.4333, longitude = 6.5500))

  @Before
  fun setUp() {
    timetable = SbbTimetable()
  }

  /**
   * Tests the getFastestRoute function with a valid, common trip (Zurich to Bern). It verifies that
   * the returned duration is a plausible positive number.
   */
  @Test
  fun getFastestRoute_withValidLocations_returnsPositiveDurationInSeconds() = runBlocking {
    val durationSeconds = timetable.getFastestRoute(zurichHB, bern)

    // Print the result for easier debugging
    println("getFastestRoute(Zurich -> Bern) result: $durationSeconds seconds")

    // A typical train trip is ~56 mins (3360s) to 1.5 hours (5400s)
    val plausibleMinDuration = 3000
    val plausibleMaxDuration = 7200
    assertTrue(
        "Duration from Zurich to Bern ($durationSeconds s) should be a plausible positive number.",
        durationSeconds in plausibleMinDuration..plausibleMaxDuration)
  }

  /**
   * Tests that getFastestRoute returns -1 when no route can be found (e.g., from a valid station to
   * a location in the middle of a lake).
   */
  @Test
  fun getFastestRoute_withInvalidDestination_returnsMinusOne() = runBlocking {
    val durationSeconds = timetable.getFastestRoute(zurichHB, middleOfLakeGeneva)

    // Print the result
    println("getFastestRoute(Zurich -> Lake) result: $durationSeconds")

    assertEquals(
        "The duration should be -1 for a trip to an unreachable location.", -1, durationSeconds)
  }

  /**
   * Tests the getDurationMatrix function with a small list of locations. This test is slow due to
   * the built-in delay to respect API rate limits. It checks:
   * 1. The matrix dimensions are correct.
   * 2. Diagonal elements are 0.
   * 3. Off-diagonal elements contain plausible durations (positive numbers).
   * 4. A failed route (to the lake) is marked with -1.0.
   */
  @Test
  fun getDurationMatrix_computesCorrectly_withRateLimiting() = runBlocking {
    val locations = listOf(zurichHB, bern, geneva, middleOfLakeGeneva)
    val n = locations.size

    val matrix = timetable.getDurationMatrix(locations)

    // Print the resulting matrix for easy inspection
    println("Computed Duration Matrix (in seconds):")
    matrix.forEach { row -> println(row.joinToString(separator = "\t") { "%.1f".format(it) }) }

    // 1. Check matrix dimensions
    assertEquals("Matrix should have $n rows.", n, matrix.size)
    matrix.forEach { assertEquals("Each row should have $n columns.", n, it.size) }

    // 2. Check diagonal elements
    for (i in 0 until n) {
      assertEquals("Diagonal element matrix[$i][$i] should be 0.0.", 0.0, matrix[i][i], 0.001)
    }

    // 3. Check off-diagonal elements for valid routes (Zurich -> Bern)
    val zurichToBernSeconds = matrix[0][1]
    assertTrue(
        "Duration from Zurich to Bern should be a plausible positive number.",
        zurichToBernSeconds > 0)
    // A typical train trip is ~56 mins (3360s) to 1.5 hours (5400s)
    assertTrue(
        "Duration from Zurich to Bern ($zurichToBernSeconds s) seems implausible.",
        zurichToBernSeconds in 3000.0..7200.0)

    // 4. Check the failed route
    val bernToLakeSeconds = matrix[1][3]
    assertEquals(
        "Duration to an unreachable location should be -1.0.", -1.0, bernToLakeSeconds, 0.001)
  }
}
