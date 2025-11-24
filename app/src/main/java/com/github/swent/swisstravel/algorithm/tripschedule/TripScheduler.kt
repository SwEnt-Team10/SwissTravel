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
 * @property dayStart the start of a day (first moment of the day when activities can start)
 * @property dayEnd the end of a day (last moment of the day when activities can end)
 * @property pauseBetweenEachActivity the pause between each activity
 * @property maxActivitiesPerDay the maximum activities per day
 * @property travelEnd the travel end (last moment of the day when you can travel)
 */
data class ScheduleParams(
    val dayStart: LocalTime = LocalTime.of(8, 0),
    val dayEnd: LocalTime = LocalTime.of(18, 0),
    val travelEnd: LocalTime = LocalTime.of(22, 0),
    val maxActivitiesPerDay: Int = 3,
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
  val prefs = profile.preferences.toSet()
  val hasNightOwl = Preference.NIGHTLIFE in prefs || Preference.NIGHT_OWL in prefs
  val hasEarlyBird = Preference.EARLY_BIRD in prefs

  // Apply time-related preferences
  var eff =
      when {
        hasNightOwl && hasEarlyBird ->
            base.copy(
                dayStart = LocalTime.of(6, 0),
                travelEnd = LocalTime.of(22, 0),
                maxActivitiesPerDay = 6)
        hasNightOwl ->
            base.copy(
                dayStart = LocalTime.of(10, 0),
                dayEnd = LocalTime.of(22, 0),
                travelEnd = LocalTime.of(23, 59))
        hasEarlyBird ->
            base.copy(
                dayStart = LocalTime.of(6, 0),
                travelEnd = LocalTime.of(20, 0),
                maxActivitiesPerDay = 6)
        else -> base
      }

  // Apply pace-related preferences
  eff =
      when {
        Preference.SLOW_PACE in prefs -> eff.copy(pauseBetweenEachActivity = 60 * 60)
        Preference.QUICK in prefs -> eff.copy(pauseBetweenEachActivity = 0)
        else -> eff
      }

  return eff
}

/**
 * Encapsulates the state and logic for the trip scheduling algorithm. This avoids complex state
 * management with captured variables in nested functions.
 */
private class TripSchedulerState(
    tripProfile: TripProfile,
    private val ordered: OrderedRoute,
    private val activities: List<Activity>,
    private val params: ScheduleParams
) {
  private val zone = ZoneId.systemDefault()
  private val tripEndDay = tripProfile.endDate.toInstant().atZone(zone).toLocalDate()

  private var currentDay: LocalDate = tripProfile.startDate.toInstant().atZone(zone).toLocalDate()
  private var cursor: LocalDateTime =
      LocalDateTime.of(currentDay, params.dayStart).roundUpToQuarter()
  private var activitiesToday = 0
  private val output = mutableListOf<TripElement>()

  /**
   * The main entry point to run the scheduling algorithm.
   *
   * @param onProgress Callback to report scheduling progress (0.0 to 1.0).
   */
  fun buildSchedule(onProgress: (Float) -> Unit): List<TripElement> {
    var completedStep = 0
    val totalSteps = ordered.orderedLocations.size
    // Main scheduling loop
    for (i in ordered.orderedLocations.indices) {
      // Schedule all activities for the current location
      activities
          .asSequence()
          .filter { it.location == ordered.orderedLocations[i] }
          .forEach { scheduleActivity(it) }

      // Schedule travel to the next location if it's not the last one
      if (i < ordered.orderedLocations.lastIndex) {
        scheduleTravel(i)
      }
      completedStep++
      onProgress(completedStep.toFloat() / totalSteps)
    }
    onProgress(1f)
    return output.sortedBy { it.startDate.seconds }
  }

  private fun scheduleActivity(act: Activity) {
    if (activitiesToday >= params.maxActivitiesPerDay) {
      if (isLastDay()) return
      nextDay()
    }

    cursor = cursor.roundUpToQuarter()

    if (!fitsInTimeWindow(act.estimatedTime, params.dayEnd)) {
      if (isLastDay()) return
      nextDay()
    }

    val start = cursor
    val end = start.plusSeconds(act.estimatedTime.toLong()).roundUpToQuarter()

    output += TripElement.TripActivity(act.copy(startDate = start.toTs(), endDate = end.toTs()))
    activitiesToday++
    cursor = end
  }

  private fun scheduleTravel(fromIdx: Int) {
    // Apply pause before travel and then round up
    cursor = cursor.plusSeconds(params.pauseBetweenEachActivity.toLong()).roundUpToQuarter()

    val travelDurationSec = ordered.segmentDuration[fromIdx].toInt()
    if (!fitsInTimeWindow(travelDurationSec, params.travelEnd)) {
      if (isLastDay()) return
      nextDay()
      // Re-check after advancing the day
      if (!fitsInTimeWindow(travelDurationSec, params.travelEnd)) {
        return // Still doesn't fit, can't schedule.
      }
    }

    val start = cursor
    val end = start.plusSeconds(travelDurationSec.toLong()).roundUpToQuarter()
    val locs = ordered.orderedLocations
    output +=
        TripElement.TripSegment(
            RouteSegment(
                from = locs[fromIdx],
                to = locs[fromIdx + 1],
                durationMinutes = ceil(travelDurationSec / 60.0).toInt(),
                transportMode = TransportMode.CAR,
                startDate = start.toTs(),
                endDate = end.toTs()))
    cursor = end
  }

  /** Advances the scheduler to the start of the next day. */
  private fun nextDay() {
    currentDay = currentDay.plusDays(1)
    cursor = LocalDateTime.of(currentDay, params.dayStart).roundUpToQuarter()
    activitiesToday = 0
  }

  /**
   * Checks if an event of a given duration fits before the specified end time for the current day.
   */
  private fun fitsInTimeWindow(durationSeconds: Int, dayEndTime: LocalTime): Boolean {
    val endOfDay = LocalDateTime.of(currentDay, dayEndTime)
    return !cursor.plusSeconds(durationSeconds.toLong()).isAfter(endOfDay)
  }

  /** Checks if the scheduler is currently on the final day of the trip. */
  private fun isLastDay(): Boolean = currentDay == tripEndDay
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
 * @param activities Activities to place (all the activities of the trip); `estimatedTime` is in
 *   seconds.
 * @param params Base constraints (may be tweaked by preferences).
 * @param onProgress Callback to report scheduling progress (0.0 to 1.0).
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
    params: ScheduleParams = ScheduleParams(),
    onProgress: (Float) -> Unit
): List<TripElement> {
  if (ordered.orderedLocations.isEmpty()) return emptyList()

  val effectiveParams = applyPreferenceOverrides(tripProfile, params)

  return TripSchedulerState(tripProfile, ordered, activities, effectiveParams)
      .buildSchedule(onProgress)
}
