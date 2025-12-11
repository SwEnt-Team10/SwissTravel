package com.github.swent.swisstravel.model.trip

import android.net.Uri
import com.github.swent.swisstravel.model.user.Preference
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import io.mockk.*
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
    // Par défaut, si on parse une string, on retourne un mock d'Uri
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

    // Mock URI Location Map (String key -> Location Map value)
    val uriStr = "http://example.com/photo.jpg"
    val uriLocationMap = mapOf(uriStr to locationMap)

    every { doc.get("locations") } returns listOf(locationMap)
    every { doc.get("routeSegments") } returns listOf(routeSegmentMap)
    every { doc.get("activities") } returns listOf(activityMap)
    every { doc.get("tripProfile") } returns tripProfileMap
    every { doc.getBoolean("favorite") } returns true
    every { doc.getBoolean("currentTrip") } returns false
    // C'est ici que ça change : on retourne une map pour uriLocation
    every { doc.get("uriLocation") } returns uriLocationMap
    every { doc.get("collaboratorsId") } returns emptyList<String>()
    every { doc.getBoolean("random") } returns false

    val trip = repo.getTrip("trip1")

    assertEquals("trip1", trip.uid)
    assertEquals("TripName", trip.name)
    assertEquals("owner1", trip.ownerId)
    assertTrue(trip.locations.isNotEmpty())
    assertTrue(trip.routeSegments.isNotEmpty())
    assertTrue(trip.activities.isNotEmpty())
    assertEquals(1, trip.tripProfile.preferences.size)
    assertTrue(trip.isFavorite)

    // Vérification de la nouvelle map
    assertEquals(1, trip.uriLocation.size)
    // La clé doit être un Uri (grâce au mock statique)
    val key = trip.uriLocation.keys.first()
    assertEquals(uriStr, key.toString())
  }

  @Test
  fun `getTrip throws when required fields missing`() = runTest {
    val doc = mockk<DocumentSnapshot>()
    every { mockCollection.document("badTrip").get(Source.SERVER) } returns Tasks.forResult(doc)
    every { doc.id } returns "badTrip"
    every { doc.getString("name") } returns null // triggers documentToTrip null
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

    val trip = repo.getTrip("tripImgUrls")

    assertEquals(1, trip.activities.size)
    assertTrue(!trip.activities.first().imageUrls.isEmpty())
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
    // uriLocation manquant ou vide
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

    val trip = repo.getTrip("tripEmpty")
    assertEquals(0, trip.locations.size)
    assertEquals(0, trip.routeSegments.size)
    assertEquals(0, trip.activities.size)
    assertEquals(0, trip.tripProfile.preferences.size)
    assertEquals(0, trip.uriLocation.size)
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

    // 2) Trips where current user is a collaborator (return empty list)
    every { mockCollection.whereArrayContains("collaboratorsId", "owner123") } returns
        mockCollaboratorQuery
    every { mockCollaboratorQuery.get() } returns Tasks.forResult(mockCollaboratorSnapshot)
    every { mockCollaboratorSnapshot.documents } returns emptyList()

    // Document fields used by documentToTrip
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
    // IMPORTANT: use the same key as in documentToTrip
    every { doc.getBoolean("favorite") } returns false
    every { doc.getBoolean("currentTrip") } returns false
    every { doc.getBoolean("isFavorite") } returns false
    every { doc.getBoolean("random") } returns false

    // Act
    val trips = repo.getAllTrips()

    // Assert
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
    // Mock a specific URI
    val mockUri = mockk<Uri>()
    every { mockUri.toString() } returns "http://fake.uri"

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
            // On passe une map avec un mock URI
            uriLocation = emptyMap(),
            collaboratorsId = emptyList(),
            isRandom = false)
    every { mockCollection.document("t1") } returns mockDocumentRef
    // On attend n'importe quel map lors du set
    every { mockDocumentRef.set(any()) } returns Tasks.forResult(null)

    repo.addTrip(trip)

    // On vérifie que le set est appelé avec les bonnes données transformées
    verify {
      mockDocumentRef.set(
          match { data ->
            val map = data as Map<String, Any?>
            map["uid"] == "t1" &&
                map["uriLocation"] is Map<*, *> // Vérifie que uriLocation est présent
          })
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

    // Verify we wrote to the doc identified by the tripId and passed the updatedTrip as payload
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

    // -------- ONLINE QUERIES (will fail) --------
    every { mockDb.collection(TRIPS_COLLECTION_PATH).whereEqualTo("ownerId", "owner123") } returns
        mockOwnerQuery
    every {
      mockDb.collection(TRIPS_COLLECTION_PATH).whereArrayContains("collaboratorsId", "owner123")
    } returns mockCollaboratorQuery

    // Make the ONLINE .get() fail so we enter the catch block
    every { mockOwnerQuery.get() } returns Tasks.forException(RuntimeException("Network error"))
    every { mockCollaboratorQuery.get() } returns
        Tasks.forException(RuntimeException("Network error"))

    // -------- CACHE QUERIES (used in catch block) --------
    every { mockOwnerQuery[Source.CACHE] } returns Tasks.forResult(mockOwnerCacheSnapshot)
    every { mockCollaboratorQuery[Source.CACHE] } returns
        Tasks.forResult(mockCollaboratorCacheSnapshot)

    every { mockOwnerCacheSnapshot.documents } returns listOf(doc)
    every { mockCollaboratorCacheSnapshot.documents } returns emptyList()

    // Document fields used by documentToTrip
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
    every { doc.getBoolean("random") } returns false

    // Act
    val trips = repo.getAllTrips()

    // Assert – result comes from cache path
    assertEquals(1, trips.size)
    assertEquals("Cached Trip", trips.first().name)
  }
}
