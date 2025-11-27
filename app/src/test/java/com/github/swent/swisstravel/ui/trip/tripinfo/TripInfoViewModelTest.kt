package com.github.swent.swisstravel.ui.trip.tripinfo

import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.ui.trip.tripinfos.TripInfoViewModel
import com.google.firebase.Timestamp
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class TripInfoViewModelTest {
  private val testDispatcher = StandardTestDispatcher()
  private lateinit var tripsRepository: TripsRepository
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
          isCurrentTrip = false)

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockkStatic(android.util.Log::class)
    every { android.util.Log.d(any(), any()) } returns 0
    every { android.util.Log.e(any(), any()) } returns 0
    every { android.util.Log.e(any(), any(), any()) } returns 0

    tripsRepository = mockk()
    viewModel = TripInfoViewModel(tripsRepository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  // --- Existing Test ---
  @Test
  fun `toggleFavorite updates UI state and calls repository`() = runTest {
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
    assertNotNull(viewModel.uiState.value.errorMsg)

    // Act
    viewModel.clearErrorMsg()

    // Assert
    assertNull(viewModel.uiState.value.errorMsg)
  }
}
