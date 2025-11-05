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
 * The trip scheduling algorithm. It calculates an optimal trip taking into account the start of the
 * trip and the optimal routes between activities.
 *
 * @param tripStart the trip start
 * @param ordered the ordered route
 * @param activities the activities
 * @param params the parameters of the algorithm
 * @return A list of trip elements (i.e RouteSegments or TripActivities)
 */
fun scheduleTrip(
    tripStart: LocalDate,
    ordered: OrderedRoute,
    activities: List<Activity>,
    params: ScheduleParams = ScheduleParams()
): List<TripElement> {

  // TODO: Take into account numerous preferences and other transport modes
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
