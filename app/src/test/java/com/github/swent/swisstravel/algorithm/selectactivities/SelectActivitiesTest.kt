package com.github.swent.swisstravel.algorithm.selectactivities

import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.github.swent.swisstravel.model.trip.activity.ActivityRepository
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.ui.trip.tripinfos.TripInfoViewModelContract
import com.github.swent.swisstravel.ui.tripcreation.TripArrivalDeparture
import com.github.swent.swisstravel.ui.tripcreation.TripDate
import com.github.swent.swisstravel.ui.tripcreation.TripSettings
import com.google.firebase.Timestamp
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SelectActivitiesTest {

  private lateinit var mockActivityRepository: ActivityRepository
  private lateinit var tripSettings: TripSettings
  private lateinit var mockTripInfoVM: TripInfoViewModelContract

  // Test data
  private val lausanne = Location(Coordinate(46.5197, 6.6323), "Lausanne", "url")
  private val geneva = Location(Coordinate(46.2044, 6.1432), "Geneva", "url")
  private val zurich = Location(Coordinate(47.3769, 8.5417), "Zurich", "url")

  private val activityLausanne =
      Activity(
          Timestamp.now(), Timestamp.now(), lausanne, "Activity in Lausanne", emptyList(), 3600)
  private val activityGeneva =
      Activity(Timestamp.now(), Timestamp.now(), geneva, "Activity in Geneva", emptyList(), 3600)
  private val activityZurich =
      Activity(Timestamp.now(), Timestamp.now(), zurich, "Activity in Zurich", emptyList(), 3600)

  @Before
  fun setUp() {
    mockActivityRepository = mockk()
    mockTripInfoVM = mockk(relaxed = true)
    tripSettings =
        TripSettings(
            destinations = listOf(lausanne),
            date = TripDate(LocalDate.of(2025, 7, 20), LocalDate.of(2025, 7, 21)), // 2-day trip
            preferences = emptyList(),
            arrivalDeparture =
                TripArrivalDeparture(departureLocation = geneva, arrivalLocation = zurich))
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `addActivities returns activities when no preferences are set`() = runBlocking {
    // Given
    val expectedActivities = listOf(activityLausanne, activityGeneva, activityZurich)
    val progressUpdates = mutableListOf<Float>()

    coEvery { mockActivityRepository.getActivitiesNear(any(), any(), any()) } returns
        listOf(activityLausanne) andThen
        listOf(activityGeneva) andThen
        listOf(activityZurich)

    val selectActivities =
        SelectActivities(
            tripSettings, tripInfoVM = mockTripInfoVM, activityRepository = mockActivityRepository)

    // When
    val result = selectActivities.addActivities { progressUpdates.add(it) }

    // Then
    assertEquals(3, result.size)
    assertTrue(result.containsAll(expectedActivities))
    assertTrue(progressUpdates.last() == 1.0f)
  }

  @Test
  fun `addActivities returns activities based on optional and mandatory preferences`() =
      runBlocking {
        // Given
        val preferences =
            listOf(Preference.WHEELCHAIR_ACCESSIBLE, Preference.MUSEUMS, Preference.HIKE)
        tripSettings = tripSettings.copy(preferences = preferences)
        val progressUpdates = mutableListOf<Float>()

        val mandatory = listOf(Preference.WHEELCHAIR_ACCESSIBLE)
        val optional = listOf(Preference.MUSEUMS, Preference.HIKE)

        // Expect a single call with all optional preferences
        coEvery {
          mockActivityRepository.getActivitiesNearWithPreference(
              match { it.containsAll(mandatory + optional) }, any(), any(), any())
        } returns
            listOf(activityLausanne) andThen
            listOf(activityGeneva) andThen
            listOf(activityZurich)

        val selectActivities =
            SelectActivities(
                tripSettings,
                tripInfoVM = mockTripInfoVM,
                activityRepository = mockActivityRepository)

        // When
        val result = selectActivities.addActivities { progressUpdates.add(it) }

        // Then
        // 3 destinations * 1 call (batched optional preferences) = 3 calls
        assertEquals(3, result.size)
        assertTrue(result.containsAll(listOf(activityLausanne, activityGeneva, activityZurich)))
        assertTrue(progressUpdates.last() == 1.0f)
      }

  @Test
  fun `addActivities returns activities based on mandatory preferences`() = runBlocking {
    // Given
    val preferences = listOf(Preference.WHEELCHAIR_ACCESSIBLE)
    tripSettings = tripSettings.copy(preferences = preferences)
    val progressUpdates = mutableListOf<Float>()

    val mandatory = listOf(Preference.WHEELCHAIR_ACCESSIBLE)

    // Expect a single call with all optional preferences
    coEvery {
      mockActivityRepository.getActivitiesNearWithPreference(
          match { it.containsAll(mandatory) }, any(), any(), any())
    } returns listOf(activityLausanne) andThen listOf(activityGeneva) andThen listOf(activityZurich)

    val selectActivities =
        SelectActivities(
            tripSettings, tripInfoVM = mockTripInfoVM, activityRepository = mockActivityRepository)

    // When
    val result = selectActivities.addActivities { progressUpdates.add(it) }

    // Then
    // 3 destinations * 1 call (batched optional preferences) = 3 calls
    assertEquals(3, result.size)
    assertTrue(result.containsAll(listOf(activityLausanne, activityGeneva, activityZurich)))
    assertTrue(progressUpdates.last() == 1.0f)
  }

  @Test
  fun `addActivities returns empty list when no destinations are provided`() = runBlocking {
    // Given
    tripSettings =
        tripSettings.copy(
            destinations = emptyList(), arrivalDeparture = TripArrivalDeparture(null, null))
    val selectActivities =
        SelectActivities(
            tripSettings, tripInfoVM = mockTripInfoVM, activityRepository = mockActivityRepository)

    // When
    val result = selectActivities.addActivities {}

    // Then
    assertTrue(result.isEmpty())
  }

  @Test
  fun `onProgress is called and finishes with 1f`() = runBlocking {
    // Given
    val progressUpdates = mutableListOf<Float>()
    coEvery { mockActivityRepository.getActivitiesNear(any(), any(), any()) } returns
        listOf(activityLausanne)

    val selectActivities =
        SelectActivities(
            tripSettings, tripInfoVM = mockTripInfoVM, activityRepository = mockActivityRepository)

    // When
    selectActivities.addActivities { progressUpdates.add(it) }

    // Then
    assertTrue(progressUpdates.isNotEmpty())
    assertEquals(1.0f, progressUpdates.last())
    // 3 destinations = 3 steps -> 1/3, 2/3, 1.0
    assertEquals(4, progressUpdates.size)
    assertEquals(1 / 3f, progressUpdates[0], 0.01f)
    assertEquals(2 / 3f, progressUpdates[1], 0.01f)
    assertEquals(3 / 3f, progressUpdates[2], 0.01f)
  }

  @Test
  fun `addActivities works with an empty preference list`() = runBlocking {
    // Given
    tripSettings = tripSettings.copy(preferences = emptyList())
    coEvery { mockActivityRepository.getActivitiesNear(geneva.coordinate, any(), any()) } returns
        listOf(activityGeneva)
    coEvery { mockActivityRepository.getActivitiesNear(lausanne.coordinate, any(), any()) } returns
        listOf(activityLausanne)
    coEvery { mockActivityRepository.getActivitiesNear(zurich.coordinate, any(), any()) } returns
        listOf(activityZurich)

    val selectActivities =
        SelectActivities(
            tripSettings, tripInfoVM = mockTripInfoVM, activityRepository = mockActivityRepository)

    // When
    val result = selectActivities.addActivities {}

    // Then
    // 3 destinations, each call returns one activity
    assertEquals(3, result.size)
  }

  @Test
  fun `getOneActivityNearWithPreferences returns activity using preference-based fetch`() =
      runBlocking {
        val mandatory = listOf(Preference.WHEELCHAIR_ACCESSIBLE)
        val optional = listOf(Preference.MUSEUMS)
        // Given user preferences
        tripSettings = tripSettings.copy(preferences = mandatory + optional)

        val selectActivities =
            SelectActivities(
                tripSettings,
                tripInfoVM = mockTripInfoVM,
                activityRepository = mockActivityRepository)

        // Mock: must call getActivitiesNearWithPreference with mandatory + optional
        coEvery {
          mockActivityRepository.getActivitiesNearWithPreference(
              match { prefs -> prefs.containsAll(mandatory + optional) },
              lausanne.coordinate,
              any(),
              1)
        } returns listOf(activityLausanne)

        // When
        val result = selectActivities.getOneActivityNearWithPreferences(lausanne.coordinate)

        // Then
        assertEquals(activityLausanne, result)

        // Ensure getActivitiesNear() was NOT called
        io.mockk.coVerify(exactly = 0) {
          mockActivityRepository.getActivitiesNear(any(), any(), any())
        }

        // Ensure preference-based call WAS made
        io.mockk.coVerify(exactly = 1) {
          mockActivityRepository.getActivitiesNearWithPreference(
              any(), lausanne.coordinate, any(), 1)
        }
      }

  @Test
  fun `getOneActivityNearWithPreferences returns activity using mandatory preference-based fetch`() =
      runBlocking {
        val mandatory = listOf(Preference.WHEELCHAIR_ACCESSIBLE)

        // Given user preferences
        tripSettings = tripSettings.copy(preferences = mandatory)

        val selectActivities =
            SelectActivities(
                tripSettings,
                tripInfoVM = mockTripInfoVM,
                activityRepository = mockActivityRepository)

        // Mock: must call getActivitiesNearWithPreference with mandatory + optional
        coEvery {
          mockActivityRepository.getActivitiesNearWithPreference(
              match { prefs -> prefs.containsAll(mandatory) }, lausanne.coordinate, any(), 1)
        } returns listOf(activityLausanne)

        // When
        val result = selectActivities.getOneActivityNearWithPreferences(lausanne.coordinate)

        // Then
        assertEquals(activityLausanne, result)

        // Ensure getActivitiesNear() was NOT called
        io.mockk.coVerify(exactly = 0) {
          mockActivityRepository.getActivitiesNear(any(), any(), any())
        }

        // Ensure preference-based call WAS made
        io.mockk.coVerify(exactly = 1) {
          mockActivityRepository.getActivitiesNearWithPreference(
              any(), lausanne.coordinate, any(), 1)
        }
      }

  @Test
  fun `getOneActivityNearWithPreferences returns activity when no preferences`() = runBlocking {
    // Given no preferences
    tripSettings = tripSettings.copy(preferences = emptyList())

    val selectActivities =
        SelectActivities(
            tripSettings, tripInfoVM = mockTripInfoVM, activityRepository = mockActivityRepository)

    // Mock: should call getActivitiesNear()
    coEvery { mockActivityRepository.getActivitiesNear(lausanne.coordinate, any(), 1) } returns
        listOf(activityLausanne)

    // When
    val result = selectActivities.getOneActivityNearWithPreferences(lausanne.coordinate)

    // Then
    assertEquals(activityLausanne, result)

    // Ensure preference call NOT used
    io.mockk.coVerify(exactly = 0) {
      mockActivityRepository.getActivitiesNearWithPreference(any(), any(), any(), any())
    }

    // Ensure normal near call WAS used
    io.mockk.coVerify(exactly = 1) {
      mockActivityRepository.getActivitiesNear(lausanne.coordinate, any(), 1)
    }
  }
}
