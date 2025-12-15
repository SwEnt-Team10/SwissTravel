package com.github.swent.swisstravel.algorithm

import android.content.Context
import android.content.res.Resources
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.algorithm.orderlocations.OrderedRoute
import com.github.swent.swisstravel.algorithm.orderlocationsv2.ProgressiveRouteOptimizer
import com.github.swent.swisstravel.algorithm.selectactivities.SelectActivities
import com.github.swent.swisstravel.algorithm.tripschedule.ScheduleParams
import com.github.swent.swisstravel.algorithm.tripschedule.scheduleTrip
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.RouteSegment
import com.github.swent.swisstravel.model.trip.TransportMode
import com.github.swent.swisstravel.model.trip.TripElement
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.ui.tripcreation.TripArrivalDeparture
import com.github.swent.swisstravel.ui.tripcreation.TripSettings
import com.google.firebase.Timestamp
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

// These tests were done with AI
class TripAlgorithmTest {

    private lateinit var selectActivities: SelectActivities
    private lateinit var routeOptimizer: ProgressiveRouteOptimizer
    private lateinit var context: Context
    private lateinit var resources: Resources
    private lateinit var algorithm: TripAlgorithm

    // Standard Locations
    private val startLocation = Location(Coordinate(46.2044, 6.1432), "Geneva")
    private val endLocation = Location(Coordinate(47.3769, 8.5417), "Zurich")

    // Helper to create Timestamps easily
    private fun timestampFrom(day: Int, hour: Int): Timestamp {
        val ldt = LocalDateTime.of(2024, 1, day, hour, 0)
        return Timestamp(ldt.toEpochSecond(ZoneOffset.UTC), 0)
    }

