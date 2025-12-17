package com.github.swent.swisstravel

import android.net.Uri
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.RouteSegment
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.github.swent.swisstravel.model.user.Friend
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.model.user.User
import com.github.swent.swisstravel.model.user.UserRepository
import com.github.swent.swisstravel.model.user.UserStats
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** Shared helper to create a Trip with default values for Unit Tests. */
fun createTestTrip(
    uid: String = "1",
    name: String = "Test Trip",
    ownerId: String = "ownerX",
    locations: List<Location> = emptyList(),
    routeSegments: List<RouteSegment> = emptyList(),
    activities: List<Activity> = emptyList(),
    startDate: Timestamp = Timestamp.now(),
    endDate: Timestamp = Timestamp.now(),
    preferredLocations: List<Location> = emptyList(),
    preferences: List<Preference> = emptyList(),
    adults: Int = 1,
    children: Int = 0,
    departureLocation: Location? = null,
    arrivalLocation: Location? = null,
    uriLocation: Map<Uri, Location> = emptyMap(),
    collaboratorsId: List<String> = emptyList(),
    isRandom: Boolean = false
): Trip {
  val profile =
      TripProfile(
          startDate = startDate,
          endDate = endDate,
          preferredLocations = preferredLocations,
          preferences = preferences,
          adults = adults,
          children = children,
          departureLocation = departureLocation,
          arrivalLocation = arrivalLocation)
  return Trip(
      uid = uid,
      name = name,
      ownerId = ownerId,
      locations = locations,
      routeSegments = routeSegments,
      activities = activities,
      tripProfile = profile,
      uriLocation = uriLocation,
      collaboratorsId = collaboratorsId,
      isRandom = isRandom)
}

/** Shared helper to create a User with default values for Unit Tests. */
fun createTestUser(
    uid: String = "123",
    name: String = "Test User",
    biography: String = "Bio",
    email: String = "test@example.com",
    profilePicUrl: String = "http://example.com/pic.jpg",
    preferences: List<Preference> = emptyList(),
    friends: List<Friend> = emptyList(),
    stats: UserStats = UserStats(),
    pinnedTripsUids: List<String> = emptyList(),
    pinnedPicturesUids: List<String> = emptyList(),
    favoriteTripsUids: List<String> = emptyList(),
    currentTripUid: String = ""
): User {
  return User(
      uid = uid,
      name = name,
      biography = biography,
      email = email,
      profilePicUrl = profilePicUrl,
      preferences = preferences,
      friends = friends,
      stats = stats,
      pinnedTripsUids = pinnedTripsUids,
      pinnedPicturesUids = pinnedPicturesUids,
      favoriteTripsUids = favoriteTripsUids,
      currentTrip = currentTripUid)
}

/**
 * A JUnit Rule that sets the Main dispatcher to the provided TestDispatcher. This ensures ViewModel
 * operations running on Main use the same scheduler as the test.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(val testDispatcher: TestDispatcher = StandardTestDispatcher()) :
    TestWatcher() {
  override fun starting(description: Description?) {
    Dispatchers.setMain(testDispatcher)
  }

  override fun finished(description: Description?) {
    Dispatchers.resetMain()
  }
}

/** A fake repository that records added trips and can be made to throw on addTrip. */
class FakeTripsRepository : TripsRepository {
  var addedTrip: Trip? = null
  var shouldThrow: Boolean = false

  // match the interface: non-suspending
  override fun getNewUid(): String = "fake-uid"

  override suspend fun addTrip(trip: Trip) {
    if (shouldThrow) throw Exception("boom")
    addedTrip = trip
  }

  // minimal stubs required by the interface
  override suspend fun getAllTrips(): List<Trip> = emptyList()

  override suspend fun getTrip(tripId: String): Trip = throw NotImplementedError()

  override suspend fun deleteTrip(tripId: String) {}

  override suspend fun shareTripWithUsers(tripId: String, userIds: List<String>) {}

  override suspend fun removeCollaborator(tripId: String, userId: String) {}

  override suspend fun editTrip(tripId: String, updatedTrip: Trip) {}
}

/** A fake repository used for basic user interactions */
class FakeUserRepository : UserRepository {
  override suspend fun getCurrentUser(): User {
    return createTestUser(
        uid = "test-user", name = "Test User", preferences = listOf(Preference.FOODIE))
  }

  override suspend fun getUserByUid(uid: String): User? = null

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
      currentTrip: String?,
  ) {}

  override suspend fun addFavoriteTrip(uid: String, tripUid: String) {}

  override suspend fun removeFavoriteTrip(uid: String, tripUid: String) {}
}
