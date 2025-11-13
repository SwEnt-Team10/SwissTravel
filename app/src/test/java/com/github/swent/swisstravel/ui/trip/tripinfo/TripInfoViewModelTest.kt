package com.github.swent.swisstravel.ui.trip.tripinfo

import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.ui.trip.tripinfos.TripInfoViewModel
import com.google.firebase.Timestamp
import io.mockk.*
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class TripInfoViewModelTest {
  private val testDispatcher = StandardTestDispatcher()
  private lateinit var tripsRepository: TripsRepository
  private lateinit var viewModel: TripInfoViewModel
  val now = Timestamp.now()

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
                  startDate = Timestamp(now.seconds - 3600, 0), // 1h ago
                  endDate = Timestamp(now.seconds + 3600, 0), // 1h later
                  preferredLocations = emptyList(),
                  preferences = emptyList()),
          isFavorite = false,
          isCurrentTrip = false)

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    tripsRepository = mockk()
    viewModel = TripInfoViewModel(tripsRepository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `toggleFavorite updates UI state and calls repository`() = runTest {
    // Arrange
    coEvery { tripsRepository.getTrip(dummyTrip.uid) } returns dummyTrip
    coEvery { tripsRepository.editTrip(dummyTrip.uid, any()) } just Runs

    // Load the trip first
    viewModel.loadTripInfo(dummyTrip.uid)
    testDispatcher.scheduler.advanceUntilIdle()

    assertFalse(viewModel.uiState.value.isFavorite)

    // Act
    viewModel.toggleFavorite()
    testDispatcher.scheduler.advanceUntilIdle()

    // Assert
    // UI state updated optimistically
    assertTrue(viewModel.uiState.value.isFavorite)

    // Repository update called with updated favorite
    coVerify { tripsRepository.editTrip(dummyTrip.uid, match { it.isFavorite }) }
  }
}
