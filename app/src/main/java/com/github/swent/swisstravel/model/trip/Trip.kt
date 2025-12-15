package com.github.swent.swisstravel.model.trip

import android.net.Uri
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.google.firebase.Timestamp

/**
 * Represents a trip element that can be either a route segment or an activity.
 *
 * @property startDate The start date of the trip element.
 * @property endDate The end date of the trip element.
 */
sealed class TripElement(open val startDate: Timestamp, open val endDate: Timestamp) {
  data class TripSegment(val route: RouteSegment) : TripElement(route.startDate, route.endDate)

  data class TripActivity(val activity: Activity) :
      TripElement(activity.startDate, activity.endDate)
}

/**
 * Represents a trip.
 *
 * @property uid The unique identifier of the trip.
 * @property name The name of the trip.
 * @property ownerId The unique identifier of the owner of the trip.
 * @property locations The list of locations of the trip.
 * @property routeSegments The list of route segments of the trip.
 * @property activities The list of activities of the trip.
 * @property tripProfile The profile of the trip.
 * @property isCurrentTrip Whether the trip is the current trip or not.
 * @property uriLocation A map that contains uri's photos mapped with the locations
 * @property collaboratorsId The list of unique identifiers of the collaborators of the trip.
 * @property isRandom Whether the trip is random or not.
 * @property cachedActivities The list of activities that were fetched but not selected for the
 *   trip.
 * @property likedActivities The list of liked activities in the trip.
 * @property activitiesQueue The queue of activities to be swiped for the trip.
 * @property allFetchedForSwipe The list of all activities that have been fetched for swiping.
 */
data class Trip(
    val uid: String,
    val name: String,
    val ownerId: String,
    val locations: List<Location>,
    val routeSegments: List<RouteSegment>,
    val activities: List<Activity>,
    val tripProfile: TripProfile,
    val isCurrentTrip: Boolean,
    val collaboratorsId: List<String>,
    val isRandom: Boolean = false,
    val uriLocation: Map<Uri, Location>,
    val cachedActivities: List<Activity> = emptyList(),
    // fields for swipe and like activities
    val likedActivities: List<Activity> = emptyList(),
    val activitiesQueue: List<Activity> = emptyList(),
    val allFetchedForSwipe: List<Activity> = emptyList()
) {
  /**
   * Checks if the user is the owner of the trip.
   *
   * @param userId The unique identifier of the user.
   */
  fun isOwner(userId: String): Boolean = ownerId == userId

  /**
   * Checks if the user can edit the trip.
   *
   * @param userId The unique identifier of the user.
   */
  fun canEdit(userId: String): Boolean = isOwner(userId) || collaboratorsId.contains(userId)
}
