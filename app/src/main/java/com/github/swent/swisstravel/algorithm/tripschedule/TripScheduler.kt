package com.github.swent.swisstravel.algorithm.tripschedule

import com.github.swent.swisstravel.algorithm.orderlocations.OrderedRoute
import com.github.swent.swisstravel.model.trip.RouteSegment
import com.github.swent.swisstravel.model.trip.TransportMode
import com.github.swent.swisstravel.model.trip.TripElement
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.github.swent.swisstravel.model.user.Preference
import com.google.firebase.Timestamp
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

/**
 * The schedule parameters. This will be used in the main algorithm when you call scheduleTrip(),
 * the parameters dayStart and dayEnd could be freely modified depending on which preferences the
 * user has.
 *
 * @property dayStart the day start
 * @property dayEnd the day end
 * @property pauseBetweenEachActivity the pause between each activity
 * @property maxActivitiesPerDay the maximum activities per day
 * @property travelEnd the travel end
 */
data class ScheduleParams(
    val dayStart: LocalTime = LocalTime.of(8, 0),
    val dayEnd: LocalTime = LocalTime.of(18, 0),
    val travelEnd: LocalTime = LocalTime.of(22, 0),
    val maxActivitiesPerDay: Int = 4,
    val pauseBetweenEachActivity: Int = 60 * 15
)

/** Helper to convert LocalDateTime to Timestamp. */
private fun LocalDateTime.toTs(): Timestamp =
    Timestamp(this.atZone(ZoneId.systemDefault()).toEpochSecond(), 0)

/**
 * Round up to the next quarter-hour (:00, :15, :30, :45), but keep the same time if already exactly
 * on a quarter.
 */
private fun LocalDateTime.roundUpToQuarter(): LocalDateTime {
  val t = this.truncatedTo(ChronoUnit.MINUTES)
  val remainder = t.minute % 15
  if (remainder == 0) return t
  return t.plusMinutes((15 - remainder).toLong())
}

/**
 * Builds a chronological trip schedule by assigning start/end times to activities and travel legs,
 * honoring daily windows, quarter-hour alignment, and a daily activity cap.
 *
 * ### Inputs
 * - [tripProfile] supplies the trip start date (via [TripProfile.startDate]) and user preferences.
 * - [ordered] is the TSP-ordered route (locations and segment durations) from `orderLocations`.
 * - [activities] are the activities to place; each activity must reference a [Location] that
 *   appears in [ordered.orderedLocations]. The algorithm preserves their per-location order.
 * - [params] are the base scheduling constraints (possibly adjusted by preferences).
 *
 * ### Preference Overrides (applied on top of [params])
 * - `NIGHT_OWL` or `NIGHTLIFE`:
 *     - `dayStart = 10:00`, `dayEnd = 22:00`, `travelEnd = 00:00` (midnight).
 * - `EARLY_BIRD`:
 *     - `dayStart = 06:00`, `travelEnd = 20:00`, `maxActivitiesPerDay = 6`.
 * - `SLOW_PACE`: `pauseBetweenEachActivity = 3600` (1h).
 * - `QUICK`: `pauseBetweenEachActivity = 0`.
 *
 * ### Scheduling Rules
 * - The cursor starts at the trip start **date** (from [tripProfile.startDate]) and
 *   [ScheduleParams.dayStart], then is **rounded up to the next quarter**.
 * - Activities:
 *     - Placed only within `[dayStart, dayEnd]` of the current day.
 *     - If an activity doesn't fit, the scheduler advances to the **next day** at `dayStart`.
 *     - A maximum of [ScheduleParams.maxActivitiesPerDay] activities per day is enforced.
 * - Travel (route segments):
 *     - A pause of [ScheduleParams.pauseBetweenEachActivity] seconds is added **before** travel,
 *       then the cursor is rounded up to the next quarter.
 *     - Travel must finish by [ScheduleParams.travelEnd] (which may be later than [dayEnd]). If it
 *       cannot, the scheduler moves travel to the next day at `dayStart`.
 * - All start/end times (activities and travel) are quarter-aligned (using `roundUpToQuarter()`).
 * - The resulting elements are returned sorted by start time.
 *
 * ### Example
 * Given:
 * - Day window 09:00–18:00, travel until 22:00
 * - Route A → B with 30 minutes travel
 * - Activities: A(60m), B(60m)
 * - Pause 15m (before travel only)
 *
 * The schedule is:
 * - 09:00–10:00 Activity @ A
 * - 10:00–10:30 Travel A→B
 * - 10:45–11:45 Activity @ B (pause 15m has been applied before the travel only)
 *
 * @param tripProfile Trip context (start date, preferences, etc.)
 * @param ordered Ordered route with segment durations in seconds.
 * @param activities Activities to place; `estimatedTime` is in seconds.
 * @param params Base constraints (may be tweaked by preferences).
 * @return A chronologically sorted list of [TripElement]s (activities and route segments) with
 *   Firebase [Timestamp]s for start and end.
 *
 * ### Notes & Limitations
 * - The function does **not** check venue opening hours; it only enforces daily windows.
 * - If [ordered.orderedLocations] is empty, the function returns an empty list.
 */
