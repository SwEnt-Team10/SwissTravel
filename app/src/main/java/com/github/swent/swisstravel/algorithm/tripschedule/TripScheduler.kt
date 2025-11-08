package com.github.swent.swisstravel.algorithm.tripschedule

import com.github.swent.swisstravel.algorithm.orderlocations.OrderedRoute
import com.github.swent.swisstravel.model.trip.RouteSegment
import com.github.swent.swisstravel.model.trip.TransportMode
import com.github.swent.swisstravel.model.trip.TripElement
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.github.swent.swisstravel.model.user.Preference
import com.google.firebase.Timestamp
import java.time.LocalDate
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
 * This function changes the schedule parameters depending on which preferences are on.
 *
 * @param profile the trip profile
 * @param base the base schedule parameters
 * @return the updated schedule parameters
 */
private fun applyPreferenceOverrides(profile: TripProfile, base: ScheduleParams): ScheduleParams {
  var eff = base

  val prefs = profile.preferences.toSet()

  if ((prefs.contains(Preference.NIGHTLIFE) || prefs.contains(Preference.NIGHT_OWL)) &&
      prefs.contains(Preference.EARLY_BIRD)) {
    eff =
        eff.copy(
            dayStart = LocalTime.of(6, 0), travelEnd = LocalTime.of(22, 0), maxActivitiesPerDay = 6)
  } else if (prefs.contains(Preference.NIGHTLIFE) || prefs.contains(Preference.NIGHT_OWL)) {
    eff =
        eff.copy(
            dayStart = LocalTime.of(10, 0),
            dayEnd = LocalTime.of(22, 0),
            travelEnd = LocalTime.of(23, 59))
  } else if (prefs.contains(Preference.EARLY_BIRD)) {
    eff =
        eff.copy(
            dayStart = LocalTime.of(6, 0), travelEnd = LocalTime.of(20, 0), maxActivitiesPerDay = 6)
  }

  if (prefs.contains(Preference.SLOW_PACE)) {
    eff = eff.copy(pauseBetweenEachActivity = 60 * 60)
  } else if (prefs.contains(Preference.QUICK)) {
    eff = eff.copy(pauseBetweenEachActivity = 0)
  }
  return eff
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
  if (ordered.orderedLocations.isEmpty()) return emptyList()

  val eff = applyPreferenceOverrides(tripProfile, params)
  val zone = ZoneId.systemDefault()
  var currentDay: LocalDate = tripProfile.startDate.toInstant().atZone(zone).toLocalDate()
  var cursor: LocalDateTime = LocalDateTime.of(currentDay, eff.dayStart).roundUpToQuarter()
  var activitiesToday = 0
  val out = mutableListOf<TripElement>()

  val locs = ordered.orderedLocations
  val legs = ordered.segmentDuration

  /** Advance the cursor to the next day. */
  fun nextDay() {
    currentDay = currentDay.plusDays(1)
    cursor = LocalDateTime.of(currentDay, eff.dayStart).roundUpToQuarter()
    activitiesToday = 0
  }

  /** Checks if the activity fits within the current day's window. */
  fun fitsActivity(durationSec: Int): Boolean =
      !cursor.plusSeconds(durationSec.toLong()).isAfter(LocalDateTime.of(currentDay, eff.dayEnd))

  /** Checks if the travel (RouteSegment) fits within the current day's window. */
  fun fitsTravel(durationSec: Int): Boolean =
      !cursor.plusSeconds(durationSec.toLong()).isAfter(LocalDateTime.of(currentDay, eff.travelEnd))

  /** Schedules an activity. */
  fun scheduleActivity(act: Activity) {
    // Daily cap
    if (activitiesToday >= eff.maxActivitiesPerDay) nextDay()

    cursor = cursor.roundUpToQuarter()
    if (!fitsActivity(act.estimatedTime)) nextDay()

    val start = cursor
    val end = start.plusSeconds(act.estimatedTime.toLong()).roundUpToQuarter()
    out += TripElement.TripActivity(act.copy(startDate = start.toTs(), endDate = end.toTs()))
    activitiesToday++
    cursor = end
  }

  /** Schedules a travel segment (RouteSegment). */
  fun scheduleTravel(fromIdx: Int) {
    // pause before travel, then round
    cursor = cursor.plusSeconds(eff.pauseBetweenEachActivity.toLong()).roundUpToQuarter()

    val driveSec = legs[fromIdx].toInt()
    if (!fitsTravel(driveSec)) nextDay()

    val start = cursor
    val end = start.plusSeconds(driveSec.toLong()).roundUpToQuarter()
    out +=
        TripElement.TripSegment(
            RouteSegment(
                from = locs[fromIdx],
                to = locs[fromIdx + 1],
                durationMinutes = ceil(driveSec / 60.0).toInt(),
                transportMode = TransportMode.CAR,
                startDate = start.toTs(),
                endDate = end.toTs()))
    cursor = end
  }

  // Main scheduling loop
  for (i in locs.indices) {
    // Activities at this location in provided order
    activities.asSequence().filter { it.location == locs[i] }.forEach { scheduleActivity(it) }

    // Travel to next location
    if (i < locs.lastIndex) scheduleTravel(i)
  }

  return out.sortedBy { it.startDate.seconds }
}
