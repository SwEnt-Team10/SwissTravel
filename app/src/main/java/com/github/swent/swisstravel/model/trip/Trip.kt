package com.github.swent.swisstravel.model.trip

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
 * @property startDate The start date of the trip.
 * @property endDate The end date of the trip.
 * @property ownerId The unique identifier of the owner of the trip.
 * @property locations The list of locations of the trip.
 * @property routeSegments The list of route segments of the trip.
 * @property activities The list of activities of the trip.
 */
data class Trip(
    val uid: String,
    val name: String,
    val ownerId: String,
    val locations: List<Location>,
    val routeSegments: List<RouteSegment>,
    val activities: List<Activity>,
    val tripProfile: TripProfile
) {
  /**
   * Returns all trip elements ordered by start date.
   *
   * @return The list of trip elements.
   */
  fun getAllTripElementsOrdered(): List<TripElement> {
    val elements = mutableListOf<TripElement>()
    elements.addAll(routeSegments.map { TripElement.TripSegment(it) })
    elements.addAll(activities.map { TripElement.TripActivity(it) })
    return elements.sortedBy { it.startDate.seconds }
  }

  /**
   * Returns the upcoming trip elements.
   *
   * @param time The current time.
   * @param getCurrent If true, returns the current trip element too, otherwise only the upcoming
   * @return The list of upcoming trip elements.
   */
  fun getUpcomingTripElements(
      time: Timestamp = Timestamp.now(),
      getCurrent: Boolean = false
  ): List<TripElement> {
    val allElements = getAllTripElementsOrdered()

    val firstUpcomingIndex =
        if (getCurrent) {
          allElements.indexOfFirst { it.endDate.seconds > time.seconds }
        } else {
          allElements.indexOfFirst { it.startDate.seconds >= time.seconds }
        }

    /* There was no event during and after the current time */
    if (firstUpcomingIndex == -1) {
      return emptyList()
    }

    /* Return all upcoming events */
    return allElements.subList(firstUpcomingIndex, allElements.size)
  }

  /** Returns the total time of the trip in hours. */
  fun getTotalTime(): Double {
    return tripProfile.getTotalTime()
  }
}