fun scheduleTrip(
    tripProfile: TripProfile,
    ordered: OrderedRoute,
    activities: List<Activity>,
    params: ScheduleParams = ScheduleParams()
): List<TripElement> {

  val preferences = tripProfile.preferences
  var effectiveParams = params

  if (preferences.contains(Preference.NIGHT_OWL) || preferences.contains(Preference.NIGHTLIFE)) {
    effectiveParams =
        effectiveParams.copy(
            dayStart = LocalTime.of(10, 0),
            dayEnd = LocalTime.of(22, 0),
            travelEnd = LocalTime.of(23, 59))
  }

  if (preferences.contains(Preference.EARLY_BIRD)) {
    effectiveParams =
        effectiveParams.copy(
            dayStart = LocalTime.of(6, 0), travelEnd = LocalTime.of(20, 0), maxActivitiesPerDay = 6)
  }

  if (preferences.contains(Preference.SLOW_PACE)) {
    effectiveParams = effectiveParams.copy(pauseBetweenEachActivity = 60 * 60)
  }

  if (preferences.contains(Preference.QUICK)) {
    effectiveParams = effectiveParams.copy(pauseBetweenEachActivity = 0)
  }

  if (ordered.orderedLocations.isEmpty()) return emptyList()

  var currentDay = tripProfile.startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
  var cursor = LocalDateTime.of(currentDay, effectiveParams.dayStart).roundUpToQuarter()
  var activitiesToday = 0
  val elements = mutableListOf<TripElement>()

  val locs = ordered.orderedLocations
  val segments = ordered.segmentDuration

  fun resetToNextDay() {
    currentDay = currentDay.plusDays(1)
    cursor = LocalDateTime.of(currentDay, effectiveParams.dayStart).roundUpToQuarter()
    activitiesToday = 0
  }

  fun fitsInActivityWindow(durationSec: Int): Boolean {
    val endOfDay = LocalDateTime.of(currentDay, effectiveParams.dayEnd)
    return !cursor.plusSeconds(durationSec.toLong()).isAfter(endOfDay)
  }

  fun fitsInTravelWindow(durationSec: Int): Boolean {
    val travelCutoff = LocalDateTime.of(currentDay, effectiveParams.travelEnd)
    return !cursor.plusSeconds(durationSec.toLong()).isAfter(travelCutoff)
  }

  for (i in locs.indices) {
    val location = locs[i]
    // Activities *at* this location, in given order
    val activitiesHere = activities.filter { it.location == location }

    for (act in activitiesHere) {
      val durationSec = act.estimatedTime

      // Respect daily activity cap
      if (activitiesToday >= effectiveParams.maxActivitiesPerDay) {
        resetToNextDay()
      }

      // Ensure we start on a quarter
      cursor = cursor.roundUpToQuarter()

      // If it does not fit today, move to next day
      if (!fitsInActivityWindow(durationSec)) {
        resetToNextDay()
      }

      val start = cursor
      val end = start.plusSeconds(durationSec.toLong()).roundUpToQuarter()

      val scheduled = act.copy(startDate = start.toTs(), endDate = end.toTs())
      elements += TripElement.TripActivity(scheduled)

      activitiesToday += 1
      cursor = end // next item starts *after* rounded end
    }

    // Add travel segment to next location
    if (i < locs.lastIndex) {
      val driveSec = segments[i].toInt()

      // Pause after the last thing, then round
      cursor =
          cursor.plusSeconds(effectiveParams.pauseBetweenEachActivity.toLong()).roundUpToQuarter()

      // If travel can't finish today (by travelEnd), move to next day (start at dayStart)
      if (!fitsInTravelWindow(driveSec)) {
        resetToNextDay()
      }

      val segStart = cursor
      val segEnd = segStart.plusSeconds(driveSec.toLong()).roundUpToQuarter()

      val seg =
          RouteSegment(
              from = locs[i],
              to = locs[i + 1],
              distanceMeter = 0,
              durationMinutes = ceil(driveSec / 60.0).toInt(),
              path = emptyList(),
              transportMode = TransportMode.BUS,
              startDate = segStart.toTs(),
              endDate = segEnd.toTs())
      elements += TripElement.TripSegment(seg)

      cursor = segEnd
    }
  }

  return elements.sortedBy { it.startDate.seconds }
}
