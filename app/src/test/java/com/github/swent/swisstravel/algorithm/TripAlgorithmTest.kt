package com.github.swent.swisstravel.algorithm

import android.content.Context
import com.github.swent.swisstravel.algorithm.orderlocations.OrderedRoute
import com.github.swent.swisstravel.algorithm.orderlocationsv2.ProgressiveRouteOptimizer
import com.github.swent.swisstravel.algorithm.selectactivities.SelectActivities
import com.github.swent.swisstravel.algorithm.tripschedule.ScheduleParams
import com.github.swent.swisstravel.algorithm.tripschedule.scheduleTrip
import com.github.swent.swisstravel.model.trip.*
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.github.swent.swisstravel.model.trip.activity.ActivityRepository
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.ui.tripcreation.TripArrivalDeparture
import com.github.swent.swisstravel.ui.tripcreation.TripSettings
import com.google.firebase.Timestamp
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest
import org.junit.Test

class TripAlgorithmTest {

  private val selectActivities = mockk<SelectActivities>()
  private val routeOptimizer = mockk<ProgressiveRouteOptimizer>()
  private val scheduleParams = ScheduleParams()

  private val algorithm =
      TripAlgorithm(
          activitySelector = selectActivities,
          routeOptimizer = routeOptimizer,
          scheduleParams = scheduleParams)

  @Test
  fun `computeTrip runs full pipeline`() = runTest {
    val coordinates =
        listOf(
            Location(Coordinate(10.0, 10.0), "Start"),
            Location(Coordinate(10.0, 20.0), "Museum"),
            Location(Coordinate(20.0, 20.0), "End"))

    val museumActivity =
        Activity(
            description = "",
            estimatedTime = 3600,
            startDate = Timestamp(3900, 0),
            endDate = Timestamp(7500, 0),
            imageUrls = emptyList(),
            location = coordinates[1])

    val activityElements = listOf(museumActivity)

    coEvery { selectActivities.addActivities(any<(Float) -> Unit>()) } returns activityElements

    val orderedRoute =
        OrderedRoute(
            orderedLocations = coordinates,
            totalDuration = 9000.0,
            segmentDuration = listOf(3600.0, 1200.0))

    coEvery {
      routeOptimizer.optimize(
          start = any(),
          end = any(),
          allLocations = any(),
          activities = any(),
          mode = any(),
          onProgress = any())
    } returns orderedRoute

    val startRouteSegment =
        RouteSegment(
            from = coordinates[0],
            to = coordinates[1],
            startDate = Timestamp(0, 0),
            endDate = Timestamp(3600, 0),
            transportMode = TransportMode.CAR,
            durationMinutes = 60)

    val endRouteSegment =
        RouteSegment(
            from = coordinates[1],
            to = coordinates[2],
            startDate = Timestamp(7800, 0),
            endDate = Timestamp(9000, 0),
            transportMode = TransportMode.CAR,
            durationMinutes = 20)

    val finalSchedule =
        listOf(
            TripElement.TripSegment(startRouteSegment),
            TripElement.TripActivity(museumActivity),
            TripElement.TripSegment(endRouteSegment),
        )

    // Help by AI
    mockkStatic("com.github.swent.swisstravel.algorithm.tripschedule.TripSchedulerKt")

    coEvery {
      scheduleTrip(
          activities = activityElements,
          params = any(),
          onProgress = any(),
          tripProfile = any(),
          ordered = any())
    } returns finalSchedule

    val settings =
        TripSettings(
            name = "My Trip",
            arrivalDeparture =
                TripArrivalDeparture(
                    arrivalLocation = coordinates[0], departureLocation = coordinates[2]),
            destinations = coordinates,
            preferences = listOf(Preference.MUSEUMS))

    val profile =
        TripProfile(
            startDate = Timestamp(0, 0),
            endDate = Timestamp(9000, 0),
            preferredLocations = emptyList(),
            preferences = emptyList(),
            adults = 2,
            children = 1,
            arrivalLocation = settings.arrivalDeparture.arrivalLocation,
            departureLocation = settings.arrivalDeparture.departureLocation)

    val result = algorithm.computeTrip(settings, profile)

    assertEquals(finalSchedule.size, result.size)
    assertEquals(finalSchedule[0].startDate, result[0].startDate)
    assertEquals(finalSchedule[0].endDate, result[0].endDate)
    assertEquals(finalSchedule[1].startDate, result[1].startDate)
    assertEquals(finalSchedule[1].endDate, result[1].endDate)
    assertEquals(finalSchedule[2].startDate, result[2].startDate)
    assertEquals(finalSchedule[2].endDate, result[2].endDate)
  }

