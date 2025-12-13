package com.github.swent.swisstravel.model.trip

import com.github.swent.swisstravel.model.trip.activity.Activity
import com.google.firebase.Timestamp
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.mockito.MockitoAnnotations

/** Unit tests for TripRepositoryLocal (tests inspired by the bootcamp) */
class TripRepositoryLocalTest {
  private lateinit var tripRepositoryLocal: TripRepositoryLocal
  private val trip =
      Trip(
          uid = "1",
          name = "Test trip",
          ownerId = "10",
          locations =
              listOf(
                  Location(
                      coordinate = Coordinate(latitude = 10.0, longitude = 100.0),
                      name = "fake place1",
                      imageUrl = "fakeImage1")),
          routeSegments =
              listOf(
                  RouteSegment(
                      from =
                          Location(
                              coordinate = Coordinate(latitude = -12.0, longitude = 0.0),
                              name = "fake place2",
                              imageUrl = "fakeImage2"),
                      to =
                          Location(
                              coordinate = Coordinate(latitude = -28.0, longitude = 13.0),
                              name = "fake place3",
                              imageUrl = "fakeImage3"),
                      durationMinutes = 200,
                      transportMode = TransportMode.TRAIN,
                      startDate = Timestamp.now(),
                      endDate = Timestamp.now())),
          activities =
              listOf(
                  Activity(
                      startDate = Timestamp.now(),
                      endDate = Timestamp.now(),
                      location =
                          Location(
                              coordinate = Coordinate(latitude = 19.8, longitude = 243.0),
                              name = "fake place4",
                              imageUrl = "fakeImage4"),
                      description = "Of course",
                      imageUrls = listOf("yes", "No"),
                      estimatedTime = 30)),
          tripProfile =
              TripProfile(
                  startDate = Timestamp.now(),
                  endDate = Timestamp.now(),
                  preferredLocations = emptyList(),
                  preferences = emptyList(),
                  arrivalLocation =
                      Location(
                          coordinate = Coordinate(latitude = 0.1, longitude = 0.2),
                          name = "fake place5",
                          imageUrl = "fakeImage5"),
                  departureLocation =
                      Location(
                          coordinate = Coordinate(latitude = 0.3, longitude = 0.8),
                          name = "fake place6",
                          imageUrl = "fakeImage6")),
          isCurrentTrip = true,
          uriLocation = emptyMap(),
          collaboratorsId = emptyList())

  @Before
  fun setUp() {
    MockitoAnnotations.openMocks(this)

    tripRepositoryLocal = TripRepositoryLocal()
  }

  @Test
  fun correctlyGeneratesNewUID() {
    val uid = tripRepositoryLocal.getNewUid()
    assertTrue(uid.isNotEmpty())

    val anotherUid = tripRepositoryLocal.getNewUid()
    assertTrue(uid != anotherUid)
  }

  @Test
  fun addTripsSucceeds() = runTest {
    tripRepositoryLocal.addTrip(trip)
    val trips = tripRepositoryLocal.getAllTrips()
    assertTrue(trips.contains(trip))
    assertEquals(1, trips.size)

    val retrievedTrip = tripRepositoryLocal.getTrip(trip.uid)
    assertEquals(trip, retrievedTrip)
  }

  @Test
  fun updateTripSucceeds() = runTest {
    tripRepositoryLocal.addTrip(trip)

    val updatedTrip = trip.copy(name = "updated Trip")

    tripRepositoryLocal.editTrip(trip.uid, updatedTrip)

    val trips = tripRepositoryLocal.getAllTrips()
    assertTrue(trips.contains(updatedTrip))
    assertTrue(!trips.contains(trip))
    assertEquals(1, trips.size)
  }

  @Test
  fun updateTripFailWhenTripNotFound() {
    assertThrows(Exception::class.java) { runTest { tripRepositoryLocal.editTrip(trip.uid, trip) } }
  }

  @Test
  fun deleteTripByIdCallsOnSuccess() = runTest {
    tripRepositoryLocal.addTrip(trip)

    tripRepositoryLocal.deleteTrip(trip.uid)

    val trips = tripRepositoryLocal.getAllTrips()
    assertTrue(!trips.contains(trip))
    assertEquals(0, trips.size)

    assertThrows(Exception::class.java) { runBlocking { tripRepositoryLocal.getTrip(trip.uid) } }
  }

  @Test
  fun deleteTripByIdDeletesTheCorrectTrip() = runTest {
    val trip2 = trip.copy(uid = "2", name = "second trip")
    tripRepositoryLocal.addTrip(trip)
    tripRepositoryLocal.addTrip(trip2)

    tripRepositoryLocal.deleteTrip(trip.uid)

    val trips = tripRepositoryLocal.getAllTrips()
    assertTrue(!trips.contains(trip))
    assertTrue(trips.contains(trip2))
  }

  @Test
  fun deleteTripByIdCallsOnFailureWhenTripNotFound() {
    assertThrows(Exception::class.java) {
      runBlocking { tripRepositoryLocal.deleteTrip("non-existent-id") }
    }
  }
}
