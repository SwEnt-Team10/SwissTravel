package com.github.swent.swisstravel.model.trip

import org.junit.Assert.assertEquals
import org.junit.Test

class LocationTest {
  @Test
  fun checkHaversineDistanceLocation() {
    val l1 = Location(Coordinate(48.8606, 2.3376), "Le Louvre")
    val l2 = Location(Coordinate(46.522782, 6.565124), "EPFL")

    assertEquals(409.40, l1.haversineDistanceTo(l2), 0.1)
  }
}
