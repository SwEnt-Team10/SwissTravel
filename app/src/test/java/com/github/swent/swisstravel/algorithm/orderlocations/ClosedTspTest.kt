package com.github.swent.swisstravel.algorithm.orderlocations

import org.junit.Assert.assertEquals
import org.junit.Test

class ClosedTspTest {

  private val closedTsp = ClosedTsp()

  @Test
  fun `closedTSP should return correct route`() {
    val dist =
        arrayOf(
            doubleArrayOf(0.0, 1.0, 10.0, 1.0),
            doubleArrayOf(1.0, 0.0, 1.0, 10.0),
            doubleArrayOf(10.0, 1.0, 0.0, 1.0),
            doubleArrayOf(1.0, 10.0, 1.0, 0.0))

    // Start and end at location 0
    val route = closedTsp.closedTsp(dist, 0)

    // The route should start and end at the same node
    assertEquals("Route should start at 0", 0, route.first())
    assertEquals("Route should end at 0", 0, route.last())

    // All nodes should be visited, plus the return to the start
    assertEquals("Route should visit all 4 nodes and return", 5, route.size)
    assertEquals("Route should contain 4 unique nodes", 4, route.distinct().size)

    // The optimal tour is 0 -> 1 -> 2 -> 3 -> 0
    val expectedRoute = listOf(0, 1, 2, 3, 0)
    assertEquals(expectedRoute, route)
  }
}
