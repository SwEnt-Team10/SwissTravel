package com.android.swisstravel.ui.mytrips

import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripsRepository

class FakeTripsRepository(initialTrips: List<Trip> = emptyList()) : TripsRepository {
    var trips = initialTrips.toMutableList()

    override suspend fun getTrip(tripId: String): Trip {
        return trips.find { it.uid == tripId }!!
    }

    override suspend fun getAllTrips(): List<Trip> {
        return trips
    }

    override suspend fun addTrip(trip: Trip) {
        trips.add(trip)
    }

    override suspend fun deleteTrip(tripId: String) {
        trips.removeAll { it.uid == tripId }
    }

    override fun getNewUid(): String {
        return "new_trip_uid"
    }
}
