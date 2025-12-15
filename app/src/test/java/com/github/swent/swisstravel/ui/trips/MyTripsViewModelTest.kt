package com.github.swent.swisstravel.ui.trips

import com.github.swent.swisstravel.MainDispatcherRule
import com.github.swent.swisstravel.createTestTrip
import com.github.swent.swisstravel.createTestUser
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
class MyTripsViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  @get:Rule val mainDispatcherRule = MainDispatcherRule(testDispatcher)

  private lateinit var tripsRepository: TripsRepository
  private lateinit var userRepository: UserRepository
  private lateinit var viewModel: MyTripsViewModel
  private lateinit var dummyUser: User

  @Before
  fun setup() {
    tripsRepository = mockk()
    userRepository = mockk()

    dummyUser =
        User(
            uid = "user",
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
    coEvery { userRepository.getCurrentUser() } returns dummyUser
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

    coEvery { tripsRepository.getAllTrips() } returns listOf(currentTrip, upcomingTrip)
    viewModel = MyTripsViewModel(userRepository = userRepository, tripsRepository = tripsRepository)
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

    coEvery { tripsRepository.getAllTrips() } returns listOf(currentTrip)
    viewModel = MyTripsViewModel(userRepository = userRepository, tripsRepository = tripsRepository)
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

    coEvery { tripsRepository.getAllTrips() } returns listOf(upcomingTrip)
    viewModel = MyTripsViewModel(userRepository = userRepository, tripsRepository = tripsRepository)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(null, state.currentTrip)
    assertEquals(listOf(upcomingTrip), state.tripsList)
  }

  @Test
  fun `UI state shows empty when no trips`() = runTest {
    coEvery { tripsRepository.getAllTrips() } returns emptyList()

    viewModel = MyTripsViewModel(userRepository = userRepository, tripsRepository = tripsRepository)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(null, state.currentTrip)
    assertEquals(0, state.tripsList.size)
  }

  @Test
  fun `UI state shows error message on exception`() = runTest {
    coEvery { tripsRepository.getAllTrips() } throws Exception("Fake network error")

    viewModel = MyTripsViewModel(userRepository = userRepository, tripsRepository = tripsRepository)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals("Failed to load trips.", state.errorMsg)
  }

  @Test
  fun `clearErrorMsg clears the error`() = runTest {
    coEvery { tripsRepository.getAllTrips() } throws Exception("Fake network error")

    viewModel = MyTripsViewModel(userRepository = userRepository, tripsRepository = tripsRepository)
    advanceUntilIdle()

    assert(viewModel.uiState.value.errorMsg != null)
    viewModel.clearErrorMsg()
    val state = viewModel.uiState.value
    assertEquals(null, state.errorMsg)
  }

  @Test
  fun `toggleSelectionMode enables and disables selection correctly`() = runTest {
    coEvery { tripsRepository.getAllTrips() } returns emptyList()
    viewModel = MyTripsViewModel(userRepository = userRepository, tripsRepository = tripsRepository)
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
    coEvery { tripsRepository.getAllTrips() } returns emptyList()
    viewModel = MyTripsViewModel(userRepository = userRepository, tripsRepository = tripsRepository)
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
    coEvery { tripsRepository.getAllTrips() } returns emptyList()
    viewModel = MyTripsViewModel(userRepository = userRepository, tripsRepository = tripsRepository)
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

    coEvery { tripsRepository.getAllTrips() } returns listOf(trip1, trip2)
    coEvery { tripsRepository.deleteTrip(any()) } returns Unit

    viewModel = MyTripsViewModel(userRepository = userRepository, tripsRepository = tripsRepository)
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

    coEvery { tripsRepository.getAllTrips() } returns listOf(currentTrip, upcomingTrip)
    viewModel = MyTripsViewModel(userRepository = userRepository, tripsRepository = tripsRepository)
    advanceUntilIdle()

    viewModel.selectAllTrips()

    val selected = viewModel.uiState.value.selectedTrips
    assertEquals(setOf(currentTrip, upcomingTrip), selected)
  }

  @Test
  fun `deleteSelectedTrips sets error message on failure`() = runTest {
    val trip1 = createTestTrip(uid = "1")
    coEvery { tripsRepository.getAllTrips() } returns listOf(trip1)
    coEvery { tripsRepository.deleteTrip(any()) } throws Exception("DB failure")

    viewModel = MyTripsViewModel(userRepository = userRepository, tripsRepository = tripsRepository)
    advanceUntilIdle()

    viewModel.toggleSelectionMode(true)
    viewModel.toggleTripSelection(trip1)
    viewModel.deleteSelectedTrips()
    advanceUntilIdle()

    assert(viewModel.uiState.value.errorMsg?.contains("Failed to delete trips") == true)
  }

  @Test
  fun `toggleFavoriteForSelectedTrips toggles favorites correctly`() = runTest {
    val trip = createTestTrip(uid = "1", name = "Trip")
    val user = createTestUser(uid = "user1", name = "Test")

    coEvery { tripsRepository.getAllTrips() } returns listOf(trip)
    coEvery { userRepository.getCurrentUser() } returns user
    coEvery { userRepository.addFavoriteTrip(any(), any()) } returns Unit
    coEvery { userRepository.removeFavoriteTrip(any(), any()) } returns Unit

    viewModel = MyTripsViewModel(userRepository = userRepository, tripsRepository = tripsRepository)
    advanceUntilIdle()

    viewModel.toggleSelectionMode(true)
    viewModel.toggleTripSelection(trip)
    viewModel.toggleFavoriteForSelectedTrips()
    advanceUntilIdle()

    coVerify { userRepository.addFavoriteTrip(user.uid, trip.uid) }
  }

  // chatGPT
  @Test
  fun `changeCurrentTrip sets error message on failure`() = runTest {
    val trip = createTestTrip(uid = "1")
    coEvery { tripsRepository.getAllTrips() } returns listOf(trip)
    coEvery { tripsRepository.editTrip(any(), any()) } throws Exception("Firestore edit failed")

    viewModel = MyTripsViewModel(userRepository = userRepository, tripsRepository = tripsRepository)
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
            isCurrentTrip = false,
            uriLocation = emptyMap(),
            collaboratorsId = emptyList())

    coEvery { tripsRepository.editTrip(any(), any()) } throws Exception("Firestore edit failed")

    viewModel = MyTripsViewModel(userRepository = userRepository, tripsRepository = tripsRepository)
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
    coEvery { tripsRepository.getAllTrips() } returns listOf(trip)

    // Re-initialize to trigger getAllTrips -> buildCollaboratorsByTrip
    viewModel = MyTripsViewModel(userRepository, tripsRepository)
    advanceUntilIdle()

    val collaborators = viewModel.uiState.value.collaboratorsByTripId[tripId]
    assertEquals(1, collaborators?.size)
    assertEquals("John Doe", collaborators?.get(0)?.displayName)
    assertEquals("http://example.com/pic.jpg", collaborators?.get(0)?.avatarUrl)
  }

  // --- SORTING TESTS ---
  // Helper for sort tests
  private fun createSortTrips(): List<Trip> {
    val now = Timestamp.now()
    return listOf(
        createTestTrip(
            "1",
            "Alpha",
            startDate = Timestamp(now.seconds + 1000, 0),
            endDate = Timestamp(now.seconds + 2000, 0)),
        createTestTrip(
            "2",
            "Beta",
            startDate = Timestamp(now.seconds + 2000, 0),
            endDate = Timestamp(now.seconds + 3000, 0)),
        createTestTrip(
            "3",
            "Gamma",
            startDate = Timestamp(now.seconds + 1500, 0),
            endDate = Timestamp(now.seconds + 2500, 0)))
  }

  @Test
  fun `sort START_DATE_ASC`() =
      runTest(testDispatcher) {
        val trips = createSortTrips()
        coEvery { tripsRepository.getAllTrips() } returns trips
        viewModel = MyTripsViewModel(userRepository, tripsRepository)
        advanceUntilIdle()

        viewModel.updateSortType(TripSortType.START_DATE_ASC)
        advanceUntilIdle() // This now works because testDispatcher is shared

        val sorted = viewModel.uiState.value.tripsList
        assertEquals(listOf(trips[0], trips[2], trips[1]), sorted)
      }

  @Test
  fun `sort START_DATE_DESC`() =
      runTest(testDispatcher) {
        val trips = createSortTrips()
        coEvery { tripsRepository.getAllTrips() } returns trips
        viewModel = MyTripsViewModel(userRepository, tripsRepository)
        advanceUntilIdle()

        viewModel.updateSortType(TripSortType.START_DATE_DESC)
        advanceUntilIdle()

        val sorted = viewModel.uiState.value.tripsList
        assertEquals(listOf(trips[1], trips[2], trips[0]), sorted)
      }

  @Test
  fun `sort END_DATE_ASC`() =
      runTest(testDispatcher) {
        val trips = createSortTrips()
        coEvery { tripsRepository.getAllTrips() } returns trips

        viewModel =
            MyTripsViewModel(userRepository = userRepository, tripsRepository = tripsRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateSortType(TripSortType.END_DATE_ASC)
        testDispatcher.scheduler.advanceUntilIdle()

        val sorted = viewModel.uiState.value.tripsList
        assertEquals(listOf(trips[0], trips[2], trips[1]), sorted)
      }

  @Test
  fun `sort END_DATE_DESC`() =
      runTest(testDispatcher) {
        val trips = createSortTrips()
        coEvery { tripsRepository.getAllTrips() } returns trips

        viewModel =
            MyTripsViewModel(userRepository = userRepository, tripsRepository = tripsRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateSortType(TripSortType.END_DATE_DESC)
        testDispatcher.scheduler.advanceUntilIdle()

        val sorted = viewModel.uiState.value.tripsList
        assertEquals(listOf(trips[1], trips[2], trips[0]), sorted)
      }

  @Test
  fun `sort NAME_ASC`() =
      runTest(testDispatcher) {
        val trips = createSortTrips()
        coEvery { tripsRepository.getAllTrips() } returns trips
        viewModel = MyTripsViewModel(userRepository, tripsRepository)
        advanceUntilIdle()

        viewModel.updateSortType(TripSortType.NAME_ASC)
        advanceUntilIdle()

        val sorted = viewModel.uiState.value.tripsList
        assertEquals(listOf(trips[0], trips[1], trips[2]), sorted) // Alpha, Beta, Gamma
      }

  @Test
  fun `sort NAME_DESC`() =
      runTest(testDispatcher) {
        val trips = createSortTrips()
        coEvery { tripsRepository.getAllTrips() } returns trips

        viewModel =
            MyTripsViewModel(userRepository = userRepository, tripsRepository = tripsRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateSortType(TripSortType.NAME_DESC)
        testDispatcher.scheduler.advanceUntilIdle()

        val sorted = viewModel.uiState.value.tripsList
        assertEquals(listOf(trips[2], trips[1], trips[0]), sorted) // Gamma, Beta, Alpha
      }
}
