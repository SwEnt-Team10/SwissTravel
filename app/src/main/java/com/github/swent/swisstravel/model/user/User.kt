package com.github.swent.swisstravel.model.user

import com.github.swent.swisstravel.model.trip.TransportMode

/**
 * User data model representing a user in the application.
 *
 * @property uid Unique identifier for the user.
 * @property name Full name of the user.
 * @property biography Short biography of the user.
 * @property email Email address of the user.
 * @property profilePicUrl URL to the user's profile picture.
 * @property preferences List of user preferences.
 * @property friends List of user's friends.
 * @property stats User statistics.
 * @property pinnedTripsUids List of trip UIDs that the user has pinned.
 * @property pinnedPicturesUids List of picture UIDs that the user has pinned.
 * @property favoriteTripsUids List of trip UIDs that the user has marked as favorite.
 * @property currentTrip UID of the current trip the user has set.
 */
data class User(
    val uid: String,
    val name: String,
    val biography: String,
    val email: String,
    val profilePicUrl: String,
    val preferences: List<Preference>,
    val friends: List<Friend>,
    val stats: UserStats = UserStats(),
    val pinnedTripsUids: List<String>,
    val pinnedPicturesUids: List<String>,
    val favoriteTripsUids: List<String>,
    val currentTrip: String
)

/**
 * User data model representing a user's various statistics
 *
 * @property totalTrips Total number of trips the user has taken
 * @property totalTravelMinutes Total travel hours the user has taken
 * @property uniqueLocations Number of unique locations the user has traveled to
 * @property mostUsedTransportMode Most used transport mode by the user
 * @property longestRouteSegmentMin Longest route segment the user has traveled
 */
data class UserStats(
    val totalTrips: Int = 0,
    val totalTravelMinutes: Int = 0,
    val uniqueLocations: Int = 0,
    val mostUsedTransportMode: TransportMode? = null,
    val longestRouteSegmentMin: Int = 0
)

/**
 * A data model representing a friend
 *
 * @property uid Unique identifier for the friend
 * @property status Status of the friend request
 */
data class Friend(val uid: String, val status: FriendStatus)

/**
 * Enum representing the status of a friend request
 *
 * @property PENDING_INCOMING Incoming friend request
 * @property PENDING_OUTGOING Outgoing friend request
 * @property ACCEPTED Accepted friend request
 */
enum class FriendStatus {
  PENDING_INCOMING,
  PENDING_OUTGOING,
  ACCEPTED
}

/**
 * Data class to represent the fields that can be updated in a user
 *
 * @property name Full name of the user
 * @property biography Short biography of the user
 * @property profilePicUrl URL to the user's profile picture
 * @property preferences List of user preferences
 * @property pinnedTripsUids List of trip UIDs that the user has pinned
 * @property pinnedPicturesUids List of picture UIDs that the user has pinned
 * @property currentTrip UID of the current trip
 */
data class UserUpdate(
    val name: String? = null,
    val biography: String? = null,
    val profilePicUrl: String? = null,
    val preferences: List<Preference>? = null,
    val pinnedTripsUids: List<String>? = null,
    val pinnedPicturesUids: List<String>? = null,
    val currentTrip: String? = null
) {
  /** Helper to convert non-null fields to a Map for Firestore updates */
  fun toMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>()
    if (name != null) map["name"] = name
    if (biography != null) map["biography"] = biography
    if (profilePicUrl != null) map["profilePicUrl"] = profilePicUrl
    if (preferences != null) map["preferences"] = preferences
    if (pinnedTripsUids != null) map["pinnedTripsUids"] = pinnedTripsUids
    if (pinnedPicturesUids != null) map["pinnedPicturesUids"] = pinnedPicturesUids
    if (currentTrip != null) map["currentTrip"] = currentTrip
    return map
  }
}
