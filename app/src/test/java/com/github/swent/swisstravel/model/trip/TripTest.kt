package com.github.swent.swisstravel.model.trip

import com.github.swent.swisstravel.model.trip.activity.Activity
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.description

class TripTest {

  private val listActivities =
      listOf(
          Activity(
              startDate = Timestamp(1734000000, 0), // 09:20
              endDate = Timestamp(1734003600, 0), // 10:20
              location =
                  Location(name = "Jet d'eau de Genève", coordinate = Coordinate(46.2074, 6.1551)),
              description = ""),
          Activity(
              startDate = Timestamp(1734012600, 0), // 13:10 (after travel + pause)
              endDate = Timestamp(1734016200, 0), // 14:10
              location = Location(name = "Zoo de Zurich", coordinate = Coordinate(47.3850, 8.5736)),
              description = ""),
          Activity(
              startDate = Timestamp(1734028800, 0), // 17:20 (after 2 segments + pause)
              endDate = Timestamp(1734032400, 0), // 18:20
              location =
                  Location(
                      name = "Musée des Transports de Lucerne",
                      coordinate = Coordinate(47.0502, 8.3103)),
              description = ""))

  private val listRouteSegments =
      listOf(
          // Geneva → Zurich
          RouteSegment(
              from = listActivities[0].location,
              to = listActivities[1].location,
              distanceMeter = 278000, // ~278 km
              durationMinutes = 140,
              path = listOf(Coordinate(46.2074, 6.1551), Coordinate(47.3850, 8.5736)),
              transportMode = TransportMode.TRAIN,
              startDate = Timestamp(1734005400, 0), // 10:30 (30 min after Geneva activity)
              endDate = Timestamp(1734011400, 0) // 12:10
              ),

          // Zurich → Lucerne part 1 (train)
          RouteSegment(
              from = listActivities[1].location,
              to = Location(name = "Lucerne Station", coordinate = Coordinate(47.0503, 8.3102)),
              distanceMeter = 52000, // ~52 km
              durationMinutes = 45,
              path = listOf(Coordinate(47.3850, 8.5736), Coordinate(47.0503, 8.3102)),
              transportMode = TransportMode.TRAIN,
              startDate = Timestamp(1734017400, 0), // 14:30 (after Zurich activity + 20 min pause)
              endDate = Timestamp(1734020100, 0) // 15:15
              ),

          // Lucerne Station → Museum (bus or walk)
          RouteSegment(
              from = Location(name = "Lucerne Station", coordinate = Coordinate(47.0503, 8.3102)),
              to = listActivities[2].location,
              distanceMeter = 2200,
              durationMinutes = 20,
              path = listOf(Coordinate(47.0503, 8.3102), Coordinate(47.0502, 8.3103)),
              transportMode = TransportMode.BUS,
              startDate = Timestamp(1734021900, 0), // 15:45 (after 30-min pause)
              endDate = Timestamp(1734023100, 0) // 16:05
              ))

  private val orderedTripElements =
      listOf(
          TripElement.TripActivity(listActivities[0]), // Geneva activity
          TripElement.TripSegment(listRouteSegments[0]), // Geneva → Zurich
          TripElement.TripActivity(listActivities[1]), // Zurich activity
          TripElement.TripSegment(listRouteSegments[1]), // Zurich → Lucerne (part 1)
          TripElement.TripSegment(listRouteSegments[2]), // Lucerne station → museum
          TripElement.TripActivity(listActivities[2]) // Lucerne activity
          )

  private val tripProfile =
      TripProfile(
          startDate = Timestamp(1734000000, 0),
          endDate = Timestamp(1734032400, 0),
          preferredLocations = emptyList(),
          preferences = emptyList())

  private val trip =
      Trip(
          uid = "testUid",
          name = "testName",
          ownerId = "testOwner",
          locations = emptyList(),
          routeSegments = listRouteSegments,
          activities = listActivities,
          tripProfile = tripProfile,
          isFavorite = false)

  @Test
  fun testGetAllTripElementsOrdered() {
    assertEquals(trip.getAllTripElementsOrdered(), orderedTripElements)
  }

  @Test
  fun testGetAllTripElementsOrderedFromChosenTime() {
    val tripCropped = trip.getUpcomingTripElements(Timestamp(1734018400, 0), true)
    assertEquals(orderedTripElements.subList(3, orderedTripElements.size), tripCropped)
  }

  @Test
  fun testGetAllTripElementsOrderedAfterChosenTime() {
    val tripCropped = trip.getUpcomingTripElements(Timestamp(1734018400, 0), false)
    assertEquals(orderedTripElements.subList(4, orderedTripElements.size), tripCropped)
  }

  @Test
  fun testGetUpcomingTripElementsReturnEmptyList() {
    val tripCropped = trip.getUpcomingTripElements()
    assertEquals(emptyList<TripElement>(), tripCropped)
  }

  @Test
  fun testGetTotalTime() {
    val totalTime = trip.getTotalTime()
    assertEquals(totalTime, 9.0, 0.01)
  }
}
