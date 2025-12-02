package com.github.swent.swisstravel.ui.trips

import com.github.swent.swisstravel.model.trip.*
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
                preferences = emptyList()),
            isFavorite = false,
            isCurrentTrip = true,
            listUri = emptyList())

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
                preferences = emptyList()),
            isFavorite = false,
            isCurrentTrip = false,
            listUri = emptyList())

    coEvery { repository.getAllTrips() } returns listOf(currentTrip, upcomingTrip)
    viewModel = MyTripsViewModel(repository)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(currentTrip, state.currentTrip)
    assertEquals(listOf(upcomingTrip), state.tripsList)
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
                preferences = emptyList()),
            isFavorite = false,
            isCurrentTrip = true,
            listUri = emptyList())

    coEvery { repository.getAllTrips() } returns listOf(currentTrip)
    viewModel = MyTripsViewModel(repository)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(currentTrip, state.currentTrip)
    assertEquals(emptyList<Trip>(), state.tripsList)
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
                preferences = emptyList()),
            isFavorite = false,
            isCurrentTrip = false,
            listUri = emptyList())

    coEvery { repository.getAllTrips() } returns listOf(upcomingTrip)
    viewModel = MyTripsViewModel(repository)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(null, state.currentTrip)
    assertEquals(listOf(upcomingTrip), state.tripsList)
  }

  @Test
  fun `UI state shows empty when no trips`() = runTest {
    coEvery { repository.getAllTrips() } returns emptyList()

    viewModel = MyTripsViewModel(repository)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(null, state.currentTrip)
    assertEquals(0, state.tripsList.size)
  }

  @Test
  fun `UI state shows error message on exception`() = runTest {
    coEvery { repository.getAllTrips() } throws Exception("Fake network error")

    viewModel = MyTripsViewModel(repository)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals("Failed to load trips.", state.errorMsg)
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

  @Test
  fun `toggleSelectionMode enables and disables selection correctly`() = runTest {
    coEvery { repository.getAllTrips() } returns emptyList()
    viewModel = MyTripsViewModel(repository)
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
            listUri = emptyList())

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
  fun `toggleTripSelection adds and removes trips from selection`() = runTest {
    coEvery { repository.getAllTrips() } returns emptyList()
    viewModel = MyTripsViewModel(repository)
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
            listUri = emptyList())

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
            listUri = emptyList())

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
  fun `toggleTripSelection disables selection mode when last trip unselected`() = runTest {
    coEvery { repository.getAllTrips() } returns emptyList()
    viewModel = MyTripsViewModel(repository)
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
            listUri = emptyList())

    viewModel.toggleSelectionMode(true)
    viewModel.toggleTripSelection(trip)
    assertEquals(true, viewModel.uiState.value.isSelectionMode)

    // Unselect last trip
    viewModel.toggleTripSelection(trip)
    assertEquals(false, viewModel.uiState.value.isSelectionMode)
  }

  @Test
  fun `deleteSelectedTrips removes selected trips from repository`() = runTest {
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
            listUri = emptyList())

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
            listUri = emptyList())

    coEvery { repository.getAllTrips() } returns listOf(trip1, trip2)
    coEvery { repository.deleteTrip(any()) } returns Unit

    viewModel = MyTripsViewModel(repository)
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
  fun `selectAllTrips selects all current and upcoming trips`() = runTest {
    val now = Timestamp.now()

    val currentTrip =
        Trip(
            "1",
            "Current Trip",
            "user",
            emptyList(),
            emptyList(),
            emptyList(),
            TripProfile(
                startDate = Timestamp(now.seconds - 1000, 0),
                endDate = Timestamp(now.seconds + 1000, 0),
                preferredLocations = emptyList(),
                preferences = emptyList()),
            isFavorite = false,
            isCurrentTrip = false,
            listUri = emptyList())

    val upcomingTrip =
        Trip(
            "2",
            "Upcoming Trip",
            "user",
            emptyList(),
            emptyList(),
            emptyList(),
            TripProfile(
                startDate = Timestamp(now.seconds + 2000, 0),
                endDate = Timestamp(now.seconds + 3000, 0),
                preferredLocations = emptyList(),
                preferences = emptyList()),
            isFavorite = false,
            isCurrentTrip = false,
            listUri = emptyList())

    coEvery { repository.getAllTrips() } returns listOf(currentTrip, upcomingTrip)
    viewModel = MyTripsViewModel(repository)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.selectAllTrips()

    val selected = viewModel.uiState.value.selectedTrips
    assertEquals(setOf(currentTrip, upcomingTrip), selected)
  }

  @Test
  fun `deleteSelectedTrips sets error message on failure`() = runTest {
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
            listUri = emptyList())

    coEvery { repository.getAllTrips() } returns listOf(trip1)
    coEvery { repository.deleteTrip(any()) } throws Exception("DB failure")

    viewModel = MyTripsViewModel(repository)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.toggleSelectionMode(true)
    viewModel.toggleTripSelection(trip1)
    viewModel.deleteSelectedTrips()
    testDispatcher.scheduler.advanceUntilIdle()

    assert(viewModel.uiState.value.errorMsg?.contains("Failed to delete trips") == true)
  }

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
                startDate = Timestamp(now.seconds + 1000, 0),
                endDate = Timestamp(now.seconds + 2000, 0),
                preferredLocations = emptyList(),
                preferences = emptyList()),
            isFavorite = false,
            isCurrentTrip = false,
            listUri = emptyList()),
        Trip(
            "2",
            "Beta",
            "owner",
            emptyList(),
            emptyList(),
            emptyList(),
            TripProfile(
                startDate = Timestamp(now.seconds + 2000, 0),
                endDate = Timestamp(now.seconds + 3000, 0),
                preferredLocations = emptyList(),
                preferences = emptyList()),
            isFavorite = false,
            isCurrentTrip = false,
            listUri = emptyList()),
        Trip(
            "3",
            "Gamma",
            "owner",
            emptyList(),
            emptyList(),
            emptyList(),
            TripProfile(
                startDate = Timestamp(now.seconds + 1500, 0),
                endDate = Timestamp(now.seconds + 2500, 0),
                preferredLocations = emptyList(),
                preferences = emptyList()),
            isFavorite = false,
            isCurrentTrip = false,
            listUri = emptyList()))
  }

  @Test
  fun `sort START_DATE_ASC`() = runTest {
    val trips = createTrips()
    coEvery { repository.getAllTrips() } returns trips

    viewModel = MyTripsViewModel(repository)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.updateSortType(TripSortType.START_DATE_ASC)
    testDispatcher.scheduler.advanceUntilIdle()

    val sorted = viewModel.uiState.value.tripsList
    assertEquals(listOf(trips[0], trips[2], trips[1]), sorted)
  }

  @Test
  fun `sort START_DATE_DESC`() = runTest {
    val trips = createTrips()
    coEvery { repository.getAllTrips() } returns trips

    viewModel = MyTripsViewModel(repository)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.updateSortType(TripSortType.START_DATE_DESC)
    testDispatcher.scheduler.advanceUntilIdle()

    val sorted = viewModel.uiState.value.tripsList
    assertEquals(listOf(trips[1], trips[2], trips[0]), sorted)
  }

  @Test
  fun `sort END_DATE_ASC`() = runTest {
    val trips = createTrips()
    coEvery { repository.getAllTrips() } returns trips

    viewModel = MyTripsViewModel(repository)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.updateSortType(TripSortType.END_DATE_ASC)
    testDispatcher.scheduler.advanceUntilIdle()

    val sorted = viewModel.uiState.value.tripsList
    assertEquals(listOf(trips[0], trips[2], trips[1]), sorted)
  }

  @Test
  fun `sort END_DATE_DESC`() = runTest {
    val trips = createTrips()
    coEvery { repository.getAllTrips() } returns trips

    viewModel = MyTripsViewModel(repository)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.updateSortType(TripSortType.END_DATE_DESC)
    testDispatcher.scheduler.advanceUntilIdle()

    val sorted = viewModel.uiState.value.tripsList
    assertEquals(listOf(trips[1], trips[2], trips[0]), sorted)
  }

  @Test
  fun `sort NAME_ASC`() = runTest {
    val trips = createTrips()
    coEvery { repository.getAllTrips() } returns trips

    viewModel = MyTripsViewModel(repository)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.updateSortType(TripSortType.NAME_ASC)
    testDispatcher.scheduler.advanceUntilIdle()

    val sorted = viewModel.uiState.value.tripsList
    assertEquals(listOf(trips[0], trips[1], trips[2]), sorted) // Alpha, Beta, Gamma
  }

  @Test
  fun `sort NAME_DESC`() = runTest {
    val trips = createTrips()
    coEvery { repository.getAllTrips() } returns trips

    viewModel = MyTripsViewModel(repository)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.updateSortType(TripSortType.NAME_DESC)
    testDispatcher.scheduler.advanceUntilIdle()

    val sorted = viewModel.uiState.value.tripsList
    assertEquals(listOf(trips[2], trips[1], trips[0]), sorted) // Gamma, Beta, Alpha
  }

  @Test
  fun `toggleFavoriteForSelectedTrips toggles favorites correctly`() = runTest {
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
            listUri = emptyList())

    coEvery { repository.getAllTrips() } returns listOf(trip)
    coEvery { repository.editTrip(any(), any()) } returns Unit

    viewModel = MyTripsViewModel(repository)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.toggleSelectionMode(true)
    viewModel.toggleTripSelection(trip)
    viewModel.toggleFavoriteForSelectedTrips()
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify { repository.editTrip(trip.uid, trip.copy(isFavorite = true)) }
  }

  // chatGPT
  @Test
  fun `changeCurrentTrip sets error message on failure`() = runTest {
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
            listUri = emptyList())

    // Simulate normal getAllTrips but fail when editing trip
    coEvery { repository.getAllTrips() } returns listOf(trip)
    coEvery { repository.editTrip(any(), any()) } throws Exception("Firestore edit failed")

    viewModel = MyTripsViewModel(repository)
    testDispatcher.scheduler.advanceUntilIdle()

    // Act — trigger the failure
    viewModel.changeCurrentTrip(trip)
    testDispatcher.scheduler.advanceUntilIdle()

    // Assert — verify error message set by catch block
    val state = viewModel.uiState.value
    assertEquals("Failed to change current trip.", state.errorMsg)
  }

  @Test
  fun `toggleFavoriteForSelectedTrips sets error message on failure`() = runTest {
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
            listUri = emptyList())

    coEvery { repository.editTrip(any(), any()) } throws Exception("Firestore edit failed")

    viewModel = MyTripsViewModel(repository)
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
