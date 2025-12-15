package com.github.swent.swisstravel.ui.trips

import com.github.swent.swisstravel.MainDispatcherRule
import com.github.swent.swisstravel.createTestTrip
import com.github.swent.swisstravel.model.trip.*
import com.github.swent.swisstravel.model.user.User
import com.github.swent.swisstravel.model.user.UserRepository
import com.github.swent.swisstravel.model.user.UserStats
import com.google.firebase.Timestamp
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
/** Tests for [PastTripsViewModel]. */
class PastTripsViewModelTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private lateinit var tripsRepository: TripsRepository
  private lateinit var viewModel: PastTripsViewModel
  private lateinit var userRepository: UserRepository
  private lateinit var dummyUser: User

  @Before
  fun setup() {
    tripsRepository = mockk()
    userRepository = mockk()

    // Create a dummy user for mocking
    dummyUser =
        User(
            uid = "testUser",
            name = "Test User",
            biography = "",
            email = "test@example.com",
            profilePicUrl = "",
            preferences = emptyList(),
            friends = emptyList(),
            stats = UserStats(),
            pinnedTripsUids = emptyList(),
            pinnedPicturesUids = emptyList(),
            favoriteTripsUids = emptyList())

    // Mock getCurrentUser for all tests that initialize the VM (which calls getAllTrips)
    coEvery { userRepository.getCurrentUser() } returns dummyUser
    coEvery { userRepository.getUserByUid(any()) } returns null
  }

  // Helper to instantiate VM with mocks
  private fun createViewModel() {
    viewModel =
        PastTripsViewModel(userRepository = userRepository, tripsRepository = tripsRepository)
  }

  @Test
  fun uiStateUpdatesPastTrips() = runTest {
    val now = Timestamp.now()

    val pastTrip1 =
        createTestTrip(
            uid = "1",
            name = "Past Trip 1",
            startDate = Timestamp(now.seconds - 7200, 0),
            endDate = Timestamp(now.seconds - 3600, 0),
            isCurrentTrip = true)
    val pastTrip2 =
        createTestTrip(
            uid = "2",
            name = "Past Trip 2",
            startDate = Timestamp(now.seconds - 10800, 0),
            endDate = Timestamp(now.seconds - 7200, 0),
            isCurrentTrip = false)

    coEvery { tripsRepository.getAllTrips() } returns listOf(pastTrip1, pastTrip2)
    createViewModel()
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(listOf(pastTrip2, pastTrip1), state.tripsList)
  }

  @Test
  fun uiStateShowsEmptyWhenNoTrips() = runTest {
    coEvery { tripsRepository.getAllTrips() } returns emptyList()

    createViewModel()
    advanceUntilIdle()
    assertEquals(0, viewModel.uiState.value.tripsList.size)
  }

  @Test
  fun uiStateShowsErrorMessageOnException() = runTest {
    coEvery { tripsRepository.getAllTrips() } throws Exception("Fake network error")

    createViewModel()
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals("Failed to load trips.", state.errorMsg)
  }

  @Test
  fun clearErrorMsgClearsTheError() = runTest {
    coEvery { tripsRepository.getAllTrips() } throws Exception("Fake network error")

    createViewModel()
    advanceUntilIdle()
    assertEquals("Failed to load trips.", viewModel.uiState.value.errorMsg)
  }

  @Test
  fun toggleSelectionModeEnablesAndDisablesSelectionCorrectly() = runTest {
    coEvery { tripsRepository.getAllTrips() } returns emptyList()
    createViewModel()
    advanceUntilIdle()

    val trip1 = createTestTrip(uid = "1")

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
    coEvery { tripsRepository.getAllTrips() } returns emptyList()
    createViewModel()
    advanceUntilIdle()

    val trip1 = createTestTrip("1")
    val trip2 = createTestTrip("2")

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
    coEvery { tripsRepository.getAllTrips() } returns emptyList()
    createViewModel()
    advanceUntilIdle()

    val trip =
        Trip(
            "1",
            "Trip 1",
            "user",
            emptyList(),
            emptyList(),
            emptyList(),
            TripProfile(Timestamp.now(), Timestamp.now(), emptyList(), emptyList()),
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
    val trip1 = createTestTrip("1")
    val trip2 = createTestTrip("2")

    coEvery { tripsRepository.getAllTrips() } returns listOf(trip1, trip2)
    coEvery { tripsRepository.deleteTrip(any()) } returns Unit

    createViewModel()
    advanceUntilIdle()

    viewModel.toggleSelectionMode(true)
    viewModel.toggleTripSelection(trip1)
    viewModel.toggleTripSelection(trip2)

    viewModel.deleteSelectedTrips()
    advanceUntilIdle()

    assertEquals(false, viewModel.uiState.value.isSelectionMode)
    assertEquals(emptySet<Trip>(), viewModel.uiState.value.selectedTrips)
  }

  @Test
  fun selectAllTripsSelectsAllPastTrips() = runTest {
    val now = Timestamp.now()
    val pastTrip1 = createTestTrip("1", startDate = Timestamp(now.seconds - 2000, 0))
    val pastTrip2 = createTestTrip("2", startDate = Timestamp(now.seconds - 3000, 0))

    coEvery { tripsRepository.getAllTrips() } returns listOf(pastTrip1, pastTrip2)
    createViewModel()
    advanceUntilIdle()

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
            isCurrentTrip = false,
            uriLocation = emptyMap(),
            collaboratorsId = emptyList())

    coEvery { tripsRepository.getAllTrips() } returns listOf(trip1)
    coEvery { tripsRepository.deleteTrip(any()) } throws Exception("DB failure")

    createViewModel()
    advanceUntilIdle()

    viewModel.toggleSelectionMode(true)
    viewModel.toggleTripSelection(trip1)
    viewModel.deleteSelectedTrips()
    advanceUntilIdle()

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
            isCurrentTrip = false,
            uriLocation = emptyMap(),
            collaboratorsId = emptyList()))
  }

  @Test
  fun sortSTART_DATE_ASC() = runTest {
    val trips = createTrips()
    coEvery { tripsRepository.getAllTrips() } returns trips

    createViewModel()
    advanceUntilIdle()

    viewModel.updateSortType(TripSortType.START_DATE_ASC)
    advanceUntilIdle()

    val sorted = viewModel.uiState.value.tripsList
    assertEquals(listOf(trips[1], trips[2], trips[0]), sorted)
  }

  @Test
  fun sortSTART_DATE_DESC() = runTest {
    val trips = createTrips()
    coEvery { tripsRepository.getAllTrips() } returns trips

    createViewModel()
    advanceUntilIdle()

    viewModel.updateSortType(TripSortType.START_DATE_DESC)
    advanceUntilIdle()

    val sorted = viewModel.uiState.value.tripsList
    assertEquals(listOf(trips[0], trips[2], trips[1]), sorted)
  }

  @Test
  fun sortEND_DATE_ASC() = runTest {
    val trips = createTrips()
    coEvery { tripsRepository.getAllTrips() } returns trips

    createViewModel()
    advanceUntilIdle()

    viewModel.updateSortType(TripSortType.END_DATE_ASC)
    advanceUntilIdle()

    val sorted = viewModel.uiState.value.tripsList
    assertEquals(listOf(trips[1], trips[2], trips[0]), sorted)
  }

  @Test
  fun sortEND_DATE_DESC() = runTest {
    val trips = createTrips()
    coEvery { tripsRepository.getAllTrips() } returns trips

    createViewModel()
    advanceUntilIdle()

    viewModel.updateSortType(TripSortType.END_DATE_DESC)
    advanceUntilIdle()

    val sorted = viewModel.uiState.value.tripsList
    assertEquals(listOf(trips[0], trips[2], trips[1]), sorted)
  }

  @Test
  fun sortNAME_ASC() = runTest {
    val trips = createTrips()
    coEvery { tripsRepository.getAllTrips() } returns trips

    createViewModel()
    advanceUntilIdle()

    viewModel.updateSortType(TripSortType.NAME_ASC)
    advanceUntilIdle()

    val sorted = viewModel.uiState.value.tripsList
    assertEquals(listOf(trips[0], trips[1], trips[2]), sorted) // Alpha, Beta, Gamma
  }

  @Test
  fun sortNAME_DESC() = runTest {
    val trips = createTrips()
    coEvery { tripsRepository.getAllTrips() } returns trips

    createViewModel()
    advanceUntilIdle()

    viewModel.updateSortType(TripSortType.NAME_DESC)
    advanceUntilIdle()

    val sorted = viewModel.uiState.value.tripsList
    assertEquals(listOf(trips[2], trips[1], trips[0]), sorted) // Gamma, Beta, Alpha
  }

  @Test
  fun toggleFavoriteForSelectedTripsTogglesFavoritesCorrectly() = runTest {
    val trip = createTestTrip("1", isFavorite = false)
    coEvery { tripsRepository.getAllTrips() } returns listOf(trip)
    coEvery { tripsRepository.editTrip(any(), any()) } returns Unit

    createViewModel()
    advanceUntilIdle()

    viewModel.toggleSelectionMode(true)
    viewModel.toggleTripSelection(trip)
    viewModel.toggleFavoriteForSelectedTrips()
    advanceUntilIdle()

    // Verify interaction with userRepository instead of tripsRepository
    coVerify { userRepository.addFavoriteTrip(dummyUser.uid, trip.uid) }
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
            isCurrentTrip = false,
            uriLocation = emptyMap(),
            collaboratorsId = emptyList())

    coEvery { tripsRepository.getAllTrips() } returns listOf(trip)

    // Simulate failure on userRepository
    coEvery { userRepository.addFavoriteTrip(any(), any()) } throws
        Exception("Firestore edit failed")

    createViewModel()
    viewModel.toggleSelectionMode(true) // Needs to be in selection mode
    viewModel.toggleTripSelection(trip)
    advanceUntilIdle()

    // Act — trigger the failure
    viewModel.toggleFavoriteForSelectedTrips()
    advanceUntilIdle()

    // Assert — verify error message set by catch block
    val state = viewModel.uiState.value
    assertEquals("Failed to update favorites.", state.errorMsg)
  }
}