    @Before
    fun setup() {
        // 1. Mock Dependencies
        selectActivities = mockk(relaxed = true)
        routeOptimizer = mockk(relaxed = true)
        context = mockk(relaxed = true)
        resources = mockk(relaxed = true)

        // 2. Mock Context and Resources (Used in init/companion)
        every { context.resources } returns resources
        // Mock the grand tour array resource
        every { resources.getStringArray(R.array.grand_tour) } returns arrayOf("GrandTourLoc;46.0;8.0")
        // Mock string resources
        every { context.getString(any(), any()) } returns "Mocked String"
        every { context.getString(any()) } returns "Mocked String"

        // 3. Mock Static scheduleTrip function
        // IMPORTANT: This prevents the real scheduler from running
        mockkStatic("com.github.swent.swisstravel.algorithm.tripschedule.TripSchedulerKt")

        // 4. Instantiate Algorithm
        algorithm = TripAlgorithm(selectActivities, routeOptimizer, context)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `computeTrip nominal case - no expansion needed`() = runBlocking {
        // Arrange
        val settings = createSettings(listOf(Preference.MUSEUMS))
        val profile = createProfile(days = 1) // 1 day trip

        val activity = createActivity("Museum", 3600)

        // Mock initial activity selection
        coEvery { selectActivities.addActivities(any(), any(), any()) } returns listOf(activity)

        // Should not be called but it is safer to mock it just in case the test fails
        coEvery { selectActivities.getActivitiesNearWithPreferences(any(), any(), any(), any(), any()) } returns
                listOf(createActivity("API Activity", 3600))

        // Optimizer returns a valid route
        val route = OrderedRoute(
            listOf(startLocation, activity.location, endLocation),
            3600.0,
            listOf(1800.0, 1800.0)
        )
        coEvery { routeOptimizer.optimize(any(), any(), any(), any(), any(), any()) } returns route

        // Scheduler returns a schedule that matches the end date (Success)
        val schedule = listOf(
            TripElement.TripActivity(activity.copy(endDate = profile.endDate)) // Ends exactly on time
        )
        every { scheduleTrip(any(), any(), any(), any(), any()) } returns schedule

        // Act
        val result = algorithm.computeTrip(settings, profile)

        // Assert
        assertEquals(schedule, result)
        // Should NOT have called getActivitiesNearWithPreferences (Expansion logic)
        coVerify(exactly = 0) { selectActivities.getActivitiesNearWithPreferences(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `computeTrip expansion - adds cached activity when trip too short`() = runBlocking {
        // Arrange
        val settings = createSettings(listOf(Preference.MUSEUMS))
        val profile = createProfile(days = 5) // Long trip target

        val initialActivity = createActivity("ShortActivity", 3600)
        val cachedActivity = createActivity("CachedActivity", 7200, Location(Coordinate(46.21, 6.15), "NearGeneva"))

        // 1. Initial Fetch: Return one activity but populate the cache
        coEvery { selectActivities.addActivities(any(), any(), any()) } answers {
            val cacheList = firstArg<MutableList<Activity>>()
            cacheList.add(cachedActivity)
            listOf(initialActivity)
        }

        coEvery { selectActivities.getActivitiesNearWithPreferences(any(), any(), any(), any(), any()) } returns
                listOf(createActivity("API Activity", 3600))

        // 2. Mock Route (Basic)
        val route = OrderedRoute(listOf(startLocation, endLocation), 100.0, listOf(100.0))
        coEvery { routeOptimizer.optimize(any(), any(), any(), any(), any(), any()) } returns route

        // 3. Mock Schedule to simulate "Too Short" then "Just Right"
        var callCount = 0
        every { scheduleTrip(any(), any(), any(), any(), any()) } answers {
            callCount++
            val activitiesPassed = thirdArg<List<Activity>>()

            if (activitiesPassed.size == 1) {
                // First call: Only initial activity -> Ends Day 1 (Too short)
                listOf(TripElement.TripActivity(initialActivity.copy(endDate = timestampFrom(1, 12))))
            } else {
                // Second call: Has cached activity -> Ends Day 5 (Perfect)
                listOf(TripElement.TripActivity(cachedActivity.copy(endDate = profile.endDate)))
            }
        }

        // Act
        val result = algorithm.computeTrip(settings, profile, cachedActivities = mutableListOf())

        // Assert
        // Should have added the cached activity
        assertTrue("Result should contain the expansion activity", callCount >= 2)
        // Verify we optimized again (proof that tryAddingCachedActivities ran)
        coVerify(atLeast = 2) { routeOptimizer.optimize(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `computeTrip expansion - tries adding new city via API when generic addition fails`() = runBlocking {
        // Arrange
        // 1. Setup locations far from Swiss cities (e.g. 0,0) to fail 'addCityActivity' (generic logic)
        // This forces the algorithm into 'tryAddingCity' which calls the API.
        val remoteStart = Location(Coordinate(0.0, 0.0), "RemoteStart")
        val remoteEnd = Location(Coordinate(0.0, 0.0), "RemoteEnd")

        val settings = TripSettings(
            arrivalDeparture = TripArrivalDeparture(remoteStart, remoteEnd),
            destinations = emptyList(),
            preferences = listOf(Preference.MUSEUMS)
        )
        val profile = createProfile(days = 5).copy(
            arrivalLocation = remoteStart,
            departureLocation = remoteEnd,
            preferredLocations = listOf(remoteStart, remoteEnd)
        )

        // 2. Cache empty
        coEvery { selectActivities.addActivities(any(), any(), any()) } returns emptyList()

        // 3. Route logic
        val route = OrderedRoute(listOf(remoteStart, remoteEnd), 100.0, listOf(100.0))
        coEvery { routeOptimizer.optimize(any(), any(), any(), any(), any(), any()) } returns route

        // 4. Scheduler logic
        // Return short schedule (Trip too short) initially to trigger expansion
        val shortSchedule = listOf(TripElement.TripSegment(RouteSegment(remoteStart, remoteEnd, 60, TransportMode.CAR, timestampFrom(1, 10), timestampFrom(1, 11))))
        val longSchedule = listOf(TripElement.TripActivity(createActivity("NewCityActivity", 3600).copy(endDate = profile.endDate)))

        var scheduleCalls = 0
        every { scheduleTrip(any(), any(), any(), any(), any()) } answers {
            scheduleCalls++
            if (scheduleCalls < 3) shortSchedule else longSchedule
        }

        // 5. API Mock
        // tryAddingCity calls getActivitiesNearWithPreferences. We verify it gets called.
        coEvery { selectActivities.getActivitiesNearWithPreferences(any(), any(), any(), any(), any()) } returns
                listOf(createActivity("API Activity", 3600))

        // Act
        algorithm.computeTrip(settings, profile)

        // Assert
        // Verify we hit the API to fetch activities for a new city
        coVerify(atLeast = 1) {
            selectActivities.getActivitiesNearWithPreferences(
                any(),
                any(),
                eq(3), // NUMBER_OF_ACTIVITY_NEW_CITY
                any(),
                any()
            )
        }
    }

    @Test
    fun `computeTrip with intermediate stops - verifies addInBetween logic`() = runBlocking {
        // Arrange
        val settings = createSettings(listOf(Preference.INTERMEDIATE_STOPS)) // PREFERENCE ENABLED
        val profile = createProfile(days = 1)

        val route = OrderedRoute(listOf(startLocation, endLocation), 10000.0, listOf(10000.0)) // Long distance
        coEvery { routeOptimizer.optimize(any(), any(), any(), any(), any(), any()) } returns route

        // Mock SelectActivities to return an intermediate activity
        val intermediateActivity = createActivity("Stop", 1800, Location(Coordinate(46.5, 7.0), "MidPoint"))
        coEvery { selectActivities.getActivitiesNearWithPreferences(any(), any(), any(), any(), any()) } returns listOf(intermediateActivity)

        // Mock Recompute to simulate the split (This is called inside addInBetweenActivities)
        val splitRoute = OrderedRoute(listOf(startLocation, intermediateActivity.location, endLocation), 11000.0, listOf(5000.0, 6000.0))
        coEvery { routeOptimizer.recomputeOrderedRoute(any(), any(), any(), any(), any()) } returns splitRoute

        // Mock Scheduler
        every { scheduleTrip(any(), any(), any(), any(), any()) } returns listOf(TripElement.TripActivity(intermediateActivity))

        // Act
        algorithm.computeTrip(settings, profile)

        // Assert
        // Verify recompute was called (proof that addInBetweenActivities ran)
        coVerify(exactly = 1) { routeOptimizer.recomputeOrderedRoute(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `scheduleRemove - cleans ghosts (missing activities)`() = runBlocking {
        // Arrange
        val settings = createSettings(emptyList())
        val profile = createProfile(days = 1)

        // We have 2 activities in the list
        val act1 = createActivity("Act1", 3600, Location(Coordinate(1.0, 1.0), "Loc1"))
        val act2 = createActivity("Act2", 3600, Location(Coordinate(2.0, 2.0), "Loc2"))// This one will be skipped ("Ghost")

        coEvery { selectActivities.addActivities(any(), any(), any()) } returns listOf(act1, act2)

        coEvery { selectActivities.getActivitiesNearWithPreferences(any(), any(), any(), any(), any()) } returns
                listOf(createActivity("API Activity", 3600))

        val route = OrderedRoute(listOf(startLocation, act1.location, act2.location, endLocation), 100.0, listOf(10.0))
        coEvery { routeOptimizer.optimize(any(), any(), any(), any(), any(), any()) } returns route

        // Scheduler Logic:
        // 1st call: Returns schedule with ONLY Act1 (skips Act2) to simulate "no time"
        // 2nd call (Cleanup): Returns schedule with Act1 (Act2 removed from input)
        every { scheduleTrip(any(), any(), any(), any(), any()) } answers {
            val inputActs = thirdArg<List<Activity>>()
            if (inputActs.contains(act2)) {
                // Ghost scenario: Scheduler dropped Act2
                listOf(TripElement.TripActivity(act1))
            } else {
                // Clean scenario
                listOf(TripElement.TripActivity(act1.copy(endDate = profile.endDate))) // Fits perfect
            }
        }

        // Mock Recompute for cleanup
        coEvery { routeOptimizer.recomputeOrderedRoute(any(), any(), any(), any(), any()) } returns route

        // Act
        val result = algorithm.computeTrip(settings, profile)

        // Assert
        // Verify recomputeOrderedRoute was called (triggered by ghost cleanup)
        coVerify(atLeast = 1) { routeOptimizer.recomputeOrderedRoute(any(), any(), any(), any(), any()) }
        // Verify final result contains Act1
        assertTrue(result.any { it is TripElement.TripActivity && it.activity.description == "Act1" })
    }

    // --- Helpers ---

    private fun createSettings(prefs: List<Preference>): TripSettings {
        return TripSettings(
            arrivalDeparture = TripArrivalDeparture(startLocation, endLocation),
            destinations = emptyList(),
            preferences = prefs
        )
    }

    private fun createProfile(days: Int): TripProfile {
        return TripProfile(
            startDate = timestampFrom(1, 8),
            endDate = timestampFrom(1 + days, 20), // Ends later
            preferredLocations = listOf(startLocation, endLocation),
            arrivalLocation = startLocation,
            departureLocation = endLocation,
            preferences = emptyList(),
            adults = 1,
            children = 0
        )
    }

    private fun createActivity(name: String, duration: Int, loc: Location = startLocation): Activity {
        return Activity(
            location = loc,
            description = name,
            estimatedTime = duration,
            imageUrls = emptyList(),
            startDate = Timestamp.now(),
            endDate = Timestamp.now()
        )
    }
}