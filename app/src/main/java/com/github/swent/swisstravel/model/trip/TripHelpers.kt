package com.github.swent.swisstravel.model.trip

import com.google.firebase.Timestamp
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Convert a Firebase `Timestamp` to a `ZonedDateTime` using the system default time zone.
 *
 * @return a `ZonedDateTime` representing the same instant in the system default zone
 * @receiver Timestamp the Firebase timestamp to convert
 */
fun Timestamp.toZonedDateTime(): ZonedDateTime =
    this.toDate().toInstant().atZone(ZoneId.systemDefault())

/**
 * Check whether the trip is upcoming.
 *
 * A trip is considered upcoming when its `endDate` is strictly after the current instant.
 * Comparison is performed using `ZonedDateTime` in the system default time zone.
 *
 * @param userCurrentTrip The UID of the user's current trip.
 * @return `true` if the trip's `endDate` is after now and the trip is not the current trip,
 *   otherwise `false`
 * @receiver Trip the trip to evaluate
 */
fun Trip.isUpcoming(userCurrentTrip: String?): Boolean {
  val now = ZonedDateTime.now()
  return tripProfile.endDate.toZonedDateTime().isAfter(now) && this.uid != userCurrentTrip
}

/**
 * Check whether the trip is currently ongoing.
 *
 * A trip is considered current when the current instant is between the trip's `startDate` and
 * `endDate`, inclusive. Dates are compared as `ZonedDateTime` using the system default zone.
 *
 * @param userCurrentTrip The UID of the user's current trip.
 * @return `true` if now is between `startDate` and `endDate` (inclusive), otherwise `false`
 * @receiver Trip the trip to evaluate
 */
fun Trip.isCurrent(userCurrentTrip: String?): Boolean {
  return this.uid == userCurrentTrip && !isUpcoming(userCurrentTrip) && !isPast()
}

/**
 * Check whether the trip is in the past.
 *
 * A trip is considered past when its `endDate` is strictly before the current instant. Comparison
 * is performed using `ZonedDateTime` in the system default time zone.
 *
 * @return `true` if the trip's `endDate` is before now, otherwise `false`
 * @receiver Trip the trip to evaluate
 */
fun Trip.isPast(): Boolean {
  val now = ZonedDateTime.now()
  return tripProfile.endDate.toZonedDateTime().isBefore(now)
}

/**
 * Returns all trip elements ordered by start date.
 *
 * @return The list of trip elements.
 */
fun Trip.getAllTripElementsOrdered(): List<TripElement> {
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
fun Trip.getUpcomingTripElements(
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
  return if (firstUpcomingIndex == -1) emptyList()
  /* Return all upcoming events */
  else allElements.subList(firstUpcomingIndex, allElements.size)
}

/**
 * Returns the total time of the trip in hours.
 *
 * @return The total time of the trip in hours
 */
fun Trip.getTotalTime(): Double = tripProfile.getTotalTime()
