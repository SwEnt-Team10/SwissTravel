package com.github.swent.swisstravel.ui.trips

import com.github.swent.swisstravel.MainDispatcherRule
import com.github.swent.swisstravel.createTestTrip
import com.github.swent.swisstravel.createTestUser
import com.github.swent.swisstravel.model.trip.*
import com.github.swent.swisstravel.model.user.UserRepository
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
class MyTripsViewModelTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var repository: TripsRepository
  private lateinit var userRepository: UserRepository
  private lateinit var viewModel: MyTripsViewModel

  @Before
  fun setup() {
    repository = mockk()
    userRepository = mockk()
    coEvery { userRepository.getUserByUid(any()) } returns null
  }

  @Test
  fun `UI state updates with current and upcoming trips`() = runTest {
    val now = Timestamp.now()

    val currentTrip =
        createTestTrip(
            uid = "1",
            name = "Current Trip",
            startDate = Timestamp(now.seconds - 3600, 0),
            endDate = Timestamp(now.seconds + 3600, 0),
            isCurrentTrip = true)

    // Upcoming trip: start > now
    val upcomingTrip =
        createTestTrip(
            uid = "2",
            name = "Upcoming Trip",
            startDate = Timestamp(now.seconds + 7200, 0),
            endDate = Timestamp(now.seconds + 10800, 0),
            isCurrentTrip = false)

    coEvery { repository.getAllTrips() } returns listOf(currentTrip, upcomingTrip)
    viewModel = MyTripsViewModel(userRepository = userRepository, tripsRepository = repository)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(currentTrip, state.currentTrip)
    assertEquals(listOf(upcomingTrip), state.tripsList)
  }

  @Test
  fun `UI state shows only current trip`() = runTest {
    val now = Timestamp.now()

    val currentTrip =
        createTestTrip(
            uid = "1",
            name = "Current Trip",
            startDate = Timestamp(now.seconds - 3600, 0),
            endDate = Timestamp(now.seconds + 3600, 0),
            isCurrentTrip = true)

    coEvery { repository.getAllTrips() } returns listOf(currentTrip)
    viewModel = MyTripsViewModel(userRepository = userRepository, tripsRepository = repository)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(currentTrip, state.currentTrip)
    assertEquals(emptyList<Trip>(), state.tripsList)
  }

  @Test
  fun `UI state shows only upcoming trips`() = runTest {
    val now = Timestamp.now()

    val upcomingTrip =
        createTestTrip(
            uid = "2",
            name = "Upcoming Trip",
            startDate = Timestamp(now.seconds + 3600, 0),
            endDate = Timestamp(now.seconds + 7200, 0),
            isCurrentTrip = false)

    coEvery { repository.getAllTrips() } returns listOf(upcomingTrip)
    viewModel = MyTripsViewModel(userRepository = userRepository, tripsRepository = repository)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(null, state.currentTrip)
    assertEquals(listOf(upcomingTrip), state.tripsList)
  }

  @Test
  fun `UI state shows empty when no trips`() = runTest {
    coEvery { repository.getAllTrips() } returns emptyList()

    viewModel = MyTripsViewModel(userRepository = userRepository, tripsRepository = repository)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(null, state.currentTrip)
    assertEquals(0, state.tripsList.size)
  }

  @Test
  fun `UI state shows error message on exception`() = runTest {
    coEvery { repository.getAllTrips() } throws Exception("Fake network error")

    viewModel = MyTripsViewModel(userRepository = userRepository, tripsRepository = repository)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals("Failed to load trips.", state.errorMsg)
  }

  @Test
  fun `clearErrorMsg clears the error`() = runTest {
    coEvery { repository.getAllTrips() } throws Exception("Fake network error")

    viewModel = MyTripsViewModel(userRepository = userRepository, tripsRepository = repository)
    advanceUntilIdle()

    assert(viewModel.uiState.value.errorMsg != null)
    viewModel.clearErrorMsg()
    val state = viewModel.uiState.value
    assertEquals(null, state.errorMsg)
  }

  @Test
  fun `toggleSelectionMode enables and disables selection correctly`() = runTest {
    coEvery { repository.getAllTrips() } returns emptyList()
    viewModel = MyTripsViewModel(userRepository = userRepository, tripsRepository = repository)
    advanceUntilIdle()

    val trip1 = createTestTrip(uid = "1", name = "Trip 1")

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
    viewModel = MyTripsViewModel(userRepository = userRepository, tripsRepository = repository)
    advanceUntilIdle()

    val trip1 = createTestTrip(uid = "1", name = "Trip 1")
    val trip2 = createTestTrip(uid = "2", name = "Trip 2")

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
    viewModel = MyTripsViewModel(userRepository = userRepository, tripsRepository = repository)
    advanceUntilIdle()

    val trip = createTestTrip(uid = "1", name = "Trip 1")

    viewModel.toggleSelectionMode(true)
    viewModel.toggleTripSelection(trip)
    assertEquals(true, viewModel.uiState.value.isSelectionMode)

    // Unselect last trip
    viewModel.toggleTripSelection(trip)
    assertEquals(false, viewModel.uiState.value.isSelectionMode)
  }

  @Test
  fun `deleteSelectedTrips removes selected trips from repository`() = runTest {
    val trip1 = createTestTrip(uid = "1", name = "Trip 1")
    val trip2 = createTestTrip(uid = "2", name = "Trip 2")

    coEvery { repository.getAllTrips() } returns listOf(trip1, trip2)
    coEvery { repository.deleteTrip(any()) } returns Unit

    viewModel = MyTripsViewModel(userRepository = userRepository, tripsRepository = repository)
    advanceUntilIdle()

    viewModel.toggleSelectionMode(true)
    viewModel.toggleTripSelection(trip1)
    viewModel.toggleTripSelection(trip2)

    viewModel.deleteSelectedTrips()
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(false, state.isSelectionMode)
    assertEquals(emptySet<Trip>(), state.selectedTrips)
  }

  @Test
  fun `selectAllTrips selects all current and upcoming trips`() = runTest {
    val now = Timestamp.now()

    val currentTrip =
        createTestTrip(
            uid = "1",
            name = "Current",
            startDate = Timestamp(now.seconds - 1000, 0),
            endDate = Timestamp(now.seconds + 1000, 0))
    val upcomingTrip =
        createTestTrip(
            uid = "2",
            name = "Upcoming",
            startDate = Timestamp(now.seconds + 2000, 0),
            endDate = Timestamp(now.seconds + 3000, 0))

    coEvery { repository.getAllTrips() } returns listOf(currentTrip, upcomingTrip)
    viewModel = MyTripsViewModel(userRepository = userRepository, tripsRepository = repository)
    advanceUntilIdle()

    viewModel.selectAllTrips()

    val selected = viewModel.uiState.value.selectedTrips
    assertEquals(setOf(currentTrip, upcomingTrip), selected)
  }

  @Test
  fun `deleteSelectedTrips sets error message on failure`() = runTest {
    val trip1 = createTestTrip(uid = "1")
    coEvery { repository.getAllTrips() } returns listOf(trip1)
    coEvery { repository.deleteTrip(any()) } throws Exception("DB failure")

    viewModel = MyTripsViewModel(userRepository = userRepository, tripsRepository = repository)
    advanceUntilIdle()

    viewModel.toggleSelectionMode(true)
    viewModel.toggleTripSelection(trip1)
    viewModel.deleteSelectedTrips()
    advanceUntilIdle()

    assert(viewModel.uiState.value.errorMsg?.contains("Failed to delete trips") == true)
  }

  private fun createTrips(): List<Trip> {
    val now = Timestamp.now()
    return listOf(
        createTestTrip("1", "Alpha", startDate = Timestamp(now.seconds + 1000, 0)),
        createTestTrip("2", "Beta", startDate = Timestamp(now.seconds + 2000, 0)),
        createTestTrip("3", "Gamma", startDate = Timestamp(now.seconds + 1500, 0)))
  }

  @Test
  fun `sort START_DATE_ASC`() = runTest {
    val trips = createTrips()
    coEvery { repository.getAllTrips() } returns trips

    viewModel = MyTripsViewModel(userRepository = userRepository, tripsRepository = repository)
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

    viewModel = MyTripsViewModel(userRepository = userRepository, tripsRepository = repository)
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

    viewModel = MyTripsViewModel(userRepository = userRepository, tripsRepository = repository)
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

    viewModel = MyTripsViewModel(userRepository = userRepository, tripsRepository = repository)
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

    viewModel = MyTripsViewModel(userRepository = userRepository, tripsRepository = repository)
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

    viewModel = MyTripsViewModel(userRepository = userRepository, tripsRepository = repository)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.updateSortType(TripSortType.NAME_DESC)
    testDispatcher.scheduler.advanceUntilIdle()

    val sorted = viewModel.uiState.value.tripsList
    assertEquals(listOf(trips[2], trips[1], trips[0]), sorted) // Gamma, Beta, Alpha
  }

  @Test
  fun `toggleFavoriteForSelectedTrips toggles favorites correctly`() = runTest {
    val trip = createTestTrip(uid = "1", isFavorite = false)
    coEvery { repository.getAllTrips() } returns listOf(trip)
    coEvery { repository.editTrip(any(), any()) } returns Unit

    viewModel = MyTripsViewModel(userRepository = userRepository, tripsRepository = repository)
    advanceUntilIdle()

    viewModel.toggleSelectionMode(true)
    viewModel.toggleTripSelection(trip)
    viewModel.toggleFavoriteForSelectedTrips()
    advanceUntilIdle()

    coVerify { repository.editTrip(trip.uid, trip.copy(isFavorite = true)) }
  }

  // chatGPT
  @Test
  fun `changeCurrentTrip sets error message on failure`() = runTest {
    val trip = createTestTrip(uid = "1")
    coEvery { repository.getAllTrips() } returns listOf(trip)
    coEvery { repository.editTrip(any(), any()) } throws Exception("Firestore edit failed")

    viewModel = MyTripsViewModel(userRepository = userRepository, tripsRepository = repository)
    advanceUntilIdle()

    // Act — trigger the failure
    viewModel.changeCurrentTrip(trip)
    advanceUntilIdle()

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
            uriLocation = emptyMap(),
            collaboratorsId = emptyList())

    coEvery { repository.editTrip(any(), any()) } throws Exception("Firestore edit failed")

    viewModel = MyTripsViewModel(userRepository = userRepository, tripsRepository = repository)
    viewModel.toggleTripSelection(trip)
    advanceUntilIdle()

    // Act — trigger the failure
    viewModel.toggleFavoriteForSelectedTrips()
    advanceUntilIdle()

    // Assert — verify error message set by catch block
    val state = viewModel.uiState.value
    assertEquals("Failed to update favorites.", state.errorMsg)
  }

  @Test
  fun `buildCollaboratorsByTrip fetches users correctly`() = runTest {
    // Arrange
    val tripId = "trip1"
    val collaboratorId = "user123"
    val trip = createTestTrip(uid = tripId, collaboratorsId = listOf(collaboratorId))
    val user = createTestUser(uid = collaboratorId, name = "John Doe")

    coEvery { userRepository.getUserByUid(collaboratorId) } returns user
    coEvery { repository.getAllTrips() } returns listOf(trip)

    // Re-initialize to trigger getAllTrips -> buildCollaboratorsByTrip
    viewModel = MyTripsViewModel(userRepository, repository)
    advanceUntilIdle()

    val collaborators = viewModel.uiState.value.collaboratorsByTripId[tripId]
    assertEquals(1, collaborators?.size)
    assertEquals("John Doe", collaborators?.get(0)?.displayName)
    assertEquals("http://example.com/pic.jpg", collaborators?.get(0)?.avatarUrl)
  }
}