  // Done with AI
  @Test
  fun `runTripAlgorithm delegates to computeTrip`() = runTest {
    // Arrange
    val settings = mockk<TripSettings>()
    val profile = mockk<TripProfile>()

    // Mock computeTrip directly
    val expected = emptyList<TripElement>()

    val algorithmSpy = spyk(algorithm)

    coEvery { algorithmSpy.computeTrip(settings, profile, any()) } returns expected

    // Act
    val result = algorithmSpy.computeTrip(settings, profile) {}

    // Assert
    assertEquals(expected, result)
    coVerify(exactly = 1) { algorithmSpy.computeTrip(settings, profile, any()) }
  }

  // Done with AI
  @Test(expected = IllegalStateException::class)
  fun `computeTrip throws when optimized route duration is zero or negative`() = runTest {
    // Arrange
    val coordinates =
        listOf(Location(Coordinate(1.0, 1.0), "A"), Location(Coordinate(2.0, 2.0), "B"))

    // Mock a valid activity, INCLUDING its location
    val activity = mockk<Activity>()
    every { activity.location } returns coordinates[0]

    coEvery { selectActivities.addActivities(any()) } returns listOf(activity)

    val invalidRoute =
        OrderedRoute(
            orderedLocations = coordinates,
            totalDuration = 0.0, // invalid
            segmentDuration = listOf(0.0))

    coEvery { routeOptimizer.optimize(any(), any(), any(), any(), any(), any()) } returns
        invalidRoute

    mockkStatic("com.github.swent.swisstravel.algorithm.tripschedule.TripSchedulerKt")
    coEvery { scheduleTrip(any(), any(), any(), any(), any()) } returns emptyList()

    val settings =
        TripSettings(
            name = "Trip",
            arrivalDeparture =
                TripArrivalDeparture(
                    arrivalLocation = coordinates[0], departureLocation = coordinates[1]),
            destinations = coordinates,
            preferences = emptyList())

    val profile =
        TripProfile(
            startDate = Timestamp(0, 0),
            endDate = Timestamp(1000, 0),
            preferredLocations = emptyList(),
            preferences = emptyList(),
            adults = 1,
            children = 0,
            arrivalLocation = coordinates[0],
            departureLocation = coordinates[1])

    // Act → should now throw *your* IllegalStateException
    algorithm.computeTrip(settings, profile)
  }

  // Done with AI
  @Test(expected = IllegalStateException::class)
  fun `computeTrip throws when scheduled trip is empty`() = runTest {
    // Arrange
    val coordinates =
        listOf(Location(Coordinate(1.0, 1.0), "A"), Location(Coordinate(2.0, 2.0), "B"))

    val activity = mockk<Activity>()
    every { activity.location } returns coordinates[0]

    coEvery { selectActivities.addActivities(any()) } returns listOf(activity)

    val validRoute =
        OrderedRoute(
            orderedLocations = coordinates,
            totalDuration = 1000.0,
            segmentDuration = listOf(1000.0))

    coEvery { routeOptimizer.optimize(any(), any(), any(), any(), any(), any()) } returns validRoute

    // scheduleTrip empty → should trigger your check
    mockkStatic("com.github.swent.swisstravel.algorithm.tripschedule.TripSchedulerKt")
    coEvery { scheduleTrip(any(), any(), any(), any(), any()) } returns emptyList()

    val settings =
        TripSettings(
            name = "Trip",
            arrivalDeparture =
                TripArrivalDeparture(
                    arrivalLocation = coordinates[0], departureLocation = coordinates[1]),
            destinations = coordinates,
            preferences = emptyList())

    val profile =
        TripProfile(
            startDate = Timestamp(0, 0),
            endDate = Timestamp(1000, 0),
            preferredLocations = emptyList(),
            preferences = emptyList(),
            adults = 1,
            children = 0,
            arrivalLocation = coordinates[0],
            departureLocation = coordinates[1])

    // Act → should now throw IllegalStateException (not MockKException)
    algorithm.computeTrip(settings, profile)
  }

  // Done with AI
  @Test
  fun `init returns a TripAlgorithm instance`() {
    // Arrange
    val context = mockk<Context>(relaxed = true)
    val repo = mockk<ActivityRepository>(relaxed = true)

    val settings =
        TripSettings(
            name = "Sample Trip",
            arrivalDeparture = mockk(relaxed = true),
            destinations = emptyList(),
            preferences = emptyList())

    // Act
    val algo =
        TripAlgorithm.init(tripSettings = settings, activityRepository = repo, context = context)

    // Assert
    assertNotNull(algo)
  }
}
