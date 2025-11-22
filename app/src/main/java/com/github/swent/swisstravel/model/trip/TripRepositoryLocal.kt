package com.github.swent.swisstravel.model.trip

/**
 * A trip repository locally stored (inspiration taken from the bootcamp)
 */
class TripRepositoryLocal(
    private val trips: MutableList<Trip> = mutableListOf(),
    private var counter: Int = 0
) : TripsRepository{
    override fun getNewUid(): String {
        return (counter++).toString()
    }

    override suspend fun getAllTrips(): List<Trip> {
        return trips
    }

    override suspend fun getTrip(tripId: String): Trip {
        return trips.find { it.uid == tripId }
            ?: throw Exception("TripRepositoryLocal: Trip not found")
    }

    override suspend fun addTrip(trip: Trip) {
        trips.add(trip)
    }

    override suspend fun editTrip(tripId: String, updatedTrip: Trip) {
        val index = trips.indexOfFirst { it.uid == tripId }
        if (index != -1) {
            trips[index] = updatedTrip
        } else {
            throw Exception("TripRepositoryLocal: Trip not found")
        }
    }

    override suspend fun deleteTrip(tripId: String) {
        val index = trips.indexOfFirst { it.uid == tripId }
        if (index != -1) {
            trips.removeAt(index)
        } else {
            throw Exception("TripRepositoryLocal: Trip not found")
        }
    }
}