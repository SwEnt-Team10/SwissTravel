package com.github.swent.swisstravel.algorithm.tripschedule

import com.github.swent.swisstravel.algorithm.orderlocations.OrderedRoute
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.TripElement
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.google.firebase.Timestamp
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class TripSchedulerTest {

  // ---- Helpers ------------------------------------------------------------

  private val zone: ZoneId = ZoneId.systemDefault()

  private fun tsToLocalDateTime(ts: Timestamp): LocalDateTime =
      Instant.ofEpochSecond(ts.seconds).atZone(zone).toLocalDateTime()

  private fun tsToLocalDate(ts: Timestamp): LocalDate =
      Instant.ofEpochSecond(ts.seconds).atZone(zone).toLocalDate()

  private fun tsToLocalTime(ts: Timestamp): LocalTime = tsToLocalDateTime(ts).toLocalTime()

  private fun loc(name: String, lat: Double, lon: Double) = Location(Coordinate(lat, lon), name)

  // Activity factory: estimatedTime is in **seconds** (your model)
  private fun activityAt(location: Location, label: String, estimatedMinutes: Int): Activity {
    // start/end are placeholders; scheduler overwrites them
    val now = Instant.parse("2025-11-05T00:00:00Z").epochSecond
    return Activity(
        startDate = Timestamp(now, 0),
        endDate = Timestamp(now, 0),
        location = location,
        description = "Activity at $label",
        imageUrls = emptyList(),
        estimatedTime = estimatedMinutes * 60)
  }

  // Common locations
  private val A = loc("A", 46.5, 6.5)
  private val B = loc("B", 46.6, 6.6)
  private val C = loc("C", 46.7, 6.7)

  // ---- Tests --------------------------------------------------------------

  @Test
  fun `simple day - activities and travel fit with pause applied only after travel`() {
    val ordered =
        OrderedRoute(
            orderedLocations = listOf(A, B),
            totalDuration = 1800.0,
            segmentDuration = listOf(1800.0) // 30 min A→B
            )
    val activities =
        listOf(
            activityAt(A, "A1", 60), // 1h
            activityAt(B, "B1", 60) // 1h
            )
    val params =
        ScheduleParams(
            dayStart = LocalTime.of(9, 0),
            dayEnd = LocalTime.of(18, 0),
            pauseBetweenEachActivity = 15 * 60 // 15 min
            )

    val out =
        scheduleTrip(
            tripStart = LocalDate.of(2025, 11, 5),
            ordered = ordered,
            activities = activities,
            params = params)

    // Expect: A1 (9:00–10:00), travel (10:00–10:30), pause to 10:45, B1 (10:45–11:45)
    assertEquals(3, out.size)
    val a1 = (out[0] as TripElement.TripActivity).activity
    val leg = (out[1] as TripElement.TripSegment).route
    val b1 = (out[2] as TripElement.TripActivity).activity

    assertEquals(LocalTime.of(9, 0), tsToLocalTime(a1.startDate))
    assertEquals(LocalTime.of(10, 0), tsToLocalTime(a1.endDate))

    assertEquals(LocalTime.of(10, 0), tsToLocalTime(leg.startDate))
    assertEquals(LocalTime.of(10, 30), tsToLocalTime(leg.endDate))

    // Pause only after travel
    assertEquals(LocalTime.of(10, 45), tsToLocalTime(b1.startDate))
    assertEquals(LocalTime.of(11, 45), tsToLocalTime(b1.endDate))
  }

  @Test
  fun `activity that doesn't fit rolls to next day at dayStart`() {
    val ordered =
        OrderedRoute(
            orderedLocations = listOf(A), // only A
            totalDuration = 0.0,
            segmentDuration = emptyList())
    val activities =
        listOf(
            activityAt(A, "A-long", 10 * 60) // 10h; 09:00→19:00 doesn't fit 18:00
            )
    val params =
        ScheduleParams(
            dayStart = LocalTime.of(9, 0),
            dayEnd = LocalTime.of(18, 0),
            pauseBetweenEachActivity = 0)

    val out =
        scheduleTrip(
            tripStart = LocalDate.of(2025, 11, 5),
            ordered = ordered,
            activities = activities,
            params = params)
    assertEquals(1, out.size)
    val a = (out[0] as TripElement.TripActivity).activity

    // Should start next day 9:00–19:00
    assertEquals(LocalDate.of(2025, 11, 6), tsToLocalDate(a.startDate))
    assertEquals(LocalTime.of(9, 0), tsToLocalTime(a.startDate))
    assertEquals(LocalTime.of(19, 0), tsToLocalTime(a.endDate))
  }

  @Test
  fun `travel that doesn't fit rolls to next day and applies pause after it`() {
    val ordered =
        OrderedRoute(
            orderedLocations = listOf(A, B),
            totalDuration = 9 * 3600.0,
            segmentDuration = listOf(9 * 3600.0) // 9h travel
            )
    val activities =
        listOf(
            activityAt(A, "A1", 60), // 1h (09:00–10:00)
            activityAt(B, "B1", 60) // should be next day after long travel + pause
            )
    val params =
        ScheduleParams(
            dayStart = LocalTime.of(9, 0),
            dayEnd = LocalTime.of(18, 0),
            pauseBetweenEachActivity = 15 * 60 // 15 min
            )

    val out =
        scheduleTrip(
            tripStart = LocalDate.of(2025, 11, 5),
            ordered = ordered,
            activities = activities,
            params = params)

    // Expect: A1 (9–10), travel would be 10–19 (doesn't fit), so travel next day 9–18,
    // pause to 18:15, then B1 would not fit same day → next day 9:00–10:00.
    // BUT note: implementation advances to next day WHEN scheduling the travel; here travel 9h fits
    // 9–18.
    // After travel, it adds pause 15 min => 18:15, B1 doesn't fit → pushed to following day
    // 9:00–10:00.
    assertEquals(3, out.size)

    val a1 = (out[0] as TripElement.TripActivity).activity
    val leg = (out[1] as TripElement.TripSegment).route
    val b1 = (out[2] as TripElement.TripActivity).activity

    // A1 day 1
    assertEquals(LocalDate.of(2025, 11, 5), tsToLocalDate(a1.startDate))
    assertEquals(LocalTime.of(9, 0), tsToLocalTime(a1.startDate))
    assertEquals(LocalTime.of(10, 0), tsToLocalTime(a1.endDate))

    // Travel day 2 (since 9h won't fit 10–19 on day 1)
    assertEquals(LocalDate.of(2025, 11, 6), tsToLocalDate(leg.startDate))
    assertEquals(LocalTime.of(9, 0), tsToLocalTime(leg.startDate))
    assertEquals(LocalTime.of(18, 0), tsToLocalTime(leg.endDate))

    // B1 day 3 due to pause pushing past 18:00 on day 2
    assertEquals(LocalDate.of(2025, 11, 7), tsToLocalDate(b1.startDate))
    assertEquals(LocalTime.of(9, 0), tsToLocalTime(b1.startDate))
    assertEquals(LocalTime.of(10, 0), tsToLocalTime(b1.endDate))
  }

  @Test
  fun `consecutive activities at same location are back-to-back - no travel, no pause`() {
    val ordered =
        OrderedRoute(
            orderedLocations = listOf(A), // only A
            totalDuration = 0.0,
            segmentDuration = emptyList())
    val activities = listOf(activityAt(A, "A1", 30), activityAt(A, "A2", 45))
    val params =
        ScheduleParams(
            dayStart = LocalTime.of(9, 0),
            dayEnd = LocalTime.of(18, 0),
            pauseBetweenEachActivity = 15 * 60)

    val out =
        scheduleTrip(
            tripStart = LocalDate.of(2025, 11, 5),
            ordered = ordered,
            activities = activities,
            params = params)

    // Expect: A1 09:00–09:30, A2 09:30–10:15
    assertEquals(2, out.size)
    val a1 = (out[0] as TripElement.TripActivity).activity
    val a2 = (out[1] as TripElement.TripActivity).activity

    assertEquals(LocalTime.of(9, 0), tsToLocalTime(a1.startDate))
    assertEquals(LocalTime.of(9, 30), tsToLocalTime(a1.endDate))
    assertEquals(LocalTime.of(9, 30), tsToLocalTime(a2.startDate))
    assertEquals(LocalTime.of(10, 15), tsToLocalTime(a2.endDate))
  }

  @Test
  fun `activity ending exactly at dayEnd still fits the same day`() {
    val ordered =
        OrderedRoute(
            orderedLocations = listOf(A), totalDuration = 0.0, segmentDuration = emptyList())
    // From 09:00 to 18:00 = 9 hours
    val activities = listOf(activityAt(A, "A-exact", 9 * 60))
    val params =
        ScheduleParams(
            dayStart = LocalTime.of(9, 0),
            dayEnd = LocalTime.of(18, 0),
            pauseBetweenEachActivity = 0)

    val out =
        scheduleTrip(
            tripStart = LocalDate.of(2025, 11, 5),
            ordered = ordered,
            activities = activities,
            params = params)
    assertEquals(1, out.size)
    val a = (out[0] as TripElement.TripActivity).activity

    assertEquals(LocalDate.of(2025, 11, 5), tsToLocalDate(a.startDate))
    assertEquals(LocalTime.of(9, 0), tsToLocalTime(a.startDate))
    assertEquals(LocalTime.of(18, 0), tsToLocalTime(a.endDate)) // equal to dayEnd OK
  }

  @Test
  fun `empty ordered locations returns empty result`() {
    val ordered =
        OrderedRoute(
            orderedLocations = emptyList(), totalDuration = 0.0, segmentDuration = emptyList())
    val activities = listOf(activityAt(A, "X", 30))
    val out =
        scheduleTrip(
            tripStart = LocalDate.of(2025, 11, 5),
            ordered = ordered,
            activities = activities,
            params = ScheduleParams())
    assertTrue(out.isEmpty())
  }

  @Test
  fun `result is chronologically sorted by start time`() {
    // Build a route A -> B -> C with short legs
    val ordered =
        OrderedRoute(
            orderedLocations = listOf(A, B, C),
            totalDuration = 3600.0,
            segmentDuration = listOf(900.0, 900.0) // 15m each
            )
    val activities =
        listOf(activityAt(A, "A1", 30), activityAt(B, "B1", 30), activityAt(C, "C1", 30))
    val out =
        scheduleTrip(
            tripStart = LocalDate.of(2025, 11, 5),
            ordered = ordered,
            activities = activities,
            params =
                ScheduleParams(
                    dayStart = LocalTime.of(9, 0),
                    dayEnd = LocalTime.of(18, 0),
                    pauseBetweenEachActivity = 0))

    // Ensure non-decreasing start times
    val starts = out.map { tsToLocalDateTime(it.startDate) }
    assertTrue(starts.zipWithNext().all { (a, b) -> !b.isBefore(a) })
  }
}
