package com.github.swent.swisstravel.ui.mytrips

import com.github.swent.swisstravel.model.trip.*
import com.google.firebase.Timestamp
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class MyTripsViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var repository: TripsRepository
  private lateinit var viewModel: MyTripsViewModel

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    repository = mockk()
  }

  @After
  fun teardown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `UI state updates with current and upcoming trips`() = runTest {
    val now = Timestamp.now()

    // Current trip: start <= now <= end
    val currentTrip =
        Trip(
            "1",
            "Current Trip",
            "ownerX",
            emptyList(),
            emptyList(),
            emptyList(),
            TripProfile(
                startDate = Timestamp(now.seconds - 3600, 0), // 1h ago
                endDate = Timestamp(now.seconds + 3600, 0), // 1h later
                preferredLocations = emptyList(),
                preferences = emptyList()))

    // Upcoming trip: start > now
    val upcomingTrip =
        Trip(
            "2",
            "Upcoming Trip",
            "ownerX",
            emptyList(),
            emptyList(),
            emptyList(),
            TripProfile(
                startDate = Timestamp(now.seconds + 7200, 0), // 2h later
                endDate = Timestamp(now.seconds + 10800, 0), // 3h later
                preferredLocations = emptyList(),
                preferences = emptyList()))

    coEvery { repository.getAllTrips() } returns listOf(currentTrip, upcomingTrip)
    viewModel = MyTripsViewModel(repository)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(currentTrip, state.currentTrip)
    assertEquals(listOf(upcomingTrip), state.upcomingTrips)
  }

  @Test
  fun `UI state shows only current trip`() = runTest {
    val now = Timestamp.now()

    val currentTrip =
        Trip(
            "1",
            "Current Trip",
            "ownerX",
            emptyList(),
            emptyList(),
            emptyList(),
            TripProfile(
                startDate = Timestamp(now.seconds - 3600, 0),
                endDate = Timestamp(now.seconds + 3600, 0),
                preferredLocations = emptyList(),
                preferences = emptyList()))

    coEvery { repository.getAllTrips() } returns listOf(currentTrip)
    viewModel = MyTripsViewModel(repository)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(currentTrip, state.currentTrip)
    assertEquals(emptyList<Trip>(), state.upcomingTrips)
  }

  @Test
  fun `UI state shows only upcoming trips`() = runTest {
    val now = Timestamp.now()

    val upcomingTrip =
        Trip(
            "2",
            "Upcoming Trip",
            "ownerX",
            emptyList(),
            emptyList(),
            emptyList(),
            TripProfile(
                startDate = Timestamp(now.seconds + 3600, 0), // 1h later
                endDate = Timestamp(now.seconds + 7200, 0), // 2h later
                preferredLocations = emptyList(),
                preferences = emptyList()))

    coEvery { repository.getAllTrips() } returns listOf(upcomingTrip)
    viewModel = MyTripsViewModel(repository)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(null, state.currentTrip)
    assertEquals(listOf(upcomingTrip), state.upcomingTrips)
  }

  @Test
  fun `UI state shows empty when no trips`() = runTest {
    coEvery { repository.getAllTrips() } returns emptyList()

    viewModel = MyTripsViewModel(repository)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(null, state.currentTrip)
    assertEquals(0, state.upcomingTrips.size)
  }

  @Test
  fun `UI state sets error message on exception`() = runTest {
    coEvery { repository.getAllTrips() } throws Exception("Fake network error")

    viewModel = MyTripsViewModel(repository)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals("Failed to load trips: Fake network error", state.errorMsg)
  }

  @Test
  fun `clearErrorMsg clears the error`() = runTest {
    coEvery { repository.getAllTrips() } throws Exception("Fake network error")

    viewModel = MyTripsViewModel(repository)
    testDispatcher.scheduler.advanceUntilIdle()

    assert(viewModel.uiState.value.errorMsg != null)
    viewModel.clearErrorMsg()
    val state = viewModel.uiState.value
    assertEquals(null, state.errorMsg)
  }
}
