package com.android.swisstravel.model.trip

import com.android.swisstravel.utils.FakeJwtGenerator
import com.android.swisstravel.utils.FirebaseEmulator
import com.android.swisstravel.utils.SwissTravelTest
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.model.trip.TripsRepositoryFirestore
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Before
import org.junit.Test

class TripsRepositoryEmulatorTest : SwissTravelTest() {
  private lateinit var repository: TripsRepositoryFirestore

  @Before
  override fun setUp() {
    super.setUp()
    FirebaseEmulator.clearFirestoreEmulator()
    repository = TripsRepositoryFirestore(Firebase.firestore)
  }

  @Test
  fun addTrip_and_getTrip_canBeRetrieved() = runBlocking {
    // Arrange: create and sign in a fake Google user
    val fakeIdToken = FakeJwtGenerator.createFakeGoogleIdToken("Tester", "tester@example.com")
    FirebaseEmulator.createGoogleUser(fakeIdToken)
    val credential = GoogleAuthProvider.getCredential(fakeIdToken, null)
    FirebaseEmulator.auth.signInWithCredential(credential).await()
    val uid = Firebase.auth.currentUser!!.uid

    // Build trip
    val tripId = repository.getNewUid()
    val now = Timestamp.now()
    val trip =
        Trip(
            uid = tripId,
            name = "Instrumented Test Trip",
            ownerId = uid,
            locations = listOf(Location(Coordinate(46.52, 6.57), "EPFL")),
            activities = emptyList(),
            tripProfile = TripProfile(now, now, emptyList(), emptyList()),
            routeSegments = emptyList())

    // Act
    repository.addTrip(trip)
    val fetched = repository.getTrip(tripId)

    // Assert
    assertEquals(tripId, fetched.uid)
    assertEquals("Instrumented Test Trip", fetched.name)
    assertEquals(uid, fetched.ownerId)
  }

  @Test
  fun getAllTrips_returnsTripsForCurrentUser() = runBlocking {
    // Arrange: sign in user
    val fakeIdToken = FakeJwtGenerator.createFakeGoogleIdToken("ListUser", "list@example.com")
    FirebaseEmulator.createGoogleUser(fakeIdToken)
    val credential = GoogleAuthProvider.getCredential(fakeIdToken, null)
    FirebaseEmulator.auth.signInWithCredential(credential).await()
    val uid = Firebase.auth.currentUser!!.uid

    val now = Timestamp.now()
    val trip1 =
        Trip(
            uid = repository.getNewUid(),
            name = "Trip One",
            ownerId = uid,
            locations = listOf(Location(Coordinate(0.0, 0.0), "A")),
            activities = emptyList(),
            tripProfile = TripProfile(now, now, emptyList(), emptyList()),
            routeSegments = emptyList())
    val trip2 =
        Trip(
            uid = repository.getNewUid(),
            name = "Trip Two",
            ownerId = uid,
            locations = listOf(Location(Coordinate(1.0, 1.0), "B")),
            activities = emptyList(),
            tripProfile = TripProfile(now, now, emptyList(), emptyList()),
            routeSegments = emptyList())

    repository.addTrip(trip1)
    repository.addTrip(trip2)

    // Act
    val all = repository.getAllTrips()

    // Assert
    val ids = all.map { it.uid }
    assertTrue(ids.contains(trip1.uid))
    assertTrue(ids.contains(trip2.uid))
  }

  @Test
  fun deleteTrip_removesTrip() =
      runBlocking<Unit> {
        // Arrange: sign in user and add trip
        val fakeIdToken = FakeJwtGenerator.createFakeGoogleIdToken("DelUser", "del@example.com")
        FirebaseEmulator.createGoogleUser(fakeIdToken)
        val credential = GoogleAuthProvider.getCredential(fakeIdToken, null)
        FirebaseEmulator.auth.signInWithCredential(credential).await()
        val uid = Firebase.auth.currentUser!!.uid

        val now = Timestamp.now()
        val trip =
            Trip(
                uid = repository.getNewUid(),
                name = "To Delete",
                ownerId = uid,
                locations = listOf(Location(Coordinate(2.0, 2.0), "C")),
                activities = emptyList(),
                tripProfile = TripProfile(now, now, emptyList(), emptyList()),
                routeSegments = emptyList())

        repository.addTrip(trip)

        // Act
        repository.deleteTrip(trip.uid)

        // Assert: getTrip should fail — repository.getTrip throws when not found
        assertFailsWith<Exception> { repository.getTrip(trip.uid) }
      }
}
