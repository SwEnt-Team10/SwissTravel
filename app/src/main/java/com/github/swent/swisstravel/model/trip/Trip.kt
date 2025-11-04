package com.github.swent.swisstravel.model.trip

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
 * @property isFavorite Whether the trip is a favorite or not.
 */
data class Trip(
    val uid: String,
    val name: String,
    val ownerId: String,
    val locations: List<Location>,
    val routeSegments: List<RouteSegment>,
    val activities: List<Activity>,
    val tripProfile: TripProfile,
    val isFavorite: Boolean
)
