package com.github.swent.swisstravel.model.user

import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.RouteSegment
import com.github.swent.swisstravel.model.trip.TransportMode
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripProfile
import com.google.firebase.Timestamp
import junit.framework.TestCase.assertEquals
import kotlin.test.Test
import kotlin.test.assertNull

class StatsCalculatorTest {

  @Test
  fun computeStats_withEmptyList_returnsDefaultUserStats() {
    val stats = StatsCalculator.computeStats(emptyList())

    val defaultStats = UserStats()
    assertEquals(defaultStats.totalTrips, stats.totalTrips)
    assertEquals(defaultStats.totalTravelMinutes, stats.totalTravelMinutes)
    assertEquals(defaultStats.uniqueLocations, stats.uniqueLocations)
    assertEquals(defaultStats.longestRouteSegmentMin, stats.longestRouteSegmentMin)
    assertNull(stats.mostUsedTransportMode)
  }

  @Test
  fun computeStats_withMultipleTrips_aggregatesCorrectly() {
    // --- Arrange test data ---

    // Adjust Location(...) constructor if your Location model differs
    val locA = Location(name = "A", coordinate = Coordinate(latitude = 0.0, longitude = 0.0))
    val locB = Location(name = "B", coordinate = Coordinate(latitude = 1.0, longitude = 1.0))
    val locC = Location(name = "C", coordinate = Coordinate(latitude = 2.0, longitude = 2.0))
    val locD = Location(name = "D", coordinate = Coordinate(latitude = 3.0, longitude = 3.0))

    // Trip 1: 2 hours long, 2 segments (TRAIN)
    val trip1Profile =
        TripProfile(
            startDate = Timestamp(0, 0), // t = 0s
            endDate = Timestamp(2 * 3600L, 0) // t = 7200s => 2h
            )

    val trip1Segments =
        listOf(
            RouteSegment(
                from = locA,
                to = locB,
                durationMinutes = 30,
                transportMode = TransportMode.TRAIN,
                startDate = Timestamp(0, 0),
                endDate = Timestamp(30 * 60L, 0)),
            RouteSegment(
                from = locB,
                to = locC,
                durationMinutes = 90,
                transportMode = TransportMode.TRAIN,
                startDate = Timestamp(30 * 60L, 0),
                endDate = Timestamp(120 * 60L, 0)))

    val trip1 =
        Trip(
            uid = "trip1",
            name = "Trip 1",
            ownerId = "user1",
            locations = listOf(locA, locB, locC),
            routeSegments = trip1Segments,
            activities = emptyList(),
            tripProfile = trip1Profile,
            isFavorite = false,
            isCurrentTrip = false,
            listUri = emptyList(),
            collaboratorsId = emptyList())

    // Trip 2: 1.5 hours long, 1 segment (BUS)
    val trip2Profile =
        TripProfile(
            startDate = Timestamp(0, 0), endDate = Timestamp((1.5 * 3600).toLong(), 0) // 1.5h
            )

    val trip2Segments =
        listOf(
            RouteSegment(
                from = locC,
                to = locD,
                durationMinutes = 60,
                transportMode = TransportMode.BUS,
                startDate = Timestamp(0, 0),
                endDate = Timestamp(60 * 60L, 0)))

    val trip2 =
        Trip(
            uid = "trip2",
            name = "Trip 2",
            ownerId = "user1",
            locations = listOf(locC, locD),
            routeSegments = trip2Segments,
            activities = emptyList(),
            tripProfile = trip2Profile,
            isFavorite = false,
            isCurrentTrip = false,
            listUri = emptyList(),
            collaboratorsId = emptyList())

    val trips = listOf(trip1, trip2)

    // Expected values:
    // totalTrips = 2
    // totalTravelMinutes = (2.0h + 1.5h) * 60 = 210
    // uniqueLocations = {A,B,C,D} = 4
    // longestRouteSegmentMin = max(30, 90, 60) = 90
    // mostUsedTransportMode = TRAIN (2 segments vs 1 BUS)

    // --- Act ---
    val stats = StatsCalculator.computeStats(trips)

    // --- Assert ---
    assertEquals(2, stats.totalTrips)
    assertEquals(210, stats.totalTravelMinutes)
    assertEquals(4, stats.uniqueLocations)
    assertEquals(90, stats.longestRouteSegmentMin)
    assertEquals(TransportMode.TRAIN, stats.mostUsedTransportMode)
  }
}
