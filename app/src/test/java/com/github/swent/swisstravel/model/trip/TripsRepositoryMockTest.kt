package com.github.swent.swisstravel.model.trip

import android.net.Uri
import com.github.swent.swisstravel.model.user.Preference
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import io.mockk.*
import kotlin.collections.emptyList
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class TripsRepositoryFirestorePublicTest {

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

    // IMPORTANT : On doit mocker Uri.parse car il est utilisé dans documentToTrip
    mockkStatic(Uri::class)
    every { Uri.parse(any()) } answers
        {
          val uriStr = firstArg<String>()
          val uriMock = mockk<Uri>()
          every { uriMock.toString() } returns uriStr
          uriMock
        }

    every { mockDb.collection(TRIPS_COLLECTION_PATH) } returns mockCollection
    repo = TripsRepositoryFirestore(mockDb, mockAuth)
  }

  @After
  fun teardown() {
    unmockkAll()
  }

  // ---------------------------------------------------
  // getNewUid
  // ---------------------------------------------------
  @Test
  fun `getNewUid returns generated document id`() {
    every { mockCollection.document() } returns mockDocumentRef
    every { mockDocumentRef.id } returns "uid-123"
    val uid = repo.getNewUid()
    assertEquals("uid-123", uid)
  }

  // ---------------------------------------------------
  // getTrip - full coverage for mapping functions
  // ---------------------------------------------------
  @Test
  fun `getTrip returns Trip with all nested fields`() = runTest {
    val doc = mockk<DocumentSnapshot>()
    every { mockCollection.document("trip1").get(Source.SERVER) } returns Tasks.forResult(doc)

    // Base document
    every { doc.id } returns "trip1"
    every { doc.getString("name") } returns "TripName"
    every { doc.getString("ownerId") } returns "owner1"

    // Locations, Activities, RouteSegments, TripProfile
    val locationMap =
        mapOf("name" to "Place", "coordinate" to mapOf("latitude" to 1.0, "longitude" to 2.0))
    val routeSegmentMap =
        mapOf(
            "from" to locationMap,
            "to" to locationMap,
            "durationMinutes" to 10,
            "transportMode" to TransportMode.WALKING.name,
            "startDate" to Timestamp.now(),
            "endDate" to Timestamp.now())
    val activityMap =
        mapOf(
            "startDate" to Timestamp.now(),
            "endDate" to Timestamp.now(),
            "location" to locationMap,
            "description" to "Activity",
            "imageUrls" to emptyList<String>(),
            "estimatedTime" to 3600)
    val tripProfileMap =
        mapOf(
            "startDate" to Timestamp.now(),
            "endDate" to Timestamp.now(),
            "preferredLocations" to listOf(locationMap),
            "preferences" to listOf(mapOf("preference" to Preference.FOODIE.name)),
            "arrivalLocation" to locationMap,
            "departureLocation" to locationMap,
        )

    // Simulation de la Map uriLocation stockée dans Firestore
    val uriLocationMap = mapOf("http://fake.uri" to locationMap)

    every { doc.get("locations") } returns listOf(locationMap)
    every { doc.get("routeSegments") } returns listOf(routeSegmentMap)
    every { doc.get("activities") } returns listOf(activityMap)
    every { doc.get("tripProfile") } returns tripProfileMap
    every { doc.getBoolean("favorite") } returns true
    every { doc.getBoolean("currentTrip") } returns false
    every { doc.get("uriLocation") } returns uriLocationMap
    every { doc.get("collaboratorsId") } returns emptyList<String>()
    every { doc.getBoolean("random") } returns false
    every { doc.get("cachedActivities") } returns emptyList<Map<String, Any>>()

    val trip = repo.getTrip("trip1")

    assertEquals("trip1", trip.uid)
    assertEquals("TripName", trip.name)
    assertEquals("owner1", trip.ownerId)
    assertTrue(trip.locations.isNotEmpty())
    assertTrue(trip.routeSegments.isNotEmpty())
    assertTrue(trip.activities.isNotEmpty())
    assertEquals(1, trip.tripProfile.preferences.size)
    assertTrue(trip.isFavorite)
    assertEquals(1, trip.uriLocation.size)
  }

  @Test
  fun `getTrip throws when required fields missing`() = runTest {
    val doc = mockk<DocumentSnapshot>()
    every { mockCollection.document("badTrip").get(Source.SERVER) } returns Tasks.forResult(doc)
    every { doc.id } returns "badTrip"
    every { doc.getString("name") } returns null
    every { doc.getString("ownerId") } returns "owner1"

    assertFailsWith<Exception> { repo.getTrip("badTrip") }
  }

  @Test
  fun `getTrip handles singleton imageUrls by treating as singleton list`() = runTest {
    val doc = mockk<DocumentSnapshot>()
    every { mockCollection.document("tripImgUrls").get(Source.SERVER) } returns Tasks.forResult(doc)
    every { doc.id } returns "tripImgUrls"
    every { doc.getString("name") } returns "TripImageUrls"
    every { doc.getString("ownerId") } returns "owner1"

    val locationMap =
        mapOf("name" to "Somewhere", "coordinate" to mapOf("latitude" to 1.0, "longitude" to 2.0))
    val activityMap =
        mapOf(
            "startDate" to Timestamp.now(),
            "endDate" to Timestamp.now(),
            "location" to locationMap,
            "description" to "ImageUrls Activity",
            "imageUrls" to "http://example.com/solo.jpg",
            "estimatedTime" to 900)

    every { doc.get("activities") } returns listOf(activityMap)
    every { doc.get("locations") } returns listOf(locationMap)
    every { doc.get("routeSegments") } returns emptyList<Map<String, Any>>()
    every { doc.get("tripProfile") } returns
        mapOf(
            "startDate" to Timestamp.now(),
            "endDate" to Timestamp.now(),
            "preferredLocations" to listOf(locationMap),
            "preferences" to emptyList<Map<String, Any>>(),
            "adults" to 1L,
            "children" to 0L,
            "arrivalLocation" to locationMap,
            "departureLocation" to locationMap)
    every { doc.getBoolean("favorite") } returns false
    every { doc.getBoolean("currentTrip") } returns false
    every { doc.get("uriLocation") } returns emptyMap<String, Any>()
    every { doc.get("collaboratorsId") } returns emptyList<String>()
    every { doc.getBoolean("random") } returns false
    every { doc.get("cachedActivities") } returns emptyList<Map<String, Any>>()

    val trip = repo.getTrip("tripImgUrls")

    assertEquals(1, trip.activities.size)
    assertTrue(!trip.activities.first().imageUrls.isEmpty())
  }

  @Test
  fun `getTrip skips invalid activities gracefully`() = runTest {
    val doc = mockk<DocumentSnapshot>()
    val locationMap =
        mapOf("name" to "Somewhere", "coordinate" to mapOf("latitude" to 1.0, "longitude" to 2.0))
    every { mockCollection.document("tripWithBadActivities").get(Source.SERVER) } returns
        Tasks.forResult(doc)
    every { doc.id } returns "tripWithBadActivities"
    every { doc.getString("name") } returns "TripWithBadActivities"
    every { doc.getString("ownerId") } returns "owner1"

    val validActivityMap =
        mapOf(
            "startDate" to Timestamp.now(),
            "endDate" to Timestamp.now(),
            "location" to
                mapOf(
                    "name" to "ValidPlace",
                    "coordinate" to mapOf("latitude" to 1.0, "longitude" to 2.0)),
            "description" to "Valid activity",
            "imageUrls" to listOf("img1", "img2"),
            "estimatedTime" to 1800)
    val invalidActivityMap =
        mapOf(
            "startDate" to Timestamp.now(),
            "endDate" to Timestamp.now(),
            "description" to "Invalid activity",
            "imageUrls" to listOf("x"),
            "estimatedTime" to 1200)

    every { doc.get("activities") } returns listOf(validActivityMap, invalidActivityMap)
    every { doc.get("locations") } returns emptyList<Map<String, Any>>()
    every { doc.get("routeSegments") } returns emptyList<Map<String, Any>>()
    every { doc.get("uriLocation") } returns emptyMap<String, Any>()
    every { doc.get("collaboratorsId") } returns emptyList<Uri>()
    every { doc.get("tripProfile") } returns
        mapOf(
            "startDate" to Timestamp.now(),
            "endDate" to Timestamp.now(),
            "preferredLocations" to emptyList<Map<String, Any>>(),
            "preferences" to emptyList<Map<String, Any>>(),
            "adults" to 1L,
            "children" to 0L,
            "arrivalLocation" to locationMap,
            "departureLocation" to locationMap)
    every { doc.getBoolean("favorite") } returns false
    every { doc.getBoolean("currentTrip") } returns false
    every { doc.getBoolean("random") } returns false
    every { doc.get("cachedActivities") } returns emptyList<Map<String, Any>>()

    val trip = repo.getTrip("tripWithBadActivities")

    assertEquals("tripWithBadActivities", trip.uid)
    assertEquals(1, trip.activities.size)
    assertEquals("Valid activity", trip.activities.first().description)
  }

  @Test
  fun `getTrip handles empty optional lists`() = runTest {
    val doc = mockk<DocumentSnapshot>()
    val locationMap =
        mapOf("name" to "Somewhere", "coordinate" to mapOf("latitude" to 1.0, "longitude" to 2.0))
    every { mockCollection.document("tripEmpty").get(Source.SERVER) } returns Tasks.forResult(doc)
    every { doc.id } returns "tripEmpty"
    every { doc.getString("name") } returns "EmptyTrip"
    every { doc.getString("ownerId") } returns "owner1"
    every { doc.get("locations") } returns emptyList<Map<String, Any>>()
    every { doc.get("routeSegments") } returns emptyList<Map<String, Any>>()
    every { doc.get("activities") } returns emptyList<Map<String, Any>>()
    every { doc.get("uriLocation") } returns emptyMap<String, Any>()
    every { doc.get("collaboratorsId") } returns emptyList<Uri>()
    every { doc.get("tripProfile") } returns
        mapOf(
            "startDate" to Timestamp.now(),
            "endDate" to Timestamp.now(),
            "preferredLocations" to emptyList<Map<String, Any>>(),
            "preferences" to emptyList<Map<String, Any>>(),
            "adults" to 1L,
            "children" to 0L,
            "arrivalLocation" to locationMap,
            "departureLocation" to locationMap)
    every { doc.getBoolean("favorite") } returns false
    every { doc.getBoolean("currentTrip") } returns false
    every { doc.getBoolean("random") } returns false
    every { doc.get("cachedActivities") } returns emptyList<Map<String, Any>>()

    val trip = repo.getTrip("tripEmpty")
    assertEquals(0, trip.locations.size)
    assertEquals(0, trip.routeSegments.size)
    assertEquals(0, trip.activities.size)
    assertEquals(0, trip.tripProfile.preferences.size)
  }

  // ---------------------------------------------------
  // getAllTrips
  // ---------------------------------------------------
  @Test
  fun `getAllTrips returns trips for current user`() = runTest {
    val mockOwnerQuery = mockk<Query>()
    val mockCollaboratorQuery = mockk<Query>()
    val mockOwnerSnapshot = mockk<QuerySnapshot>(relaxed = true)
    val mockCollaboratorSnapshot = mockk<QuerySnapshot>(relaxed = true)
    val doc = mockk<QueryDocumentSnapshot>(relaxed = true)
    val locationMap =
        mapOf("name" to "Somewhere", "coordinate" to mapOf("latitude" to 1.0, "longitude" to 2.0))

    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "owner123"

    every { mockCollection.whereEqualTo("ownerId", "owner123") } returns mockOwnerQuery
    every { mockOwnerQuery.get() } returns Tasks.forResult(mockOwnerSnapshot)
    every { mockOwnerSnapshot.documents } returns listOf(doc)
    every { mockOwnerSnapshot.iterator() } returns
        listOf(doc).iterator() as MutableIterator<QueryDocumentSnapshot?>

    every { mockCollection.whereArrayContains("collaboratorsId", "owner123") } returns
        mockCollaboratorQuery
    every { mockCollaboratorQuery.get() } returns Tasks.forResult(mockCollaboratorSnapshot)
    every { mockCollaboratorSnapshot.documents } returns emptyList()

    every { doc.id } returns "tripX"
    every { doc.getString("name") } returns "Trip X"
    every { doc.getString("ownerId") } returns "owner123"
    every { doc.get("locations") } returns emptyList<Map<String, Any>>()
    every { doc.get("routeSegments") } returns emptyList<Map<String, Any>>()
    every { doc.get("activities") } returns emptyList<Map<String, Any>>()
    every { doc.get("collaboratorsId") } returns emptyList<String>()
    every { doc.get("uriLocation") } returns emptyMap<String, Any>()
    every { doc.get("tripProfile") } returns
        mapOf(
            "startDate" to Timestamp.now(),
            "endDate" to Timestamp.now(),
            "preferredLocations" to emptyList<Map<String, Any>>(),
            "preferences" to emptyList<Map<String, Any>>(),
            "adults" to 1L,
            "children" to 0L,
            "arrivalLocation" to locationMap,
            "departureLocation" to locationMap)
    every { doc.getBoolean("favorite") } returns false
    every { doc.getBoolean("currentTrip") } returns false
    every { doc.getBoolean("isFavorite") } returns false
    every { doc.getBoolean("random") } returns false

    val trips = repo.getAllTrips()

    assertEquals(1, trips.size)
    assertEquals("Trip X", trips.first().name)
  }

  @Test
  fun `getAllTrips throws if user not logged in`() = runTest {
    every { mockAuth.currentUser } returns null
    assertFailsWith<Exception> { repo.getAllTrips() }
  }

  // ---------------------------------------------------
  // addTrip and deleteTrip
  // ---------------------------------------------------
  @Test
  fun `addTrip calls set on document`() = runTest {
    val trip =
        Trip(
            "t1",
            "Trip1",
            "ownerX",
            emptyList(),
            emptyList(),
            emptyList(),
            TripProfile(Timestamp.now(), Timestamp.now(), emptyList(), emptyList()),
            isFavorite = false,
            isCurrentTrip = false,
            uriLocation = emptyMap(),
            collaboratorsId = emptyList(),
            isRandom = false)
    every { mockCollection.document("t1") } returns mockDocumentRef
    every { mockDocumentRef.set(any()) } returns Tasks.forResult(null)

    repo.addTrip(trip)

    // CORRECTION : On spécifie <Map<String, Any?>> explicitement
    verify {
      mockDocumentRef.set(withArg<Map<String, Any?>> { data -> assertEquals("t1", data["uid"]) })
    }
  }

  @Test
  fun `deleteTrip calls delete on document`() = runTest {
    every { mockCollection.document("todel") } returns mockDocumentRef
    every { mockDocumentRef.delete() } returns Tasks.forResult(null)

    repo.deleteTrip("todel")
    verify { mockDocumentRef.delete() }
  }

  // ---------------------------------------------------
  // editTrip
  // ---------------------------------------------------
  @Test
  fun `editTrip calls set on the document with tripId`() = runTest {
    val updated =
        Trip(
            "any-uid-will-do",
            "Updated Trip",
            "ownerX",
            emptyList(),
            emptyList(),
            emptyList(),
            TripProfile(Timestamp.now(), Timestamp.now(), emptyList(), emptyList()),
            isFavorite = false,
            isCurrentTrip = false,
            uriLocation = emptyMap(),
            collaboratorsId = emptyList(),
            isRandom = false)

    every { mockCollection.document("server-id-123") } returns mockDocumentRef
    every { mockDocumentRef.set(any()) } returns Tasks.forResult(null)

    repo.editTrip("server-id-123", updated)

    verify { mockCollection.document("server-id-123") }
    verify { mockDocumentRef.set(any()) }
  }

  @Test
  fun `editTrip propagates Firestore errors`() = runTest {
    val updated =
        Trip(
            "t1",
            "Bad Update",
            "ownerX",
            emptyList(),
            emptyList(),
            emptyList(),
            TripProfile(Timestamp.now(), Timestamp.now(), emptyList(), emptyList()),
            isFavorite = false,
            isCurrentTrip = false,
            uriLocation = emptyMap(),
            collaboratorsId = emptyList(),
            isRandom = false)

    every { mockCollection.document("t1") } returns mockDocumentRef
    every { mockDocumentRef.set(any()) } returns
        Tasks.forException(RuntimeException("firestore boom"))

    assertFailsWith<RuntimeException> { repo.editTrip("t1", updated) }
  }

  @Test
  fun `editTrip uses tripId even if updatedTrip uid differs`() = runTest {
    val updated =
        Trip(
            "different-local-uid",
            "Updated Trip",
            "ownerX",
            emptyList(),
            emptyList(),
            emptyList(),
            TripProfile(Timestamp.now(), Timestamp.now(), emptyList(), emptyList()),
            isFavorite = false,
            isCurrentTrip = false,
            uriLocation = emptyMap(),
            collaboratorsId = emptyList(),
            isRandom = false)

    every { mockCollection.document("authoritative-server-id") } returns mockDocumentRef
    every { mockDocumentRef.set(any()) } returns Tasks.forResult(null)

    repo.editTrip("authoritative-server-id", updated)

    verify { mockCollection.document("authoritative-server-id") }
    verify { mockDocumentRef.set(any()) }
  }

  @Test
  fun `getAllTrips falls back to cache when online query fails`() = runTest {
    val mockOwnerQuery = mockk<Query>()
    val mockCollaboratorQuery = mockk<Query>()
    val mockOwnerCacheSnapshot = mockk<QuerySnapshot>(relaxed = true)
    val mockCollaboratorCacheSnapshot = mockk<QuerySnapshot>(relaxed = true)
    val doc = mockk<QueryDocumentSnapshot>(relaxed = true)
    val locationMap =
        mapOf("name" to "Somewhere", "coordinate" to mapOf("latitude" to 1.0, "longitude" to 2.0))

    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "owner123"

    every { mockDb.collection(TRIPS_COLLECTION_PATH).whereEqualTo("ownerId", "owner123") } returns
        mockOwnerQuery
    every {
      mockDb.collection(TRIPS_COLLECTION_PATH).whereArrayContains("collaboratorsId", "owner123")
    } returns mockCollaboratorQuery

    every { mockOwnerQuery.get() } returns Tasks.forException(RuntimeException("Network error"))
    every { mockCollaboratorQuery.get() } returns
        Tasks.forException(RuntimeException("Network error"))

    every { mockOwnerQuery[Source.CACHE] } returns Tasks.forResult(mockOwnerCacheSnapshot)
    every { mockCollaboratorQuery[Source.CACHE] } returns
        Tasks.forResult(mockCollaboratorCacheSnapshot)

    every { mockOwnerCacheSnapshot.documents } returns listOf(doc)
    every { mockCollaboratorCacheSnapshot.documents } returns emptyList()

    every { doc.id } returns "tripCache"
    every { doc.getString("name") } returns "Cached Trip"
    every { doc.getString("ownerId") } returns "owner123"
    every { doc.get("locations") } returns emptyList<Map<String, Any>>()
    every { doc.get("routeSegments") } returns emptyList<Map<String, Any>>()
    every { doc.get("activities") } returns emptyList<Map<String, Any>>()
    every { doc.get("collaboratorsId") } returns emptyList<String>()
    every { doc.get("uriLocation") } returns emptyMap<String, Any>()
    every { doc.get("tripProfile") } returns
        mapOf(
            "startDate" to Timestamp.now(),
            "endDate" to Timestamp.now(),
            "preferredLocations" to emptyList<Map<String, Any>>(),
            "preferences" to emptyList<Map<String, Any>>(),
            "adults" to 1L,
            "children" to 0L,
            "arrivalLocation" to locationMap,
            "departureLocation" to locationMap)
    every { doc.getBoolean("favorite") } returns false
    every { doc.getBoolean("currentTrip") } returns false

    val trips = repo.getAllTrips()

    assertEquals(1, trips.size)
    assertEquals("Cached Trip", trips.first().name)
  }
}
