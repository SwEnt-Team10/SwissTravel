package com.github.swent.swisstravel.model.trip

import com.github.swent.swisstravel.utils.FakeJwtGenerator
import com.github.swent.swisstravel.utils.FirebaseEmulator
import com.github.swent.swisstravel.utils.FirestoreSwissTravelTest
import com.google.firebase.Timestamp
import com.google.firebase.auth.GoogleAuthProvider
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Before
import org.junit.Test

class TripsRepositoryEmulatorTest : FirestoreSwissTravelTest() {

  override lateinit var repository: TripsRepositoryFirestore

  @Before
  override fun setUp() {
    super.setUp()
    // Always clear the Firestore and Auth emulators before each test
    FirebaseEmulator.clearFirestoreEmulator()
    FirebaseEmulator.clearAuthEmulator()

    // Initialize repository using emulator Firestore + Auth
    repository = createInitializedRepository() as TripsRepositoryFirestore
  }

  @Test
  fun addTrip_and_getTrip_canBeRetrieved() = runBlocking {
    // Arrange: create and sign in a fake Google user in emulator
    val fakeIdToken = FakeJwtGenerator.createFakeGoogleIdToken("Tester", "tester@example.com")
    FirebaseEmulator.createGoogleUser(fakeIdToken)
    val credential = GoogleAuthProvider.getCredential(fakeIdToken, null)
    FirebaseEmulator.auth.signInWithCredential(credential).await()
    val uid = FirebaseEmulator.auth.currentUser!!.uid

    // Build trip
    val tripId = repository.getNewUid()
    val now = Timestamp.now()
    val location = Location(Coordinate(0.0, 0.0), "")
    val trip =
        Trip(
            uid = tripId,
            name = "Instrumented Test Trip",
            ownerId = uid,
            locations = listOf(Location(Coordinate(46.52, 6.57), "EPFL")),
            activities = emptyList(),
            tripProfile = TripProfile(now, now, emptyList(), emptyList(), 1, 0, location, location),
            routeSegments = emptyList(),
            isFavorite = true,
            isCurrentTrip = false,
            listUri = emptyList(),
            collaboratorsId = emptyList())

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
    // Arrange: sign in fake user in emulator
    val fakeIdToken = FakeJwtGenerator.createFakeGoogleIdToken("ListUser", "list@example.com")
    FirebaseEmulator.createGoogleUser(fakeIdToken)
    val credential = GoogleAuthProvider.getCredential(fakeIdToken, null)
    FirebaseEmulator.auth.signInWithCredential(credential).await()
    val uid = FirebaseEmulator.auth.currentUser!!.uid

    val now = Timestamp.now()
    val location = Location(Coordinate(0.0, 0.0), "")
    val trip1 =
        Trip(
            uid = repository.getNewUid(),
            name = "Trip One",
            ownerId = uid,
            locations = listOf(Location(Coordinate(0.0, 0.0), "A")),
            activities = emptyList(),
            tripProfile = TripProfile(now, now, emptyList(), emptyList(), 1, 0, location, location),
            routeSegments = emptyList(),
            isFavorite = false,
            isCurrentTrip = false,
            listUri = emptyList(),
            collaboratorsId = emptyList())
    val trip2 =
        Trip(
            uid = repository.getNewUid(),
            name = "Trip Two",
            ownerId = uid,
            locations = listOf(Location(Coordinate(1.0, 1.0), "B")),
            activities = emptyList(),
            tripProfile = TripProfile(now, now, emptyList(), emptyList(), 1, 0, location, location),
            routeSegments = emptyList(),
            isFavorite = false,
            isCurrentTrip = false,
            listUri = emptyList(),
            collaboratorsId = emptyList())

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
  fun deleteTrip_removesTrip(): Unit = runBlocking {
    // Arrange
    val fakeIdToken = FakeJwtGenerator.createFakeGoogleIdToken("DelUser", "del@example.com")
    FirebaseEmulator.createGoogleUser(fakeIdToken)
    val credential = GoogleAuthProvider.getCredential(fakeIdToken, null)
    FirebaseEmulator.auth.signInWithCredential(credential).await()
    val uid = FirebaseEmulator.auth.currentUser!!.uid

    val now = Timestamp.now()
    val trip =
        Trip(
            uid = repository.getNewUid(),
            name = "To Delete",
            ownerId = uid,
            locations = listOf(Location(Coordinate(2.0, 2.0), "C")),
            activities = emptyList(),
            tripProfile = TripProfile(now, now, emptyList(), emptyList()),
            routeSegments = emptyList(),
            isFavorite = false,
            isCurrentTrip = false,
            listUri = emptyList(),
            collaboratorsId = emptyList())

    repository.addTrip(trip)

    // Act
    repository.deleteTrip(trip.uid)

    // Assert
    assertFailsWith<Exception> { repository.getTrip(trip.uid) }
  }

  @Test
  fun editTrip_updatesTrip() = runBlocking {
    // Arrange
    val fakeIdToken = FakeJwtGenerator.createFakeGoogleIdToken("EditUser", "edit@example.com")
    FirebaseEmulator.createGoogleUser(fakeIdToken)
    val credential = GoogleAuthProvider.getCredential(fakeIdToken, null)
    FirebaseEmulator.auth.signInWithCredential(credential).await()
    val uid = FirebaseEmulator.auth.currentUser!!.uid

    val now = Timestamp.now()
    val location = Location(Coordinate(0.0, 0.0), "Loc")
    val trip =
        Trip(
            uid = repository.getNewUid(),
            name = "Original Name",
            ownerId = uid,
            locations = emptyList(),
            activities = emptyList(),
            tripProfile = TripProfile(now, now, emptyList(), emptyList(), 1, 0, location, location),
            routeSegments = emptyList(),
            isFavorite = false,
            isCurrentTrip = false,
            listUri = emptyList(),
            collaboratorsId = emptyList())
    repository.addTrip(trip)

    // Act
    val updatedTrip = trip.copy(name = "Updated Name", isFavorite = true)
    repository.editTrip(trip.uid, updatedTrip)

    // Assert
    val fetched = repository.getTrip(trip.uid)
    assertEquals("Updated Name", fetched.name)
    assertTrue(fetched.isFavorite)
  }

  @Test
  fun getAllTrips_throwsIfUserNotLoggedIn() = runBlocking {
    // Arrange: ensure no user is signed in
    FirebaseEmulator.auth.signOut()

    // Act & Assert
    assertFailsWith<Exception> { repository.getAllTrips() }
    Unit
  }

  @Test
  fun getTrip_ThrowsForMalformedData() = runBlocking {
    // Arrange: create a document with missing fields manually
    val fakeIdToken = FakeJwtGenerator.createFakeGoogleIdToken("BadDataUser", "bad@example.com")
    FirebaseEmulator.createGoogleUser(fakeIdToken)
    val credential = GoogleAuthProvider.getCredential(fakeIdToken, null)
    FirebaseEmulator.auth.signInWithCredential(credential).await()

    val tripId = "malformed_trip"
    val badData =
        mapOf(
            "name" to "Bad Trip",
            "ownerId" to FirebaseEmulator.auth.currentUser!!.uid) // Missing tripProfile, etc.

    FirebaseEmulator.firestore.collection("trips").document(tripId).set(badData).await()

    // Act & Assert
    // documentToTrip returns null if conversion fails, but getTrip throws if documentToTrip returns
    // null
    // So we expect an exception with "Trip not found" or similar, or just generic Exception
    assertFailsWith<Exception> { repository.getTrip(tripId) }
    Unit
  }
}
