package com.github.swent.swisstravel.model.trip

import com.google.firebase.Firebase
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

  /**
   * Converts a Firestore document to a Trip object.
   *
   * @param document The Firestore document to convert.
   * @return The Trip object.
   */
  private fun documentToTrip(document: DocumentSnapshot): Trip? {
    val uid =
        document.getString("uid")
            ?: return null // TODO What do we want to do if a field is missing?
    val name = document.getString("name") ?: return null
    val startDate = document.getTimestamp("startDate") ?: return null
    val endDate = document.getTimestamp("endDate") ?: return null
    val ownerId = document.getString("ownerId") ?: return null
    val locationMaps = document.get("locations") as? List<Map<String, Any>> ?: return null
    val locations =
        locationMaps.mapNotNull { map ->
          mapToLocation(map["name"] as? Map<String, Any>) ?: return@mapNotNull null
        }
    // ChatGPT helped me with this part
    val routeSegmentMaps = document.get("routeSegments") as? List<Map<String, Any>> ?: return null
    val routeSegments =
        routeSegmentMaps.mapNotNull { map ->
          try {
            RouteSegment(
                from = mapToLocation(map["from"] as? Map<String, Any>) ?: return@mapNotNull null,
                to = mapToLocation(map["to"] as? Map<String, Any>) ?: return@mapNotNull null,
                distanceMeter =
                    (map["distanceMeter"] as? Number)?.toDouble() ?: return@mapNotNull null,
                durationMinutes =
                    (map["durationMinutes"] as? Number)?.toDouble() ?: return@mapNotNull null,
                path =
                    (map["path"] as? List<Map<String, Double>>)?.mapNotNull { coord ->
                      val lat = coord["latitude"] ?: return@mapNotNull null
                      val lon = coord["longitude"] ?: return@mapNotNull null
                      Coordinate(lat, lon)
                    } ?: emptyList(),
                transportMode = TransportMode.valueOf(map["transportMode"] as? String ?: "UNKNOWN"))
          } catch (e: Exception) {
            null
          }
        }

    return Trip(uid, name, startDate, endDate, ownerId, locations, routeSegments)
  }

  /**
   * Converts a map to a Location object.
   *
   * @param map The map to convert.
   * @return The Location object.
   */
  private fun mapToLocation(map: Map<String, Any>?): Location? {
    if (map == null) return null
    return try {
      val name = map["name"] as? String ?: return null
      val coordMap = map["coordinate"] as? Map<String, Double> ?: return null
      val lat = coordMap["latitude"] ?: return null
      val lon = coordMap["longitude"] ?: return null
      Location(name = name, coordinate = Coordinate(lat, lon))
    } catch (e: Exception) {
      null
    }
  }
}
