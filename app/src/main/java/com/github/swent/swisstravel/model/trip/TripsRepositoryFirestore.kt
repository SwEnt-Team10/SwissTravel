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
    TODO()
  }
}
