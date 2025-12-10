package com.github.swent.swisstravel.model.trip

import org.junit.Assert.assertEquals
import org.junit.Test

class CoordinateTest {
  @Test
  fun checkHaversineDistance() {
    val c1 = Coordinate(48.8606, 2.3376)
    val c2 = Coordinate(46.522782, 6.565124)

    assertEquals(409.40, c1.haversineDistanceTo(c2), 0.1)
  }
}
