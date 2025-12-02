package com.github.swent.swisstravel.model.trip

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.github.swent.swisstravel.model.user.Preference
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

const val TRIPS_COLLECTION_PATH = "trips"

/** Inspired by the SwEnt Bootcamp solution. */
class TripsRepositoryFirestore(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : TripsRepository {
  private val ownerAttributeName = "ownerId"

  override fun getNewUid(): String {
    return db.collection(TRIPS_COLLECTION_PATH).document().id
  }

  override suspend fun getAllTrips(): List<Trip> {
    val ownerId =
        auth.currentUser?.uid ?: throw Exception("TripsRepositoryFirestore: User not logged in.")

    val snapshot =
        try {
          db.collection(TRIPS_COLLECTION_PATH)
              .whereEqualTo(ownerAttributeName, ownerId)
              .get()
              .await()
        } catch (e: Exception) {
          db.collection(TRIPS_COLLECTION_PATH)
              .whereEqualTo(ownerAttributeName, ownerId)
              .get(Source.CACHE)
              .await()
        }

    return snapshot.mapNotNull { documentToTrip(it) }
  }

  override suspend fun getTrip(tripId: String): Trip {
    val document =
        try {
          db.collection(TRIPS_COLLECTION_PATH).document(tripId).get().await()
        } catch (e: Exception) {
          db.collection(TRIPS_COLLECTION_PATH).document(tripId).get(Source.CACHE).await()
        }
    return documentToTrip(document) ?: throw Exception("TripsRepositoryFirestore: Trip not found")
  }

  override suspend fun addTrip(trip: Trip) {
    db.collection(TRIPS_COLLECTION_PATH).document(trip.uid).set(trip).await()
  }

  override suspend fun editTrip(tripId: String, updatedTrip: Trip) {
    db.collection(TRIPS_COLLECTION_PATH).document(tripId).set(updatedTrip).await()
  }

  override suspend fun deleteTrip(tripId: String) {
    db.collection(TRIPS_COLLECTION_PATH).document(tripId).delete().await()
  }

  // The following code was made with the help of AI
  /**
   * Converts a Firestore document to a Trip object.
   *
   * @param document The Firestore document to convert.
   * @return The Trip object.
   */
  private fun documentToTrip(document: DocumentSnapshot): Trip? {
    return try {
      val uid = document.id
      val name = document.getString("name") ?: return null
      val ownerId = document.getString("ownerId") ?: return null

      val locations =
          (document["locations"] as? List<*>)?.mapNotNull { locationMap ->
            (locationMap as? Map<*, *>)?.let { mapToLocation(it) }
          } ?: emptyList()

      val routeSegments =
          (document["routeSegments"] as? List<*>)?.mapNotNull { routeSegmentMap ->
            (routeSegmentMap as? Map<*, *>)?.let { mapToRouteSegment(it) }
          } ?: emptyList()

      val activities =
          (document["activities"] as? List<*>)?.mapNotNull { activityMap ->
            (activityMap as? Map<*, *>)?.let { mapToActivity(it) }
          } ?: emptyList()

      val tripProfile =
          (document["tripProfile"] as? Map<*, *>)?.let { mapToTripProfile(it) } ?: return null

      val isFavorite = document.getBoolean("favorite") ?: false

      val isCurrentTrip = document.getBoolean("currentTrip") ?: false
      // With help of AI
      val listUriStrings = document.get("listUri") as? List<*> ?: emptyList<Uri>()
      val listUri = listUriStrings.mapNotNull { (it as? String)?.toUri() }

      Trip(
          uid = uid,
          name = name,
          ownerId = ownerId,
          locations = locations,
          routeSegments = routeSegments,
          activities = activities,
          tripProfile = tripProfile,
          isFavorite = isFavorite,
          isCurrentTrip = isCurrentTrip,
          listUri = listUri)
    } catch (e: Exception) {
      Log.e("TripsRepositoryFirestore", "Error converting document to Trip", e)
      null
    }
  }

  /**
   * Converts a Firestore map into a [Location] object.
   *
   * @param map The Firestore map expected to contain "name" and "coordinate" keys.
   * @return A [Location] if valid data is found, or `null` if data is incomplete or invalid.
   */
  private fun mapToLocation(map: Map<*, *>): Location? {
    val name = map["name"] as? String ?: return null
    val coordMap = map["coordinate"] as? Map<*, *> ?: return null
    val coordinate = mapToCoordinate(coordMap) ?: return null
    return Location(coordinate, name)
  }

  /**
   * Converts a Firestore map into a [Coordinate] object.
   *
   * @param map The Firestore map expected to contain "latitude" and "longitude" keys.
   * @return A [Coordinate] if valid numeric values are found, or `null` if data is invalid.
   */
  private fun mapToCoordinate(map: Map<*, *>): Coordinate? {
    val lat = (map["latitude"] as? Number)?.toDouble() ?: return null
    val lon = (map["longitude"] as? Number)?.toDouble() ?: return null
    return Coordinate(lat, lon)
  }

  /**
   * Converts a Firestore map into an [Activity] object.
   *
   * @param map The Firestore map expected to contain "startDate", "endDate", "location",
   *   "description", "imageUrls", and "estimatedTime" keys.
   * @return An [Activity] if all required fields are valid, or `null` otherwise.
   */
  private fun mapToActivity(map: Map<*, *>): Activity? {
    val startDate = map["startDate"] as? Timestamp ?: return null
    val endDate = map["endDate"] as? Timestamp ?: return null
    val locationMap = map["location"] as? Map<*, *> ?: return null
    val location = mapToLocation(locationMap) ?: return null
    val description = map["description"] as? String ?: return null
    val estimatedTime = (map["estimatedTime"] as? Number)?.toInt() ?: 0
    val imageUrls: List<String> =
        when (val raw = map["imageUrls"]) {
          is List<*> -> raw.mapNotNull { it as? String }
          is String -> listOf(raw)
          null -> return null
          else -> emptyList()
        }

    val act =
        Activity(
            startDate = startDate,
            endDate = endDate,
            location = location,
            description = description,
            imageUrls = imageUrls,
            estimatedTime = estimatedTime)
    return act
  }

  /**
   * Converts a Firestore map into a [RouteSegment] object.
   *
   * @param map The Firestore map expected to contain route data including: "from", "to",
   *   "distanceMeter", "durationMinutes", "path", "transportMode", "startDate", and "endDate".
   * @return A [RouteSegment] if all required fields are valid, or `null` otherwise.
   */
  private fun mapToRouteSegment(map: Map<*, *>): RouteSegment? {
    val fromMap = map["from"] as? Map<*, *> ?: return null
    val toMap = map["to"] as? Map<*, *> ?: return null
    val from = mapToLocation(fromMap) ?: return null
    val to = mapToLocation(toMap) ?: return null

    val durationMinutes = (map["durationMinutes"] as? Number)?.toInt() ?: return null

    val transportMode =
        (map["transportMode"] as? String)?.let { TransportMode.valueOf(it) } ?: return null

    val startDate = map["startDate"] as? Timestamp ?: return null
    val endDate = map["endDate"] as? Timestamp ?: return null

    return RouteSegment(from, to, durationMinutes, transportMode, startDate, endDate)
  }

  /**
   * Converts a Firestore map into a [TripProfile] object.
   *
   * @param map The Firestore map expected to contain: "startDate", "endDate", "preferredLocations",
   *   and "preferences".
   * @return A [TripProfile] if all required fields are valid, or `null` otherwise.
   */
  private fun mapToTripProfile(map: Map<*, *>): TripProfile? {
    val startDate = map["startDate"] as? Timestamp ?: return null
    val endDate = map["endDate"] as? Timestamp ?: return null

    val preferredLocations =
        (map["preferredLocations"] as? List<*>)?.mapNotNull { mapToLocation(it as Map<*, *>) }
            ?: emptyList()

    val preferences =
        (map["preferences"] as? List<*>)?.mapNotNull {
          when (it) {
            is String -> Preference.valueOf(it)
            is Map<*, *> -> mapToPreferences(it)
            else -> null
          }
        } ?: emptyList()

    val adults = (map["adults"] as? Long)?.toInt() ?: 1
    val children = (map["children"] as? Long)?.toInt() ?: 0

    val arrival = mapToLocation(map["arrivalLocation"] as? Map<*, *> ?: return null)
    val departure = mapToLocation(map["departureLocation"] as? Map<*, *> ?: return null)

    return TripProfile(
        startDate, endDate, preferredLocations, preferences, adults, children, arrival, departure)
  }

  /**
   * Converts a Firestore map into a [Preference] object.
   *
   * @param map The Firestore map expected to contain "preference" (enum name) and "rating"
   *   (number).
   * @return A [Preference] if valid data is provided, or `null` if conversion fails.
   */
  private fun mapToPreferences(map: Map<*, *>): Preference? {
    val preferenceStr = map["preference"] as? String ?: return null
    val preference = Preference.valueOf(preferenceStr)
    return preference
  }
}
