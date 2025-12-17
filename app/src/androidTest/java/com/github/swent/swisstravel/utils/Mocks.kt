package com.github.swent.swisstravel.utils

import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.model.user.User
import com.github.swent.swisstravel.model.user.UserRepository
import com.github.swent.swisstravel.model.user.UserStats

// Made with the help of AI.

/**
 * A reusable Fake TripsRepository for UI tests. Allows seeding with a list of trips and supports
 * basic CRUD operations in memory.
 */
open class FakeTripsRepository(private val trips: MutableList<Trip> = mutableListOf()) :
    TripsRepository {

  override suspend fun getAllTrips(): List<Trip> = trips

  override suspend fun getTrip(tripId: String): Trip {
    return trips.find { it.uid == tripId } ?: throw Exception("Trip not found: $tripId")
  }

  override suspend fun addTrip(trip: Trip) {
    trips.add(trip)
  }

  override suspend fun deleteTrip(tripId: String) {
    trips.removeIf { it.uid == tripId }
  }

  override suspend fun editTrip(tripId: String, updatedTrip: Trip) {
    trips.removeIf { it.uid == tripId }
    trips.add(updatedTrip)
  }

  override fun getNewUid(): String = "fake-uid-${trips.size + 1}"

  override suspend fun shareTripWithUsers(tripId: String, userIds: List<String>) {
    // no-op for basic UI tests
  }

  override suspend fun removeCollaborator(tripId: String, userId: String) {
    // no-op for basic UI tests
  }
}

/** A reusable Fake UserRepository for UI tests. Supports adding users for collaborator lookups. */
class FakeUserRepository : UserRepository {
  private val users = mutableMapOf<String, User>()

  init {
    val currentUser =
        User(
            "current",
            "Current User",
            "",
            "email",
            "",
            emptyList(),
            emptyList(),
            UserStats(),
            emptyList(),
            emptyList(),
            emptyList(),
            currentTrip = "")
    users[currentUser.uid] = currentUser
  }

  fun addUser(user: User) {
    users[user.uid] = user
  }

  override suspend fun getCurrentUser(): User = users["current"]!!

  override suspend fun getUserByUid(uid: String): User? = users[uid]

  override suspend fun getUserByNameOrEmail(query: String): List<User> = emptyList()

  override suspend fun updateUserPreferences(uid: String, preferences: List<Preference>) {}

  override suspend fun updateUserStats(uid: String, stats: UserStats) {}

  override suspend fun sendFriendRequest(fromUid: String, toUid: String) {}

  override suspend fun acceptFriendRequest(currentUid: String, fromUid: String) {}

  override suspend fun removeFriend(uid: String, friendUid: String) {}

  override suspend fun updateUser(
      uid: String,
      name: String?,
      biography: String?,
      profilePicUrl: String?,
      preferences: List<Preference>?,
      pinnedTripsUids: List<String>?,
      pinnedPicturesUids: List<String>?,
      currentTrip: String?
  ) {
    // 1. Get the existing user
    val existingUser = users[uid] ?: return

    // 2. Create a copy with the updated fields (if they are not null)
    val updatedUser =
        existingUser.copy(
            name = name ?: existingUser.name,
            biography = biography ?: existingUser.biography,
            profilePicUrl = profilePicUrl ?: existingUser.profilePicUrl,
            preferences = preferences ?: existingUser.preferences,
            pinnedTripsUids = pinnedTripsUids ?: existingUser.pinnedTripsUids,
            pinnedPicturesUids = pinnedPicturesUids ?: existingUser.pinnedPicturesUids,
            // Crucial for your test: update currentTrip if provided
            currentTrip = currentTrip ?: existingUser.currentTrip)

    // 3. Save it back to the map
    users[uid] = updatedUser
  }

  override suspend fun addFavoriteTrip(uid: String, tripUid: String) {
    users[uid] = users[uid]!!.copy(favoriteTripsUids = users[uid]!!.favoriteTripsUids + tripUid)
  }

  override suspend fun removeFavoriteTrip(uid: String, tripUid: String) {
    users[uid] = users[uid]!!.copy(favoriteTripsUids = users[uid]!!.favoriteTripsUids - tripUid)
  }
}
