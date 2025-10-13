package com.github.swent.swisstravel.model.trip

import android.util.Log
import com.github.swent.swisstravel.model.user.RatedPreferences
import com.github.swent.swisstravel.model.user.UserPreference
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

const val TRIPS_COLLECTION_PATH = "trips"

/** Inspired by the SwEnt Bootcamp solution. */
class TripsRepositoryFirestore(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : TripsRepository {
  private val ownerAttributeName = "ownerId"

  override fun getNewUid(): String {
    return db.collection(TRIPS_COLLECTION_PATH).document().id
  }

  override suspend fun getAllTrips(): List<Trip> {
    val ownerId =
        Firebase.auth.currentUser?.uid
            ?: throw Exception("TripsRepositoryFirestore: User not logged in.")

    val snapshot =
        db.collection(TRIPS_COLLECTION_PATH).whereEqualTo(ownerAttributeName, ownerId).get().await()

    return snapshot.mapNotNull { documentToTrip(it) }
  }

  override suspend fun getTrip(tripId: String): Trip {
    val document = db.collection(TRIPS_COLLECTION_PATH).document(tripId).get().await()
    return documentToTrip(document) ?: throw Exception("TripsRepositoryFirestore: Trip not found")
  }

  override suspend fun addTrip(trip: Trip) {
    db.collection(TRIPS_COLLECTION_PATH).document(trip.uid).set(trip).await()
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
          (document.get("locations") as? List<*>)?.mapNotNull { it ->
            (it as? Map<*, *>)?.let { mapToLocation(it) }
          } ?: emptyList()

      val routeSegments =
          (document.get("routeSegments") as? List<*>)?.mapNotNull { it ->
            (it as? Map<*, *>)?.let { mapToRouteSegment(it) }
          } ?: emptyList()

      val activities =
          (document.get("activities") as? List<*>)?.mapNotNull { it ->
            (it as? Map<*, *>)?.let { mapToActivity(it) }
          } ?: emptyList()

      val tripProfile =
          (document.get("tripProfile") as? Map<*, *>)?.let { mapToTripProfile(it) }
              ?: TripProfile(
                  startDate = Timestamp.now(),
                  endDate = Timestamp.now(),
                  preferredLocations = emptyList(),
                  preferences = emptyList())

      Trip(
          uid = uid,
          name = name,
          ownerId = ownerId,
          locations = locations,
          routeSegments = routeSegments,
          activities = activities,
          tripProfile = tripProfile)
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
   * @param map The Firestore map expected to contain "startDate", "endDate", and "location".
   * @return An [Activity] if all required fields are valid, or `null` otherwise.
   */
  private fun mapToActivity(map: Map<*, *>): Activity? {
    val startDate = map["startDate"] as? Timestamp ?: return null
    val endDate = map["endDate"] as? Timestamp ?: return null
    val locationMap = map["location"] as? Map<*, *> ?: return null
    val location = mapToLocation(locationMap) ?: return null
    return Activity(startDate, endDate, location)
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

    val distanceMeter = (map["distanceMeter"] as? Number)?.toInt() ?: return null
    val durationMinutes = (map["durationMinutes"] as? Number)?.toInt() ?: return null

    val pathList =
        (map["path"] as? List<*>)?.mapNotNull { mapToCoordinate(it as Map<*, *>) } ?: emptyList()

    val transportMode =
        (map["transportMode"] as? String)?.let { TransportMode.valueOf(it) } ?: return null

    val startDate = map["startDate"] as? Timestamp ?: return null
    val endDate = map["endDate"] as? Timestamp ?: return null

    return RouteSegment(
        from, to, distanceMeter, durationMinutes, pathList, transportMode, startDate, endDate)
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
        (map["preferences"] as? List<*>)?.mapNotNull { mapToRatedPreferences(it as Map<*, *>) }
            ?: emptyList()

    return TripProfile(startDate, endDate, preferredLocations, preferences)
  }

  /**
   * Converts a Firestore map into a [RatedPreferences] object.
   *
   * @param map The Firestore map expected to contain "preference" (enum name) and "rating"
   *   (number).
   * @return A [RatedPreferences] if valid data is provided, or `null` if conversion fails.
   */
  private fun mapToRatedPreferences(map: Map<*, *>): RatedPreferences? {
    val rating = (map["rating"] as? Number)?.toInt() ?: return null
    val preferenceStr = map["preference"] as? String ?: return null
    val userPreference = UserPreference.valueOf(preferenceStr)

    return RatedPreferences(userPreference, rating)
  }
}
