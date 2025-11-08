package com.github.swent.swisstravel.model.trip

import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Test

class RouteSegmentTest {
  @Test
  fun routeSegmentWithAllTransportMode() {
    val from = Location(Coordinate(48.8606, 2.3376), "Le Louvre")
    val to = Location(Coordinate(46.522782, 6.565124), "EPFL")
    val path =
        listOf(
            from.coordinate,
            Coordinate(46.522782, 5.565124),
            Coordinate(46.822782, 3.565124),
            Coordinate(47.522782, 2.865124),
            to.coordinate)
    /* Code coverage for enum */
    TransportMode.values().forEach { mode ->
      RouteSegment(
          from = from,
          to = to,
          durationMinutes = 30,
          transportMode = mode,
          startDate = Timestamp.now(),
          endDate = Timestamp.now())
    }

    val segment =
        RouteSegment(
            from = from,
            to = to,
            durationMinutes = 30,
            transportMode = TransportMode.UNKNOWN,
            startDate = Timestamp.now(),
            endDate = Timestamp.now())

    assertEquals(0.5, segment.getDurationHours(), 0.0)
  }
}
