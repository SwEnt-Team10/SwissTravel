package com.android.swisstravel.data.trips

import com.github.swent.swisstravel.data.trips.Coordinate
import com.github.swent.swisstravel.data.trips.Location
import com.github.swent.swisstravel.data.trips.RouteSegment
import com.github.swent.swisstravel.data.trips.TransportMode
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
          distanceMeter = 1500.0,
          durationMinutes = 30.0,
          path = path,
          transportMode = mode)
    }

    val segment =
        RouteSegment(
            from = from,
            to = to,
            distanceMeter = 1500.0,
            durationMinutes = 30.0,
            path = path,
            transportMode = TransportMode.UNKNOWN)

    assertEquals(1.5, segment.getDistanceKm(), 0.0)
    assertEquals(0.5, segment.getDurationHours(), 0.0)
  }
}
