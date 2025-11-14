package com.github.swent.swisstravel.utils

import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripsRepository

/**
 * Test implementation of [SwissTravelTest] that uses an in-memory repository.
 *
 * This avoids any network or Firestore interactions and is ideal for unit tests.
 */
open class InMemorySwissTravelTest : SwissTravelTest() {
  override fun createInitializedRepository(): TripsRepository {
    return InMemoryTripsRepository()
  }

  /**
   * Simple in-memory implementation of [TripsRepository] backed by a mutable list.
   *
   * Used for tests where persistence is not needed.
   *
   * @property tripList the internal storage for trips
   */
  open class InMemoryTripsRepository(val tripList: MutableList<Trip> = mutableListOf()) :
      TripsRepository {

    /** Generates a new UID based on the current number of stored trips. */
    override fun getNewUid(): String {
      return "${tripList.size}"
    }

    /** Returns all trips stored in memory. */
    override suspend fun getAllTrips(): List<Trip> {
      return tripList
    }

    /**
     * Retrieves a trip by its UID.
     *
     * @throws NoSuchElementException if no trip matches the given ID
     */
    override suspend fun getTrip(tripId: String): Trip {
      return tripList.first { it.uid == tripId }
    }

    /** Adds a new trip to the repository. */
    override suspend fun addTrip(trip: Trip) {
      tripList.add(trip)
    }

    /**
     * Replaces an existing trip with an updated version.
     *
     * If the trip does not exist, nothing happens.
     */
    override suspend fun editTrip(tripId: String, updatedTrip: Trip) {
      tripList.replaceAll { if (it.uid == tripId) updatedTrip else it }
    }

    /** Deletes a trip by its UID. */
    override suspend fun deleteTrip(tripId: String) {
      tripList.removeIf { it.uid == tripId }
    }
  }
}
