package com.github.swent.swisstravel.algorithm

import com.github.swent.swisstravel.algorithm.orderlocations.OrderedRoute
import com.github.swent.swisstravel.algorithm.orderlocationsv2.ProgressiveRouteOptimizer
import com.github.swent.swisstravel.algorithm.selectactivities.SelectActivities
import com.github.swent.swisstravel.algorithm.tripschedule.ScheduleParams
import com.github.swent.swisstravel.algorithm.tripschedule.scheduleTrip
import com.github.swent.swisstravel.model.trip.*
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.ui.tripcreation.TripArrivalDeparture
import com.github.swent.swisstravel.ui.tripcreation.TripSettings
import com.google.firebase.Timestamp
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlin.test.assertEquals
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

    coEvery { selectActivities.addActivities() } returns activityElements

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
}
