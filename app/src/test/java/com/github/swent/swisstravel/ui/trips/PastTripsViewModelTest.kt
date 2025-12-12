package com.github.swent.swisstravel.ui.trips

import com.github.swent.swisstravel.model.trip.*
import com.github.swent.swisstravel.model.user.UserRepository
import com.google.firebase.Timestamp
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
/** Tests for [PastTripsViewModel]. */
class PastTripsViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  private lateinit var repository: TripsRepository
  private lateinit var viewModel: PastTripsViewModel
  private lateinit var userRepository: UserRepository

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    repository = mockk()
    userRepository = mockk()
    coEvery { userRepository.getUserByUid(any()) } returns null
  }

  @After
  fun teardown() {
    Dispatchers.resetMain()
  }

  // Helper to instantiate VM with mocks
  private fun createViewModel() {
    viewModel = PastTripsViewModel(userRepository = userRepository, tripsRepository = repository)
  }

  @Test
  fun uiStateUpdatesPastTrips() = runTest {
    val now = Timestamp.now()

    val pastTrip1 =
        Trip(
            "1",
            "Past Trip 1",
            "ownerX",
            emptyList(),
            emptyList(),
            emptyList(),
            TripProfile(
                startDate = Timestamp(now.seconds - 7200, 0), // 2h ago
                endDate = Timestamp(now.seconds - 3600, 0), // 1h ago
                preferredLocations = emptyList(),
                preferences = emptyList()),
            isFavorite = false,
            isCurrentTrip = true,
            uriLocation = emptyMap(),
            collaboratorsId = emptyList())

    // Upcoming trip: start > now
    val pastTrip2 =
        Trip(
            "2",
            "Past Trip 2",
            "ownerX",
            emptyList(),
            emptyList(),
            emptyList(),
            TripProfile(
                startDate = Timestamp(now.seconds - 10800, 0), // 3h ago
                endDate = Timestamp(now.seconds - 7200, 0), // 2h ago
                preferredLocations = emptyList(),
                preferences = emptyList()),
            isFavorite = false,
            isCurrentTrip = false,
            uriLocation = emptyMap(),
            collaboratorsId = emptyList())

    coEvery { repository.getAllTrips() } returns listOf(pastTrip1, pastTrip2)
    createViewModel()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(listOf(pastTrip2, pastTrip1), state.tripsList)
  }

  @Test
  fun uiStateShowsEmptyWhenNoTrips() = runTest {
    coEvery { repository.getAllTrips() } returns emptyList()

    createViewModel()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(null, state.currentTrip)
    assertEquals(0, state.tripsList.size)
  }

  @Test
  fun uiStateShowsErrorMessageOnException() = runTest {
    coEvery { repository.getAllTrips() } throws Exception("Fake network error")

    createViewModel()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals("Failed to load trips.", state.errorMsg)
  }

  @Test
  fun clearErrorMsgClearsTheError() = runTest {
    coEvery { repository.getAllTrips() } throws Exception("Fake network error")

    createViewModel()
    testDispatcher.scheduler.advanceUntilIdle()

    assert(viewModel.uiState.value.errorMsg != null)
    viewModel.clearErrorMsg()
    val state = viewModel.uiState.value
    assertEquals(null, state.errorMsg)
  }

  @Test
  fun toggleSelectionModeEnablesAndDisablesSelectionCorrectly() = runTest {
    coEvery { repository.getAllTrips() } returns emptyList()
    createViewModel()
    testDispatcher.scheduler.advanceUntilIdle()

    val trip1 =
        Trip(
            "1",
            "Trip 1",
            "user",
            emptyList(),
            emptyList(),
            emptyList(),
            TripProfile(Timestamp.now(), Timestamp.now(), emptyList(), emptyList()),
            isFavorite = false,
            isCurrentTrip = false,
            uriLocation = emptyMap(),
            collaboratorsId = emptyList())

    // Initially disabled
    assertEquals(false, viewModel.uiState.value.isSelectionMode)

    // Enable selection
    viewModel.toggleSelectionMode(true)
    assertEquals(true, viewModel.uiState.value.isSelectionMode)

    // Disable selection clears selected trips
    viewModel.toggleTripSelection(trip1)
    viewModel.toggleSelectionMode(false)
    assertEquals(false, viewModel.uiState.value.isSelectionMode)
    assertEquals(emptySet<Trip>(), viewModel.uiState.value.selectedTrips)
  }

  @Test
  fun toggleTripSelectionAddsAndRemovesTripsFromSelection() = runTest {
    coEvery { repository.getAllTrips() } returns emptyList()
    createViewModel()
    testDispatcher.scheduler.advanceUntilIdle()

    val trip1 =
        Trip(
            "1",
            "Trip 1",
            "user",
            emptyList(),
            emptyList(),
            emptyList(),
            TripProfile(Timestamp.now(), Timestamp.now(), emptyList(), emptyList()),
            isFavorite = false,
            isCurrentTrip = false,
            uriLocation = emptyMap(),
            collaboratorsId = emptyList())

    val trip2 =
        Trip(
            "2",
            "Trip 2",
            "user",
            emptyList(),
            emptyList(),
            emptyList(),
            TripProfile(Timestamp.now(), Timestamp.now(), emptyList(), emptyList()),
            isFavorite = false,
            isCurrentTrip = false,
            uriLocation = emptyMap(),
            collaboratorsId = emptyList())

    viewModel.toggleSelectionMode(true)

    // Add first trip
    viewModel.toggleTripSelection(trip1)
    assertEquals(setOf(trip1), viewModel.uiState.value.selectedTrips)

    // Add second trip
    viewModel.toggleTripSelection(trip2)
    assertEquals(setOf(trip1, trip2), viewModel.uiState.value.selectedTrips)

    // Remove one trip
    viewModel.toggleTripSelection(trip1)
    assertEquals(setOf(trip2), viewModel.uiState.value.selectedTrips)
  }

  @Test
  fun toggleTripSelectionDisablesSelectionModeWhenLastTripUnselected() = runTest {
    coEvery { repository.getAllTrips() } returns emptyList()
    createViewModel()
    testDispatcher.scheduler.advanceUntilIdle()

    val trip =
        Trip(
            "1",
            "Trip 1",
            "user",
            emptyList(),
            emptyList(),
            emptyList(),
            TripProfile(Timestamp.now(), Timestamp.now(), emptyList(), emptyList()),
            isFavorite = false,
            isCurrentTrip = false,
            uriLocation = emptyMap(),
            collaboratorsId = emptyList())

    viewModel.toggleSelectionMode(true)
    viewModel.toggleTripSelection(trip)
    assertEquals(true, viewModel.uiState.value.isSelectionMode)

    // Unselect last trip
    viewModel.toggleTripSelection(trip)
    assertEquals(false, viewModel.uiState.value.isSelectionMode)
  }

  @Test
  fun deleteSelectedTripsRemovesSelectedTripsFromRepository() = runTest {
    val trip1 =
        Trip(
            "1",
            "Trip 1",
            "user",
            emptyList(),
            emptyList(),
            emptyList(),
            TripProfile(Timestamp.now(), Timestamp.now(), emptyList(), emptyList()),
            isFavorite = false,
            isCurrentTrip = false,
            uriLocation = emptyMap(),
            collaboratorsId = emptyList())

    val trip2 =
        Trip(
            "2",
            "Trip 2",
            "user",
            emptyList(),
            emptyList(),
            emptyList(),
            TripProfile(Timestamp.now(), Timestamp.now(), emptyList(), emptyList()),
            isFavorite = false,
            isCurrentTrip = false,
            uriLocation = emptyMap(),
            collaboratorsId = emptyList())

    coEvery { repository.getAllTrips() } returns listOf(trip1, trip2)
    coEvery { repository.deleteTrip(any()) } returns Unit

    createViewModel()
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.toggleSelectionMode(true)
    viewModel.toggleTripSelection(trip1)
    viewModel.toggleTripSelection(trip2)

    viewModel.deleteSelectedTrips()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(false, state.isSelectionMode)
    assertEquals(emptySet<Trip>(), state.selectedTrips)
  }

  @Test
  fun selectAllTripsSelectsAllPastTrips() = runTest {
    val now = Timestamp.now()

    val pastTrip1 =
        Trip(
            "1",
            "Current Trip",
            "user",
            emptyList(),
            emptyList(),
            emptyList(),
            TripProfile(
                startDate = Timestamp(now.seconds - 2000, 0),
                endDate = Timestamp(now.seconds - 1000, 0),
                preferredLocations = emptyList(),
                preferences = emptyList()),
            isFavorite = false,
            isCurrentTrip = false,
            uriLocation = emptyMap(),
            collaboratorsId = emptyList())

    val pastTrip2 =
        Trip(
            "2",
            "Upcoming Trip",
            "user",
            emptyList(),
            emptyList(),
            emptyList(),
            TripProfile(
                startDate = Timestamp(now.seconds - 3000, 0),
                endDate = Timestamp(now.seconds - 2000, 0),
                preferredLocations = emptyList(),
                preferences = emptyList()),
            isFavorite = false,
            isCurrentTrip = false,
            uriLocation = emptyMap(),
            collaboratorsId = emptyList())

    coEvery { repository.getAllTrips() } returns listOf(pastTrip1, pastTrip2)
    createViewModel()
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.selectAllTrips()

    val selected = viewModel.uiState.value.selectedTrips
    assertEquals(setOf(pastTrip1, pastTrip2), selected)
  }

  @Test
  fun deleteSelectedTripsSetsErrorMessageOnFailure() = runTest {
    val trip1 =
        Trip(
            "1",
            "Trip 1",
            "user",
            emptyList(),
            emptyList(),
            emptyList(),
            TripProfile(Timestamp.now(), Timestamp.now(), emptyList(), emptyList()),
            isFavorite = false,
            isCurrentTrip = false,
            uriLocation = emptyMap(),
            collaboratorsId = emptyList())

    coEvery { repository.getAllTrips() } returns listOf(trip1)
    coEvery { repository.deleteTrip(any()) } throws Exception("DB failure")

    createViewModel()
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.toggleSelectionMode(true)
    viewModel.toggleTripSelection(trip1)
    viewModel.deleteSelectedTrips()
    testDispatcher.scheduler.advanceUntilIdle()

    assert(viewModel.uiState.value.errorMsg?.contains("Failed to delete trips") == true)
  }

  /** Creates a list of trips for testing. */
  private fun createTrips(): List<Trip> {
    val now = Timestamp.now()
    return listOf(
        Trip(
            "1",
            "Alpha",
            "owner",
            emptyList(),
            emptyList(),
            emptyList(),
            TripProfile(
                startDate = Timestamp(now.seconds - 2000, 0),
                endDate = Timestamp(now.seconds - 1000, 0),
                preferredLocations = emptyList(),
                preferences = emptyList()),
            isFavorite = false,
            isCurrentTrip = false,
            uriLocation = emptyMap(),
            collaboratorsId = emptyList()),
        Trip(
            "2",
            "Beta",
            "owner",
            emptyList(),
            emptyList(),
            emptyList(),
            TripProfile(
                startDate = Timestamp(now.seconds - 3000, 0),
                endDate = Timestamp(now.seconds - 2000, 0),
                preferredLocations = emptyList(),
                preferences = emptyList()),
            isFavorite = false,
            isCurrentTrip = false,
            uriLocation = emptyMap(),
            collaboratorsId = emptyList()),
        Trip(
            "3",
            "Gamma",
            "owner",
            emptyList(),
            emptyList(),
            emptyList(),
            TripProfile(
                startDate = Timestamp(now.seconds - 2500, 0),
                endDate = Timestamp(now.seconds - 1500, 0),
                preferredLocations = emptyList(),
                preferences = emptyList()),
            isFavorite = false,
            isCurrentTrip = false,
            uriLocation = emptyMap(),
            collaboratorsId = emptyList()))
  }

  @Test
  fun sortSTART_DATE_ASC() = runTest {
    val trips = createTrips()
    coEvery { repository.getAllTrips() } returns trips

    createViewModel()
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.updateSortType(TripSortType.START_DATE_ASC)
    testDispatcher.scheduler.advanceUntilIdle()

    val sorted = viewModel.uiState.value.tripsList
    assertEquals(listOf(trips[1], trips[2], trips[0]), sorted)
  }

  @Test
  fun sortSTART_DATE_DESC() = runTest {
    val trips = createTrips()
    coEvery { repository.getAllTrips() } returns trips

    createViewModel()
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.updateSortType(TripSortType.START_DATE_DESC)
    testDispatcher.scheduler.advanceUntilIdle()

    val sorted = viewModel.uiState.value.tripsList
    assertEquals(listOf(trips[0], trips[2], trips[1]), sorted)
  }

  @Test
  fun sortEND_DATE_ASC() = runTest {
    val trips = createTrips()
    coEvery { repository.getAllTrips() } returns trips

    createViewModel()
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.updateSortType(TripSortType.END_DATE_ASC)
    testDispatcher.scheduler.advanceUntilIdle()

    val sorted = viewModel.uiState.value.tripsList
    assertEquals(listOf(trips[1], trips[2], trips[0]), sorted)
  }

  @Test
  fun sortEND_DATE_DESC() = runTest {
    val trips = createTrips()
    coEvery { repository.getAllTrips() } returns trips

    createViewModel()
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.updateSortType(TripSortType.END_DATE_DESC)
    testDispatcher.scheduler.advanceUntilIdle()

    val sorted = viewModel.uiState.value.tripsList
    assertEquals(listOf(trips[0], trips[2], trips[1]), sorted)
  }

  @Test
  fun sortNAME_ASC() = runTest {
    val trips = createTrips()
    coEvery { repository.getAllTrips() } returns trips

    createViewModel()
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.updateSortType(TripSortType.NAME_ASC)
    testDispatcher.scheduler.advanceUntilIdle()

    val sorted = viewModel.uiState.value.tripsList
    assertEquals(listOf(trips[0], trips[1], trips[2]), sorted) // Alpha, Beta, Gamma
  }

  @Test
  fun sortNAME_DESC() = runTest {
    val trips = createTrips()
    coEvery { repository.getAllTrips() } returns trips

    createViewModel()
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.updateSortType(TripSortType.NAME_DESC)
    testDispatcher.scheduler.advanceUntilIdle()

    val sorted = viewModel.uiState.value.tripsList
    assertEquals(listOf(trips[2], trips[1], trips[0]), sorted) // Gamma, Beta, Alpha
  }

  @Test
  fun toggleFavoriteForSelectedTripsTogglesFavoritesCorrectly() = runTest {
    val trip =
        Trip(
            "1",
            "Trip",
            "owner",
            emptyList(),
            emptyList(),
            emptyList(),
            TripProfile(Timestamp.now(), Timestamp.now(), emptyList(), emptyList()),
            isFavorite = false,
            isCurrentTrip = false,
            emptyList(),
            uriLocation = emptyMap())

    coEvery { repository.getAllTrips() } returns listOf(trip)
    coEvery { repository.editTrip(any(), any()) } returns Unit

    createViewModel()
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.toggleSelectionMode(true)
    viewModel.toggleTripSelection(trip)
    viewModel.toggleFavoriteForSelectedTrips()
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify { repository.editTrip(trip.uid, trip.copy(isFavorite = true)) }
  }

  @Test
  fun toggleFavoriteForSelectedTripsSetsErrorMessageOnFailure() = runTest {
    val trip =
        Trip(
            "1",
            "Fail Trip",
            "owner",
            emptyList(),
            emptyList(),
            emptyList(),
            TripProfile(Timestamp.now(), Timestamp.now(), emptyList(), emptyList()),
            isFavorite = false,
            isCurrentTrip = false,
            uriLocation = emptyMap(),
            collaboratorsId = emptyList())

    coEvery { repository.editTrip(any(), any()) } throws Exception("Firestore edit failed")

    createViewModel()
    viewModel.toggleTripSelection(trip)
    testDispatcher.scheduler.advanceUntilIdle()

    // Act — trigger the failure
    viewModel.toggleFavoriteForSelectedTrips()
    testDispatcher.scheduler.advanceUntilIdle()

    // Assert — verify error message set by catch block
    val state = viewModel.uiState.value
    assertEquals("Failed to update favorites.", state.errorMsg)
  }
}
