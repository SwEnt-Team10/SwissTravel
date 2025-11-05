package com.github.swent.swisstravel.algorithm.tripschedule

import com.github.swent.swisstravel.algorithm.orderlocations.OrderedRoute
import com.github.swent.swisstravel.model.trip.RouteSegment
import com.github.swent.swisstravel.model.trip.TransportMode
import com.github.swent.swisstravel.model.trip.TripElement
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.google.firebase.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * The schedule parameters. This will be used in the main algorithm when you call scheduleTrip(),
 * the parameters dayStart and dayEnd could be freely modified depending on which preferences the
 * user has.
 *
 * @property dayStart the day start
 * @property dayEnd the day end
 * @property pauseBetweenEachActivity the pause between each activity
 */
data class ScheduleParams(
    val dayStart: LocalTime = LocalTime.of(9, 0),
    val dayEnd: LocalTime = LocalTime.of(18, 0),
    val pauseBetweenEachActivity: Int = 60 * 15
)

/** Helper to convert LocalDateTime to Timestamp. */
private fun LocalDateTime.toTs(): Timestamp =
    Timestamp(this.atZone(ZoneId.systemDefault()).toEpochSecond(), 0)

/**
 * Builds a chronological trip schedule by assigning start and end times to activities and route
 * segments based on a daily time window.
 *
 * The function takes an [OrderedRoute] (a list of locations and travel durations between them) and
 * a list of [Activity] objects, and produces a sequence of [TripElement]s (activities and routes)
 * scheduled day-by-day starting from a given date.
 *
 * ### Scheduling Rules
 * - The day starts at [ScheduleParams.dayStart] and ends at [ScheduleParams.dayEnd].
 * - Activities are placed at their associated location in order of [OrderedRoute.orderedLocations].
 * - After each activity, a pause of [ScheduleParams.pauseBetweenEachActivity] seconds is inserted
 *   before the next travel segment or activity begins.
 * - If an activity or route segment would exceed the daily limit ([ScheduleParams.dayEnd]), it is
 *   deferred to the next day starting at [ScheduleParams.dayStart].
 * - The resulting elements are returned sorted chronologically by their [TripElement.startDate].
 *
 * ### Example
 * Suppose we start on `2025-06-01` with:
 * - Day start = 09:00, day end = 18:00
 * - A route visiting locations A → B → C
 * - Activities of 2 hours each, and travel times of 1 hour between locations
 *
 * Then:
 * ```
 * 09:00–11:00 → Activity at A
 * 11:00–12:00 → Travel A→B
 * 12:00–14:00 → Activity at B
 * 14:00–15:00 → Travel B→C
 * 15:00–17:00 → Activity at C
 * ```
 *
 * ### Parameters
 *
 * @param tripStart The starting [LocalDate] for the trip (e.g., the first travel day).
 * @param ordered The optimized route output from [orderLocations], defining location order and
 *   segment durations.
 * @param activities The list of [Activity]s to schedule along the route. Each activity must have a
 *   [Location] matching one in [ordered.orderedLocations].
 * @param params Optional [ScheduleParams] that define the daily scheduling constraints such as
 *   start/end hours and the pause between activities.
 *
 * ### Returns
 * A list of [TripElement]s (either [TripElement.TripActivity] or [TripElement.TripSegment]) with
 * properly assigned [Timestamp] start and end times, sorted by start time.
 *
 * @see TripElement
 * @see ScheduleParams
 * @see OrderedRoute
 * @see Activity
 */
fun scheduleTrip(
    tripStart: LocalDate,
    ordered: OrderedRoute,
    activities: List<Activity>,
    params: ScheduleParams = ScheduleParams()
): List<TripElement> {

  // TODO: Take into account numerous preferences and other transport modes and discuss design of
  // skipping a day or not
  if (ordered.orderedLocations.isEmpty()) return emptyList()

  var currentDay = tripStart
  var cursor = LocalDateTime.of(currentDay, params.dayStart)
  val elements = mutableListOf<TripElement>()

  val locs = ordered.orderedLocations
  val segments = ordered.segmentDuration

  fun advanceToNextDay() {
    currentDay = currentDay.plusDays(1)
    cursor = LocalDateTime.of(currentDay, params.dayStart)
  }

  fun fitsInDay(durationSec: Int): Boolean {
    val endOfDay = LocalDateTime.of(currentDay, params.dayEnd)
    return !cursor.plusSeconds(durationSec.toLong()).isAfter(endOfDay)
  }

  for (i in locs.indices) {
    val location = locs[i]
    val activitiesHere = activities.filter { it.location == location }

    for (act in activitiesHere) {
      val durationSec = act.estimatedTime

      if (!fitsInDay(durationSec)) advanceToNextDay()

      val start = cursor
      val end = start.plusSeconds(durationSec.toLong())

      val scheduled = act.copy(startDate = start.toTs(), endDate = end.toTs())
      elements += TripElement.TripActivity(scheduled)

      cursor = end
    }

    if (i < locs.lastIndex) {
      val driveSec = segments[i]
      if (!fitsInDay(driveSec.toInt())) advanceToNextDay()

      val segStart = cursor
      val segEnd = segStart.plusSeconds(driveSec.toLong())

      val seg =
          RouteSegment(
              from = locs[i],
              to = locs[i + 1],
              distanceMeter = 0,
              durationMinutes = driveSec.toInt() / 60,
              path = emptyList(),
              transportMode = TransportMode.BUS,
              startDate = segStart.toTs(),
              endDate = segEnd.toTs())
      elements += TripElement.TripSegment(seg)
      cursor = segEnd.plusSeconds(params.pauseBetweenEachActivity.toLong())
    }
  }

  return elements.sortedBy { it.startDate.seconds }
}
