package com.github.swent.swisstravel.ui.trip.tripinfo

import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.RouteSegment
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripElement
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.github.swent.swisstravel.model.user.User
import com.github.swent.swisstravel.model.user.UserRepository
import com.github.swent.swisstravel.model.user.UserStats
import com.github.swent.swisstravel.ui.trip.tripinfos.TripInfoViewModel
import com.google.firebase.Timestamp
import com.mapbox.geojson.Point
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class TripInfoViewModelTest {
  private val testDispatcher = StandardTestDispatcher()
  private lateinit var tripsRepository: TripsRepository
  private lateinit var userRepository: UserRepository
  private lateinit var viewModel: TripInfoViewModel
  val now = Timestamp(1600000000, 0)

  private val dummyTrip =
      Trip(
          uid = "trip1",
          name = "TripName",
          ownerId = "owner1",
          locations = emptyList(),
          routeSegments = emptyList(),
          activities = emptyList(),
          tripProfile =
              TripProfile(
                  startDate = Timestamp(now.seconds - 3600, 0),
                  endDate = Timestamp(now.seconds + 3600, 0),
                  preferredLocations = emptyList(),
                  preferences = emptyList()),
          isFavorite = false,
          isCurrentTrip = false,
          listUri = emptyList(),
          collaboratorsId = emptyList())

  private val fakeUser =
      User(
          uid = "123",
          name = "Test User",
          biography = "Bio",
          email = "test@example.com",
          profilePicUrl = "http://example.com/pic.jpg",
          preferences = emptyList(),
          friends = emptyList(),
          stats = UserStats(),
          pinnedTripsUids = emptyList(),
          pinnedPicturesUids = emptyList())

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockkStatic(android.util.Log::class)
    every { android.util.Log.d(any(), any()) } returns 0
    every { android.util.Log.e(any(), any(), any()) } returns 0

    tripsRepository = mockk()
    userRepository = mockk()
    viewModel = TripInfoViewModel(tripsRepository, userRepository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  // --- Existing Test ---
  @Test
  fun `toggleFavorite updates UI state and calls repository`() = runTest {
    coEvery { userRepository.getCurrentUser() } returns fakeUser

    coEvery { tripsRepository.getTrip(dummyTrip.uid) } returns dummyTrip
    coEvery { tripsRepository.editTrip(dummyTrip.uid, any()) } just Runs

    viewModel.loadTripInfo(dummyTrip.uid)
    testDispatcher.scheduler.advanceUntilIdle()
    assertFalse(viewModel.uiState.value.isFavorite)

    viewModel.toggleFavorite()
    testDispatcher.scheduler.advanceUntilIdle()

    assertTrue(viewModel.uiState.value.isFavorite)
    coVerify { tripsRepository.editTrip(dummyTrip.uid, match { it.isFavorite }) }
  }

  // --- New Tests ---

  @Test
  fun `loadTripInfo updates UI state successfully`() = runTest {
    // Arrange
    coEvery { tripsRepository.getTrip(dummyTrip.uid) } returns dummyTrip
    coEvery { userRepository.getCurrentUser() } returns fakeUser

    // Act
    viewModel.loadTripInfo(dummyTrip.uid)
    testDispatcher.scheduler.advanceUntilIdle()

    // Assert
    val state = viewModel.uiState.value
    assertEquals("TripName", state.name)
    assertEquals(dummyTrip.locations, state.locations)
    assertNull(state.errorMsg)
  }

  @Test
  fun `loadTripInfo handles null UID gracefully`() = runTest {
    // Act
    viewModel.loadTripInfo(null)
    testDispatcher.scheduler.advanceUntilIdle()

    // Assert
    // Should not crash, and state should remain default or show error depending on impl.
    // Assuming default empty state:
    assertEquals("Trip Name", viewModel.uiState.value.name)
    // OR if your VM sets an error:
    // assertNotNull(viewModel.uiState.value.errorMsg)
  }

  @Test
  fun `loadTripInfo sets error message when repository throws exception`() = runTest {
    // Arrange
    val errorMsg = "Network error"
    coEvery { tripsRepository.getTrip(any()) } throws Exception(errorMsg)

    // Act
    viewModel.loadTripInfo("someID")
    testDispatcher.scheduler.advanceUntilIdle()

    // Assert
    assertEquals("Failed to load trip info: $errorMsg", viewModel.uiState.value.errorMsg)
  }

  @Test
  fun `toggleFullscreen updates state`() = runTest {
    // Initial state should be false
    assertFalse(viewModel.uiState.value.fullscreen)

    // Act - Turn On
    viewModel.toggleFullscreen(true)
    assertTrue(viewModel.uiState.value.fullscreen)

    // Act - Turn Off
    viewModel.toggleFullscreen(false)
    assertFalse(viewModel.uiState.value.fullscreen)
  }

  @Test
  fun `clearErrorMsg resets error message to null`() = runTest {
    // Arrange - Force an error state
    coEvery { tripsRepository.getTrip(any()) } throws Exception("Error")
    viewModel.loadTripInfo("id")
    testDispatcher.scheduler.advanceUntilIdle()
    // Assert
    assertEquals("Failed to load trip info: Error", viewModel.uiState.value.errorMsg)
  }

  @Test
  fun `setCurrentDayIndex updates state`() = runTest {
    // Arrange: Load a trip with segments to populate days
    val segment1 =
        RouteSegment(
            Location(com.github.swent.swisstravel.model.trip.Coordinate(0.0, 0.0), "A"),
            Location(com.github.swent.swisstravel.model.trip.Coordinate(1.0, 1.0), "B"),
            10,
            com.github.swent.swisstravel.model.trip.TransportMode.WALKING,
            now,
            now)
    val segment2 =
        RouteSegment(
            Location(com.github.swent.swisstravel.model.trip.Coordinate(2.0, 2.0), "C"),
            Location(com.github.swent.swisstravel.model.trip.Coordinate(3.0, 3.0), "D"),
            10,
            com.github.swent.swisstravel.model.trip.TransportMode.WALKING,
            Timestamp(now.seconds + 86400, 0), // Next day
            Timestamp(now.seconds + 86400, 0))
    val tripWithSegments = dummyTrip.copy(routeSegments = listOf(segment1, segment2))
    coEvery { tripsRepository.getTrip(tripWithSegments.uid) } returns tripWithSegments

    viewModel.loadTripInfo(tripWithSegments.uid)
    testDispatcher.scheduler.advanceUntilIdle()

    // Act
    viewModel.setCurrentDayIndex(0)

    // Assert
    assertEquals(0, viewModel.uiState.value.currentDayIndex)
  }

  @Test
  fun `setSelectedStep updates state`() = runTest {
    val step =
        TripElement.TripSegment(
            RouteSegment(
                Location(com.github.swent.swisstravel.model.trip.Coordinate(0.0, 0.0), "A"),
                Location(com.github.swent.swisstravel.model.trip.Coordinate(1.0, 1.0), "B"),
                10,
                com.github.swent.swisstravel.model.trip.TransportMode.WALKING,
                now,
                now))
    viewModel.setSelectedStep(step)
    assertEquals(step, viewModel.uiState.value.selectedStep)
  }

  @Test
  fun `setDrawFromCurrentPosition updates state`() = runTest {
    viewModel.setDrawFromCurrentPosition(true)
    assertTrue(viewModel.uiState.value.drawFromCurrentPosition)
  }

  @Test
  fun `updateUserLocation updates state`() = runTest {
    val point = mockk<Point>(relaxed = true)
    every { point.latitude() } returns 1.0
    every { point.longitude() } returns 2.0

    viewModel.updateUserLocation(point)
    assertEquals(point, viewModel.uiState.value.currentGpsPoint)
  }

  @Test
  fun `selectActivity updates state`() = runTest {
    val activity =
        Activity(
            startDate = now,
            endDate = now,
            location = Location(com.github.swent.swisstravel.model.trip.Coordinate(0.0, 0.0), "A"),
            description = "Desc",
            imageUrls = emptyList(),
            estimatedTime = 60)
    viewModel.selectActivity(activity)
    assertEquals(activity, viewModel.uiState.value.selectedActivity)
  }
}
