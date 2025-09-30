package com.android.sample

import org.junit.Assert.assertEquals
import org.junit.Test

class PointTest {

  @Test
  fun checkSimpleDistance() {
    val p1 = Point(2.5, 4.0)
    val p2 = Point(5.5, 8.0)
    assertEquals(5.0, p1.distanceTo(p2), 0.01)
  }
}
