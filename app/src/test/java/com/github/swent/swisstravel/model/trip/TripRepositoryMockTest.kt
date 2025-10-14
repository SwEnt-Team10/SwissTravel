package com.github.swent.swisstravel.model.trip

import com.github.swent.swisstravel.model.user.UserPreference
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import io.mockk.*
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import org.bouncycastle.util.test.SimpleTest.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TripsRepositoryFirestoreTest {

  private lateinit var mockDb: FirebaseFirestore
  private lateinit var mockCollection: CollectionReference
  private lateinit var mockDocumentRef: DocumentReference
  private lateinit var mockAuth: FirebaseAuth
  private lateinit var mockUser: FirebaseUser
  private lateinit var repo: TripsRepositoryFirestore

  @Before
  fun setup() {
    mockDb = mockk()
    mockCollection = mockk()
    mockDocumentRef = mockk()
    mockAuth = mockk()
    mockUser = mockk()

    every { mockDb.collection(TRIPS_COLLECTION_PATH) } returns mockCollection
    repo = TripsRepositoryFirestore(mockDb, mockAuth)
  }

  @After
  fun teardown() {
    unmockkAll()
  }

  // ---------------------------------------------------------------------------
  // getNewUid
  // ---------------------------------------------------------------------------
  @Test
  fun `getNewUid returns generated document id`() {
    every { mockCollection.document() } returns mockDocumentRef
    every { mockDocumentRef.id } returns "generated-uid-123"

    val uid = repo.getNewUid()

    assertEquals("generated-uid-123", uid)
  }

  // ---------------------------------------------------------------------------
  // getTrip
  // ---------------------------------------------------------------------------
  @Test
  fun `getTrip returns Trip when document contains valid data`() = runTest {
    val doc = mockk<DocumentSnapshot>()
    every { mockDb.collection(TRIPS_COLLECTION_PATH).document("trip1").get() } returns
        Tasks.forResult(doc)

    // document basic fields
    every { doc.id } returns "trip1"
    every { doc.getString("name") } returns "My Trip"
    every { doc.getString("ownerId") } returns "owner1"

    // nested location map
    val locationMap =
        mapOf("name" to "Place A", "coordinate" to mapOf("latitude" to 10.0, "longitude" to 20.0))

    // route segment map
    val routeSegmentMap =
        mapOf(
            "from" to locationMap,
            "to" to locationMap,
            "distanceMeter" to 100,
            "durationMinutes" to 15,
            "path" to listOf(mapOf("latitude" to 1.0, "longitude" to 2.0)),
            "transportMode" to TransportMode.WALKING.name,
            "startDate" to Timestamp.now(),
            "endDate" to Timestamp.now())

    // activity map
    val activityMap =
        mapOf(
            "startDate" to Timestamp.now(), "endDate" to Timestamp.now(), "location" to locationMap)

    // trip profile with preferences
    val tripProfileMap =
        mapOf(
            "startDate" to Timestamp.now(),
            "endDate" to Timestamp.now(),
            "preferredLocations" to listOf(locationMap),
            "preferences" to
                listOf(mapOf("rating" to 5, "preference" to UserPreference.FOODIE.name)))

    every { doc.get("locations") } returns listOf(locationMap)
    every { doc.get("routeSegments") } returns listOf(routeSegmentMap)
    every { doc.get("activities") } returns listOf(activityMap)
    every { doc.get("tripProfile") } returns tripProfileMap

    val trip = repo.getTrip("trip1")

    assertEquals("trip1", trip.uid)
    assertEquals("My Trip", trip.name)
    assertEquals("owner1", trip.ownerId)
    assertTrue(trip.locations.isNotEmpty())
    assertTrue(trip.routeSegments.isNotEmpty())
    assertTrue(trip.activities.isNotEmpty())
    assertNotNull(trip.tripProfile)
  }

  @Test
  fun `getTrip throws when document cannot be converted`() = runTest {
    val doc = mockk<DocumentSnapshot>()
    every { mockDb.collection(TRIPS_COLLECTION_PATH).document("bad").get() } returns
        Tasks.forResult(doc)
    every { doc.id } returns "bad"
    every { doc.getString("name") } returns null // Missing required field

    assertFailsWith<Exception> { repo.getTrip("bad") }
  }

  // ---------------------------------------------------------------------------
  // getAllTrips
  // ---------------------------------------------------------------------------
  @Test
  fun `getAllTrips returns trips for current user`() = runTest {
    val mockQuery = mockk<Query>()
    val mockQuerySnapshot = mockk<QuerySnapshot>()
    val doc = mockk<QueryDocumentSnapshot>()

    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "owner123"

    every { mockDb.collection(TRIPS_COLLECTION_PATH).whereEqualTo("ownerId", "owner123") } returns
        mockQuery

    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)
    every { mockQuerySnapshot.documents } returns listOf(doc)
    every { mockQuerySnapshot.iterator() } returns
        listOf(doc).iterator() as MutableIterator<QueryDocumentSnapshot?>

    every { doc.id } returns "tripX"
    every { doc.getString("name") } returns "Trip X"
    every { doc.getString("ownerId") } returns "owner123"
    every { doc.get("locations") } returns emptyList<Map<String, Any>>()
    every { doc.get("routeSegments") } returns emptyList<Map<String, Any>>()
    every { doc.get("activities") } returns emptyList<Map<String, Any>>()
    every { doc.get("tripProfile") } returns null

    val trips = repo.getAllTrips()

    assertEquals(1, trips.size)
    assertEquals("Trip X", trips.first().name)
  }

  @Test
  fun `getAllTrips throws when user not logged in`() = runTest {
    every { mockAuth.currentUser } returns null
    assertFailsWith<Exception> { repo.getAllTrips() }
  }

  // ---------------------------------------------------------------------------
  // addTrip
  // ---------------------------------------------------------------------------
  @Test
  fun `addTrip calls set on document reference and completes`() = runTest {
    val trip =
        Trip(
            uid = "t1",
            name = "Test Trip",
            ownerId = "ownerX",
            locations = emptyList(),
            routeSegments = emptyList(),
            activities = emptyList(),
            tripProfile = TripProfile(Timestamp.now(), Timestamp.now(), emptyList(), emptyList()))

    every { mockCollection.document(trip.uid) } returns mockDocumentRef
    every { mockDocumentRef.set(trip) } returns Tasks.forResult(null)

    repo.addTrip(trip)

    verify { mockDocumentRef.set(trip) }
  }

  // ---------------------------------------------------------------------------
  // deleteTrip
  // ---------------------------------------------------------------------------
  @Test
  fun `deleteTrip calls delete on document reference and completes`() = runTest {
    every { mockCollection.document("todel") } returns mockDocumentRef
    every { mockDocumentRef.delete() } returns Tasks.forResult(null)

    repo.deleteTrip("todel")

    verify { mockDocumentRef.delete() }
  }
}
