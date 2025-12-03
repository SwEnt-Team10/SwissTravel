package com.github.swent.swisstravel.model.trip

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** Some tests are written with the help of AI. */
class LocationTest {

  private lateinit var geneva: Location
  private lateinit var lausanne: Location
  private lateinit var bern: Location
  private lateinit var zurich: Location

  private lateinit var testLocations: List<Location>

  @Before
  fun setUp() {
    // Approximate coordinates for Swiss cities
    geneva = Location(Coordinate(46.2044, 6.1432), "Geneva")
    lausanne = Location(Coordinate(46.5197, 6.6323), "Lausanne") // ~54km from Geneva
    bern = Location(Coordinate(46.9480, 7.4474), "Bern") // ~95km from Lausanne, ~130km from Geneva
    zurich = Location(Coordinate(47.3769, 8.5417), "Zurich") // ~96km from Bern

    testLocations = listOf(geneva, bern)
  }

  @Test
  fun checkHaversineDistanceLocation() {
    val l1 = Location(Coordinate(48.8606, 2.3376), "Le Louvre")
    val l2 = Location(Coordinate(46.522782, 6.565124), "EPFL")

    assertEquals(409.40, l1.haversineDistanceTo(l2), 0.1)
  }

  @Test
  fun `isWithinDistanceOfAny returns true when location is in range of one location in list`() {
    // Lausanne (~54km from Geneva) is within 50-100km of Bern (~95km)
    // but not Geneva. Should return true because of Bern.
    val minDistance = 50.0
    val maxDistance = 100.0
    assertTrue(lausanne.isWithinDistanceOfAny(testLocations, minDistance, maxDistance))
  }

  @Test
  fun `isWithinDistanceOfAny returns false when location is too close to all locations`() {
    // Lausanne is ~54km from Geneva and ~95km from Bern.
    // The range [60, 100] excludes Geneva (too close) but includes Bern.
    // Let's use a range where it is too close to both.
    val minDistance = 100.0
    val maxDistance = 150.0
    assertFalse(lausanne.isWithinDistanceOfAny(testLocations, minDistance, maxDistance))
  }

  @Test
  fun `isWithinDistanceOfAny returns false when location is too far from all locations`() {
    // Lausanne is ~54km from Geneva and ~95km from Bern.
    // It is too far for the range [10, 50].
    val minDistance = 10.0
    val maxDistance = 50.0
    assertFalse(lausanne.isWithinDistanceOfAny(testLocations, minDistance, maxDistance))
  }

  @Test
  fun `isWithinDistanceOfAny returns false on lower boundary edge case`() {
    // Use a location that is exactly minDistance away. Should return false as the check is strict
    // `>`.
    val slightlyFurtherThanLausanne =
        Location(Coordinate(46.9797, 6.9323), "Neuchâtel") // ~54km from Bern
    val minDistance = slightlyFurtherThanLausanne.haversineDistanceTo(bern)
    val maxDistance = minDistance + 20.0
    assertFalse(
        slightlyFurtherThanLausanne.isWithinDistanceOfAny(listOf(bern), minDistance, maxDistance))
  }

  @Test
  fun `isWithinDistanceOfAny returns false on upper boundary edge case`() {
    // Use a location that is exactly maxDistance away. Should return false as the check is strict
    // `<`.
    val slightlyFurtherThanLausanne =
        Location(Coordinate(46.9797, 6.9323), "Neuchâtel") // ~54km from Bern
    val minDistance = 20.0
    val maxDistance = slightlyFurtherThanLausanne.haversineDistanceTo(bern)
    assertFalse(
        slightlyFurtherThanLausanne.isWithinDistanceOfAny(listOf(bern), minDistance, maxDistance))
  }

  @Test
  fun `isWithinDistanceOfAny returns false for an empty list of locations`() {
    val emptyList = emptyList<Location>()
    val minDistance = 15.0
    val maxDistance = 50.0
    assertFalse(lausanne.isWithinDistanceOfAny(emptyList, minDistance, maxDistance))
  }

  @Test
  fun `isWithinDistanceOfAny returns true when multiple locations are in range`() {
    // Zurich is ~96km from Bern and ~170km from Geneva.
    // Lausanne is ~54km from Geneva and ~95km from Bern.
    // Both are within the [50, 100] range of at least one location in `testLocations`.
    val minDistance = 50.0
    val maxDistance = 100.0
    assertTrue(lausanne.isWithinDistanceOfAny(testLocations, minDistance, maxDistance))
    assertTrue(zurich.isWithinDistanceOfAny(testLocations, minDistance, maxDistance))
  }
}
