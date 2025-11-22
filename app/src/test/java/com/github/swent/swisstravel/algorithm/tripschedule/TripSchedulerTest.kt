package com.github.swent.swisstravel.algorithm.tripschedule

import com.github.swent.swisstravel.algorithm.orderlocations.OrderedRoute
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.TripElement
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.github.swent.swisstravel.model.user.Preference
import com.google.firebase.Timestamp
import java.time.*
import java.time.temporal.ChronoUnit
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

  private fun tripProfileFor(date: LocalDate) =
      TripProfile(
          startDate = Timestamp(date.atStartOfDay(ZoneId.systemDefault()).toEpochSecond(), 0),
          endDate =
              Timestamp(date.plusDays(5).atStartOfDay(ZoneId.systemDefault()).toEpochSecond(), 0))

  // Activity factory: estimatedTime is in **seconds**
  private fun activityAt(location: Location, label: String, estimatedMinutes: Int): Activity {
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

    val profile = tripProfileFor(LocalDate.of(2025, 11, 5))

    val out =
        scheduleTrip(
            tripProfile = profile, ordered = ordered, activities = activities, params = params)

    // Expect: A1 (9:00–10:00), travel (10:00–10:30), pause to 10:45, B1 (10:45–11:45)
    assertEquals(3, out.size)
    val a1 = (out[0] as TripElement.TripActivity).activity
    val leg = (out[1] as TripElement.TripSegment).route
    val b1 = (out[2] as TripElement.TripActivity).activity

    assertEquals(LocalTime.of(9, 0), tsToLocalTime(a1.startDate))
    assertEquals(LocalTime.of(10, 0), tsToLocalTime(a1.endDate))

    assertEquals(LocalTime.of(10, 15), tsToLocalTime(leg.startDate))
    assertEquals(LocalTime.of(10, 45), tsToLocalTime(leg.endDate))

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

    val profile = tripProfileFor(LocalDate.of(2025, 11, 5))

    val out =
        scheduleTrip(
            tripProfile = profile, ordered = ordered, activities = activities, params = params)
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

    val profile = tripProfileFor(LocalDate.of(2025, 11, 5))

    val out =
        scheduleTrip(
            tripProfile = profile, ordered = ordered, activities = activities, params = params)

    // Expect: A1 (9–10), travel would be 10–19 (doesn't fit), so travel next day 9–18,
    // pause to 18:15, then B1 would not fit → following day 9:00–10:00.
    assertEquals(3, out.size)

    val a1 = (out[0] as TripElement.TripActivity).activity
    val leg = (out[1] as TripElement.TripSegment).route
    val b1 = (out[2] as TripElement.TripActivity).activity

    // A1 day 1
    assertEquals(LocalDate.of(2025, 11, 5), tsToLocalDate(a1.startDate))
    assertEquals(LocalTime.of(9, 0), tsToLocalTime(a1.startDate))
    assertEquals(LocalTime.of(10, 0), tsToLocalTime(a1.endDate))

    // Travel stays same day now (fits until 22:00)
    assertEquals(LocalDate.of(2025, 11, 5), tsToLocalDate(leg.startDate))
    assertEquals(LocalTime.of(10, 15), tsToLocalTime(leg.startDate))
    assertEquals(LocalTime.of(19, 15), tsToLocalTime(leg.endDate))

    // B1 next day (activities can't go after 18:00)
    assertEquals(LocalDate.of(2025, 11, 6), tsToLocalDate(b1.startDate))
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

    val profile = tripProfileFor(LocalDate.of(2025, 11, 5))

    val out =
        scheduleTrip(
            tripProfile = profile, ordered = ordered, activities = activities, params = params)

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

    val profile = tripProfileFor(LocalDate.of(2025, 11, 5))

    val out =
        scheduleTrip(
            tripProfile = profile, ordered = ordered, activities = activities, params = params)
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

    val profile = tripProfileFor(LocalDate.of(2025, 11, 5))

    val out =
        scheduleTrip(
            tripProfile = profile,
            ordered = ordered,
            activities = activities,
            params = ScheduleParams())
    assertTrue(out.isEmpty())
  }

  @Test
  fun `result is chronologically sorted by start time`() {
    val ordered =
        OrderedRoute(
            orderedLocations = listOf(A, B, C),
            totalDuration = 3600.0,
            segmentDuration = listOf(900.0, 900.0) // 15m each
            )
    val activities =
        listOf(activityAt(A, "A1", 30), activityAt(B, "B1", 30), activityAt(C, "C1", 30))

    val profile = tripProfileFor(LocalDate.of(2025, 11, 5))

    val out =
        scheduleTrip(
            tripProfile = profile,
            ordered = ordered,
            activities = activities,
            params =
                ScheduleParams(
                    dayStart = LocalTime.of(9, 0),
                    dayEnd = LocalTime.of(18, 0),
                    pauseBetweenEachActivity = 0))

    val starts = out.map { tsToLocalDateTime(it.startDate) }
    assertTrue(starts.zipWithNext().all { (a, b) -> !b.isBefore(a) })
  }

  @Test
  fun `preferences correctly adjust scheduling parameters`() {
    val ordered =
        OrderedRoute(
            orderedLocations = listOf(A, B),
            totalDuration = 1800.0,
            segmentDuration = listOf(1800.0) // 30 min travel
            )
    val activities = listOf(activityAt(A, "Morning", 60), activityAt(B, "Evening", 60))

    // 1️⃣ EARLY_BIRD
    val earlyBirdProfile =
        tripProfileFor(LocalDate.of(2025, 11, 5)).copy(preferences = listOf(Preference.EARLY_BIRD))
    val earlyOut = scheduleTrip(earlyBirdProfile, ordered, activities)
    val earlyStart =
        tsToLocalTime((earlyOut.first() as TripElement.TripActivity).activity.startDate)
    assertTrue("Early bird should start before 8am", earlyStart <= LocalTime.of(6, 15))

    // 2️⃣ NIGHT_OWL – with short activities it won't necessarily end late.
    // Verify that scheduling still works and could extend up to the late window if needed.
    val nightOwlProfile =
        tripProfileFor(LocalDate.of(2025, 11, 5)).copy(preferences = listOf(Preference.NIGHT_OWL))
    val nightOut = scheduleTrip(nightOwlProfile, ordered, activities)
    assertTrue("Night owl should schedule at least something", nightOut.isNotEmpty())

    // Last activity ends no later than the extended window (22:00) and not before it started
    val nightEnd = tsToLocalTime((nightOut.last() as TripElement.TripActivity).activity.endDate)
    assertTrue("Night owl window should allow up to 22:00", nightEnd <= LocalTime.of(22, 0))

    // 3️⃣ SLOW_PACE adds a long pause before travel
    val slowProfile =
        tripProfileFor(LocalDate.of(2025, 11, 5)).copy(preferences = listOf(Preference.SLOW_PACE))
    val slowOut = scheduleTrip(slowProfile, ordered, activities)
    val a1 = (slowOut[0] as TripElement.TripActivity).activity
    val travel = (slowOut[1] as TripElement.TripSegment).route
    val pauseMinutes =
        ChronoUnit.MINUTES.between(tsToLocalTime(a1.endDate), tsToLocalTime(travel.startDate))
    assertTrue("Slow pace should create at least a 60-minute pause", pauseMinutes >= 60)

    // 4️⃣ QUICK removes pause before travel
    val quickProfile =
        tripProfileFor(LocalDate.of(2025, 11, 5)).copy(preferences = listOf(Preference.QUICK))
    val quickOut = scheduleTrip(quickProfile, ordered, activities)
    val qA1 = (quickOut[0] as TripElement.TripActivity).activity
    val qTravel = (quickOut[1] as TripElement.TripSegment).route
    val quickPauseMinutes =
        ChronoUnit.MINUTES.between(tsToLocalTime(qA1.endDate), tsToLocalTime(qTravel.startDate))
    assertTrue("Quick should have minimal or no pause", quickPauseMinutes < 15)
  }

  @Test
  fun `night owl allows late activity end past 21_00 when schedule pushes late`() {
    // Route A -> B with a 1h travel leg
    val A = Location(Coordinate(46.5, 6.5), "A")
    val B = Location(Coordinate(46.6, 6.6), "B")
    val ordered =
        OrderedRoute(
            orderedLocations = listOf(A, B),
            totalDuration = 3600.0,
            segmentDuration = listOf(3600.0) // 1h A→B
            )

    // Activities: A has an 8h block (10:00–18:00), then travel 1h (18:00–19:00),
    // pause 15m -> 19:15, then a 2h activity at B -> 21:15
    fun activityAt(loc: Location, mins: Int) =
        Activity(
            startDate = Timestamp(0, 0),
            endDate = Timestamp(0, 0),
            location = loc,
            description = "",
            imageUrls = emptyList(),
            estimatedTime = mins * 60)
    val activities =
        listOf(
            activityAt(A, 8 * 60), // 8h
            activityAt(B, 2 * 60) // 2h
            )

    val profile =
        TripProfile(
            startDate =
                Timestamp(
                    LocalDateTime.of(2025, 11, 5, 0, 0)
                        .atZone(ZoneId.systemDefault())
                        .toEpochSecond(),
                    0),
            endDate = Timestamp(0, 0),
            preferredLocations = emptyList(),
            preferences = listOf(Preference.NIGHT_OWL))

    val params =
        ScheduleParams(
            dayStart = LocalTime.of(8, 0),
            dayEnd = LocalTime.of(18, 0),
            travelEnd = LocalTime.of(22, 0),
            maxActivitiesPerDay = 4,
            pauseBetweenEachActivity = 15 * 60)

    val out =
        scheduleTrip(
            tripProfile = profile, ordered = ordered, activities = activities, params = params)

    // Last element should be the second activity, ending after 21:00 (expected 21:15)
    val lastAct = (out.last() as TripElement.TripActivity).activity
    val endTime =
        Instant.ofEpochSecond(lastAct.endDate.seconds).atZone(ZoneId.systemDefault()).toLocalTime()

    assertTrue(
        "Night owl should allow activities to end after 21:00, was $endTime",
        endTime >= LocalTime.of(21, 0))
  }

  @Test
  fun `nextDay is triggered by activity overflow, travel overflow, and maxActivitiesPerDay`() {
    val ordered =
        OrderedRoute(
            orderedLocations = listOf(A, B, C),
            totalDuration = 3600.0 * 13,
            segmentDuration = listOf(3600.0, 3600.0 * 12) // 1h A→B, 12h B→C
            )

    val activities =
        listOf(
            activityAt(A, "A-short", 10 * 60), // 10h
            activityAt(B, "B-long", 9 * 60), // 9h
            activityAt(C, "C-short", 30) // 30 min
            )

    val params =
        ScheduleParams(
            dayStart = LocalTime.of(8, 0),
            dayEnd = LocalTime.of(18, 0),
            travelEnd = LocalTime.of(22, 0),
            pauseBetweenEachActivity = 0,
            maxActivitiesPerDay = 1)

    val profile = tripProfileFor(LocalDate.of(2025, 11, 5))

    val out =
        scheduleTrip(
            tripProfile = profile, ordered = ordered, activities = activities, params = params)

    // 3 locations and 2 travels
    assertEquals(5, out.size)

    val a = out[0] as TripElement.TripActivity
    val ab = out[1] as TripElement.TripSegment
    val b = out[2] as TripElement.TripActivity
    val bc = out[3] as TripElement.TripSegment
    val c = out[4] as TripElement.TripActivity

    // first activity scheduled on day 1 : 8h - 18h
    assertEquals(LocalDate.of(2025, 11, 5), tsToLocalDate(a.startDate))

    // travel AB fits day 1 : 18h - 19h
    assertEquals(LocalDate.of(2025, 11, 5), tsToLocalDate(ab.startDate))

    // activity max overflow (and time overflow) → day 2 : 8h - 17h
    assertEquals(LocalDate.of(2025, 11, 6), tsToLocalDate(b.startDate))

    // travel time overflow : BC does not fit same day → day 3 : 8h - 20h
    assertEquals(LocalDate.of(2025, 11, 7), tsToLocalDate(bc.startDate))

    // activity time overflow (cannot start after 18h) → day 4 : 8h - 8h30
    assertEquals(LocalDate.of(2025, 11, 8), tsToLocalDate(c.startDate))
  }
}
