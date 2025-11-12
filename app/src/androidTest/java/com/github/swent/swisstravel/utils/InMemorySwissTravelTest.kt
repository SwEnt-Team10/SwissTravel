package com.github.swent.swisstravel.utils

import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripsRepository

open class InMemorySwissTravelTest : SwissTravelTest() {
  override fun createInitializedRepository(): TripsRepository {
    return InMemoryTripsRepository()
  }

  open class InMemoryTripsRepository(val tripList: MutableList<Trip> = mutableListOf<Trip>()) :
      TripsRepository {
    override fun getNewUid(): String {
      return "${tripList.size}"
    }

    override suspend fun getAllTrips(): List<Trip> {
      return tripList
    }

    override suspend fun getTrip(tripId: String): Trip {
      return tripList.first<Trip> { it.uid == tripId }
    }

    override suspend fun addTrip(trip: Trip) {
      tripList.add(trip)
    }

    override suspend fun editTrip(tripId: String, updatedTrip: Trip) {
      tripList.replaceAll { if (it.uid == tripId) updatedTrip else it }
    }

    override suspend fun deleteTrip(tripId: String) {
      tripList.removeIf { it.uid == tripId }
    }
  }
}
