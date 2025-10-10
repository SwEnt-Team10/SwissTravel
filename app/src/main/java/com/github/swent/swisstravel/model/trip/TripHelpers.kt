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
  return startDate.toZonedDateTime().isAfter(now)
}

/**
 * Check whether the trip is currently ongoing.
 *
 * A trip is considered current when the current instant is between the trip's `startDate` and
 * `endDate`, inclusive. Dates are compared as `ZonedDateTime` using the system default zone.
 *
 * @return `true` if now is between `startDate` and `endDate` (inclusive), otherwise `false`
 * @receiver Trip the trip to evaluate
 */
fun Trip.isCurrent(): Boolean {
  val now = ZonedDateTime.now()
  val start = startDate.toZonedDateTime()
  val end = endDate.toZonedDateTime()
  return !start.isAfter(now) && !end.isBefore(now)
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
  return endDate.toZonedDateTime().isBefore(now)
}
