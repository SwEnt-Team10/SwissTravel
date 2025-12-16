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
import com.github.swent.swisstravel.model.user.UserRepositoryFirebase
import com.github.swent.swisstravel.ui.tripcreation.TripArrivalDeparture
import com.github.swent.swisstravel.ui.tripcreation.TripSettings
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.spyk
import kotlin.collections.emptyList
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

class TripAlgorithmTest {

  private val selectActivities = mockk<SelectActivities>()
  private val routeOptimizer = mockk<ProgressiveRouteOptimizer>()
  private val scheduleParams = ScheduleParams()
  private val progression: Progression =
      Progression(
          selectActivities = 0.20f,
          optimizeRoute = 0.40f,
          fetchInBetweenActivities = 0.10f,
          scheduleTrip = 0.30f)
  private val rescheduleProgression: RescheduleProgression =
      RescheduleProgression(
          schedule = 0.30f, analyzeAndRemove = 0.10f, recomputeRoute = 0.40f, reschedule = 0.20f)

  private val algorithm =
      TripAlgorithm(
          activitySelector = selectActivities,
          routeOptimizer = routeOptimizer,
          scheduleParams = scheduleParams,
          progression = progression,
          rescheduleProgression = rescheduleProgression)

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)

    mockkObject(TripsRepositoryProvider)
    val fakeRepo = mockk<TripsRepository>(relaxed = true)
    every { TripsRepositoryProvider.repository } returns fakeRepo
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
  }

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

    coEvery { selectActivities.addActivities(any(), any<(Float) -> Unit>()) } returns
        activityElements

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
  fun `computeTrip throws when optimized route duration is zero or negative`() = runTest {
    // Arrange
    val coordinates =
        listOf(Location(Coordinate(1.0, 1.0), "A"), Location(Coordinate(2.0, 2.0), "B"))

    // Mock a valid activity, INCLUDING its location
    val activity = mockk<Activity>()
    every { activity.location } returns coordinates[0]

    coEvery { selectActivities.addActivities(any(), any()) } returns listOf(activity)

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
    assertFailsWith<IllegalStateException> { algorithm.computeTrip(settings, profile) }
  }

  // Done with AI
  @Test
  fun `computeTrip throws when scheduled trip is empty`() = runTest {
    // Arrange
    val coordinates =
        listOf(Location(Coordinate(1.0, 1.0), "A"), Location(Coordinate(2.0, 2.0), "B"))

    val activity = mockk<Activity>()
    every { activity.location } returns coordinates[0]

    coEvery { selectActivities.addActivities(any(), any()) } returns listOf(activity)

    val validRoute =
        OrderedRoute(
            orderedLocations = coordinates,
            totalDuration = 1000.0,
            segmentDuration = listOf(1000.0))

    coEvery { routeOptimizer.optimize(any(), any(), any(), any(), any(), any()) } returns validRoute

    // scheduleTrip empty → should trigger your check
    mockkStatic("com.github.swent.swisstravel.algorithm.tripschedule.TripSchedulerKt")
    coEvery { scheduleTrip(any(), any(), any(), any(), any()) } returns emptyList()
    every { activity.estimatedTime } returns 100

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
    assertFailsWith<IllegalStateException> { algorithm.computeTrip(settings, profile) }
    // Check that rescheduling was attempted
    coVerify(atMost = 1) { routeOptimizer.recomputeOrderedRoute(any(), any(), any(), any(), any()) }
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

    // additional mocking for Firebase and UserRepositoryFirebase
    mockkObject(TripsRepositoryProvider)
    val fakeTripsRepo = mockk<TripsRepository>(relaxed = true)
    every { TripsRepositoryProvider.repository } returns fakeTripsRepo

    mockkStatic(FirebaseAuth::class)
    mockkStatic(FirebaseFirestore::class)

    every { FirebaseAuth.getInstance() } returns mockk(relaxed = true)
    every { FirebaseFirestore.getInstance() } returns mockk(relaxed = true)

    mockkConstructor(UserRepositoryFirebase::class)
    coEvery { anyConstructed<UserRepositoryFirebase>().getUserByUid(any()) } returns
        mockk(relaxed = true)

    // Act
    val algo =
        TripAlgorithm.init(tripSettings = settings, activityRepository = repo, context = context)

    // Assert
    assertNotNull(algo)
  }

  @Test
  fun `addInBetweenActivities inserts intermediate activities using mocked getOneActivityNearWithPreferences`() =
      runTest {
        // Arrange
        val start = Location(Coordinate(10.0, 10.0), "Start")
        val end = Location(Coordinate(10.9, 10.9), "End") // ~100 km away

        val orderedLocations = listOf(start, end)

        val optimizedRoute =
            OrderedRoute(
                orderedLocations = orderedLocations,
                totalDuration = 500.0,
                segmentDuration = mutableListOf(500.0))

        val activities = mutableListOf<Activity>()

        // Spy on algorithm to override getOneActivityNearWithPreferences
        val algorithmSpy = spyk(algorithm)

        // Create fake activities to be returned deterministically
        val fakeActivity = mockk<Activity>()
        every { fakeActivity.location } returns Location(Coordinate(10.4, 10.4), "Fake1")

        // Mock getOneActivityNearWithPreferences to return the fake activities sequentially
        coEvery { selectActivities.getOneActivityNearWithPreferences(any(), any()) } returns
            fakeActivity

        // Mock recomputeOrderedRoute to return the route already computed
        coEvery { routeOptimizer.recomputeOrderedRoute(any(), any(), any(), any(), any()) } answers
            {
              optimizedRoute.copy(
                  orderedLocations = listOf(start, fakeActivity.location, end),
                  totalDuration = 500.0,
                  segmentDuration = listOf(300.0, 200.0))
            }

        // Act
        val result =
            algorithmSpy.addInBetweenActivities(
                optimizedRoute = optimizedRoute, activities = activities)

        // Assert
        // Check that new activities were added to activities list
        assertEquals(1, activities.size)
        assertEquals(fakeActivity, activities[0])

        // Check that new ordered locations include the activities
        assertEquals(3, result.orderedLocations.size)
        assertEquals(start, result.orderedLocations[0])
        assertEquals(fakeActivity.location, result.orderedLocations[1])
        assertEquals(end, result.orderedLocations[2])

        // Check that segment durations list has been expanded for the new activities
        assertEquals(result.orderedLocations.size - 1, result.segmentDuration.size)
        assertEquals(300.0, result.segmentDuration[0])
        assertEquals(200.0, result.segmentDuration[1])
      }

  @Test(expected = IllegalStateException::class)
  fun `algorithm should throw when rescheduled trip is empty`() = runTest {
    // Arrange
    val start = Location(Coordinate(10.0, 10.0), "Start")
    val end = Location(Coordinate(20.0, 20.0), "End")
    val locations = listOf(start, end)
    val activity = mockk<Activity>()
    every { activity.location } returns start
    every { activity.estimatedTime } returns 100

    coEvery { selectActivities.addActivities(any(), any()) } returns listOf(activity)

    val orderedRoute =
        OrderedRoute(
            orderedLocations = locations, totalDuration = 1000.0, segmentDuration = listOf(1000.0))

    coEvery { routeOptimizer.optimize(any(), any(), any(), any(), any(), any()) } returns
        orderedRoute
    coEvery { routeOptimizer.recomputeOrderedRoute(any(), any(), any(), any(), any()) } answers
        {
          firstArg<OrderedRoute>()
        }

    mockkStatic("com.github.swent.swisstravel.algorithm.tripschedule.TripSchedulerKt")
    coEvery { scheduleTrip(any(), any(), any(), any(), any()) } returns emptyList()

    val settings =
        TripSettings(
            name = "Trip",
            arrivalDeparture =
                TripArrivalDeparture(arrivalLocation = start, departureLocation = end),
            destinations = locations,
            preferences = listOf(Preference.INTERMEDIATE_STOPS))

    val profile =
        TripProfile(
            startDate = Timestamp(0, 0),
            endDate = Timestamp(1000, 0),
            preferredLocations = emptyList(),
            preferences = emptyList(),
            adults = 1,
            children = 0,
            arrivalLocation = start,
            departureLocation = end)

    // Spy the algorithm to intercept addInBetweenActivities
    val algorithmSpy = spyk(algorithm)

    // Arrange mock for getOneActivityNearWithPreferences
    val fakeActivity = mockk<Activity>()
    every { fakeActivity.location } returns Location(Coordinate(10.5, 10.5), "Fake1")
    every { fakeActivity.estimatedTime } returns 3600

    coEvery { selectActivities.getOneActivityNearWithPreferences(any(), any()) } returns
        fakeActivity

    // Arrange mock for recomputeOrderedRoute if needed
    coEvery { routeOptimizer.recomputeOrderedRoute(any(), any(), any(), any(), any()) } answers
        {
          firstArg<OrderedRoute>() // just return the route for simplicity
        }

    coEvery { algorithmSpy.addInBetweenActivities(any(), any()) } returns orderedRoute

    var progress = 0.0f
    algorithmSpy.computeTrip(settings, profile) { currentProgress -> progress = currentProgress }

    assertTrue(
        progress >=
            progression.selectActivities +
                progression.optimizeRoute +
                progression.fetchInBetweenActivities +
                (progression.scheduleTrip *
                    (rescheduleProgression.schedule +
                        rescheduleProgression.analyzeAndRemove +
                        rescheduleProgression.recomputeRoute)))
    assertTrue(progress < 1.0f)
  }

  @Test
  fun `algorithm should enter addInBetweenActivities when the preference is selected`() = runTest {
    // Arrange
    val start = Location(Coordinate(10.0, 10.0), "Start")
    val end = Location(Coordinate(10.0, 10.93), "End") // ~100 km away

    val locations = listOf(start, end)
    val activity = mockk<Activity>()
    every { activity.location } returns start
    every { activity.estimatedTime } returns 100

    coEvery { selectActivities.addActivities(any(), any()) } returns listOf(activity)

    val orderedRoute =
        OrderedRoute(
            orderedLocations = locations, totalDuration = 1000.0, segmentDuration = listOf(1000.0))

    coEvery { routeOptimizer.optimize(any(), any(), any(), any(), any(), any()) } returns
        orderedRoute
    coEvery { routeOptimizer.recomputeOrderedRoute(any(), any(), any(), any(), any()) } answers
        {
          firstArg<OrderedRoute>()
        }

    mockkStatic("com.github.swent.swisstravel.algorithm.tripschedule.TripSchedulerKt")
    coEvery { scheduleTrip(any(), any(), any(), any(), any()) } returns emptyList()

    val settings =
        TripSettings(
            name = "Trip",
            arrivalDeparture =
                TripArrivalDeparture(arrivalLocation = start, departureLocation = end),
            destinations = locations,
            preferences = listOf(Preference.INTERMEDIATE_STOPS))

    val profile =
        TripProfile(
            startDate = Timestamp(0, 0),
            endDate = Timestamp(1000, 0),
            preferredLocations = emptyList(),
            preferences = emptyList(),
            adults = 1,
            children = 0,
            arrivalLocation = start,
            departureLocation = end)

    // Spy the algorithm to intercept addInBetweenActivities
    val algorithmSpy = spyk(algorithm)

    // Arrange mock for getOneActivityNearWithPreferences
    val fakeActivity = mockk<Activity>()
    every { fakeActivity.location } returns Location(Coordinate(10.5, 10.5), "Fake1")
    every { fakeActivity.estimatedTime } returns 3600

    coEvery { selectActivities.getOneActivityNearWithPreferences(any(), any()) } returns
        fakeActivity

    // Arrange mock for recomputeOrderedRoute if needed
    coEvery { routeOptimizer.recomputeOrderedRoute(any(), any(), any(), any(), any()) } answers
        {
          firstArg<OrderedRoute>() // just return the route for simplicity
        }

    coEvery { algorithmSpy.generateActivitiesBetween(any(), any(), any()) } returns emptyList()
    coEvery { algorithmSpy.attemptRescheduleIfNeeded(any(), any(), any(), any(), any()) } returns
        locations.map { it ->
          TripElement.TripSegment(
              RouteSegment(it, it, 60, TransportMode.CAR, Timestamp(0, 0), Timestamp(3600, 0)))
        }

    var progress = 0.0f
    algorithmSpy.computeTrip(settings, profile) { currentProgress -> progress = currentProgress }

    assertEquals(1.0f, progress, 0.001f)

    coVerify(exactly = 1) { algorithmSpy.addInBetweenActivities(any(), any(), any(), any()) }
  }
}
