package com.github.swent.swisstravel.model.trip

/** Inspired by the SwEnt Bootcamp solution. Represents a repository that manages Trips. */
interface TripsRepository {

  /** Generates and returns a new unique identifier for a Trip. */
  fun getNewUid(): String

  /**
   * Retrieves all Trips from the repository.
   *
   * @return A list of all Trips.
   */
  suspend fun getAllTrips(): List<Trip>

  /**
   * Retrieves a specific Trip by its unique identifier.
   *
   * @param tripId The unique identifier of the Trip to retrieve.
   * @return The Trip with the specified identifier.
   * @throws Exception if the Trip is not found.
   */
  suspend fun getTrip(tripId: String): Trip

  /**
   * Adds a new Trip to the repository.
   *
   * @param trip The Trip to add.
   */
  suspend fun addTrip(trip: Trip)

  /**
   * Deletes a Trip from the repository.
   *
   * @param tripId The unique identifier of the Trip to delete.
   * @throws Exception if the Trip is not found.
   */
  suspend fun deleteTrip(tripId: String)
}
