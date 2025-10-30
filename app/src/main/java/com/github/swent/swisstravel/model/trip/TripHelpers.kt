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
 * A trip is considered upcoming when its `startDate` is strictly after the current instant.
 * Comparison is performed using `ZonedDateTime` in the system default time zone.
 *
 * @return `true` if the trip's `startDate` is after now, otherwise `false`
 * @receiver Trip the trip to evaluate
 */
fun Trip.isUpcoming(): Boolean {
  val now = ZonedDateTime.now()
  val end = tripProfile.endDate.toZonedDateTime()
  return end.isAfter(now)
}

/**
 * Check whether the trip has been set as the current trip.
 *
 * @return `true` if the trip is set as the current trip by the user, otherwise `false`
 * @receiver Trip the trip to evaluate
 */
fun Trip.isCurrent(): Boolean {
  return isCurrentTrip
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
