package com.github.swent.swisstravel.ui.profile.selectpinnedtrips

import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.user.User
import com.github.swent.swisstravel.model.user.UserRepository
import com.github.swent.swisstravel.model.user.UserStats
import com.google.firebase.Timestamp
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/** Unit tests for [SelectPinnedTripsViewModel]. Tests made with the help of AI. */
@OptIn(ExperimentalCoroutinesApi::class)
class SelectPinnedTripsViewModelTest {

  private lateinit var tripsRepository: TripsRepository
  private lateinit var userRepository: UserRepository
  private lateinit var viewModel: SelectPinnedTripsViewModel

  private val testDispatcher = StandardTestDispatcher()

  private val tripA =
      Trip(
          uid = "A",
          name = "A",
          ownerId = "",
          locations = emptyList(),
          routeSegments = emptyList(),
          activities = emptyList(),
          tripProfile =
              TripProfile(
                  startDate = Timestamp(0, 0),
                  endDate = Timestamp(0, 0),
                  preferredLocations = emptyList(),
                  preferences = emptyList()),
          isCurrentTrip = false,
          listUri = emptyList(),
          collaboratorsId = emptyList())
  private val tripB =
      Trip(
          uid = "B",
          name = "B",
          ownerId = "",
          locations = emptyList(),
          routeSegments = emptyList(),
          activities = emptyList(),
          tripProfile =
              TripProfile(
                  startDate = Timestamp(0, 0),
                  endDate = Timestamp(0, 0),
                  preferredLocations = emptyList(),
                  preferences = emptyList()),
          isCurrentTrip = false,
          listUri = emptyList(),
          collaboratorsId = emptyList())
  private val tripC =
      Trip(
          uid = "C",
          name = "C",
          ownerId = "",
          locations = emptyList(),
          routeSegments = emptyList(),
          activities = emptyList(),
          tripProfile =
              TripProfile(
                  startDate = Timestamp(0, 0),
                  endDate = Timestamp(0, 0),
                  preferredLocations = emptyList(),
                  preferences = emptyList()),
          isCurrentTrip = false,
          listUri = emptyList(),
          collaboratorsId = emptyList())
  private val tripD =
      Trip(
          uid = "D",
          name = "D",
          ownerId = "",
          locations = emptyList(),
          routeSegments = emptyList(),
          activities = emptyList(),
          tripProfile =
              TripProfile(
                  startDate = Timestamp(0, 0),
                  endDate = Timestamp(0, 0),
                  preferredLocations = emptyList(),
                  preferences = emptyList()),
          isCurrentTrip = false,
          listUri = emptyList(),
          collaboratorsId = emptyList())

  private val user =
      User(
          uid = "user123",
          name = "John Pork",
          pinnedTripsUids = listOf("A", "B"),
          biography = "",
          email = "",
          profilePicUrl = "",
          preferences = emptyList(),
          friends = emptyList(),
          stats = UserStats(),
          pinnedPicturesUids = emptyList(),
          favoriteTripsUids = emptyList())

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)

    tripsRepository = mockk()
    userRepository = mockk()

    coEvery { userRepository.getCurrentUser() } returns user
    coEvery { tripsRepository.getAllTrips() } returns listOf(tripA, tripB, tripC, tripD)
    coEvery { userRepository.getUserByUid(any()) } returns null

    viewModel =
        SelectPinnedTripsViewModel(
            tripsRepository = tripsRepository, userRepository = userRepository)

    // Run init {}
    testDispatcher.scheduler.advanceUntilIdle()
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun initializationLoadsTripsAndSelectsPinnedTrips() = runTest {
    val state = viewModel.uiState.value

    assertEquals(4, state.tripsList.size)
    assertEquals(setOf(tripA, tripB), state.selectedTrips)
  }

  @Test
  fun togglingRemovesAnAlreadySelectedTrip() {
    viewModel.onToggleTripSelection(tripA)
    val state = viewModel.uiState.value
    assertFalse(state.selectedTrips.contains(tripA))
  }

  @Test
  fun togglingAddsANewTripIfUnderLimit() {
    viewModel.onToggleTripSelection(tripC)
    val state = viewModel.uiState.value
    assertTrue(state.selectedTrips.contains(tripC))
  }

  @Test
  fun cannotSelectMoreThan3Trips() {
    // Already selected: A, B
    viewModel.onToggleTripSelection(tripC)
    testDispatcher.scheduler.advanceUntilIdle()

    // Try adding 4th
    viewModel.onToggleTripSelection(tripD)
    val state = viewModel.uiState.value

    assertEquals(3, state.selectedTrips.size)
    assertFalse(state.selectedTrips.contains(tripD))
    assertEquals("You can only pin up to 3 trips on your profile.", state.errorMsg)
  }

  @Test
  fun savingUpdatesUserRepositoryWithSelectedTripIds() = runTest {
    coEvery { userRepository.updateUser(any(), any()) } just Runs

    // Add one more selected trip
    viewModel.onToggleTripSelection(tripC)

    viewModel.onSaveSelectedTrips()
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify { userRepository.updateUser(uid = user.uid, pinnedTripsUids = listOf("A", "B", "C")) }
  }

  @Test
  fun saveShowsErrorWhenRepositoryUpdateFails() = runTest {
    coEvery { userRepository.updateUser(any(), any()) } throws RuntimeException()

    viewModel.onSaveSelectedTrips()
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals("Error updating selected Trips.", viewModel.uiState.value.errorMsg)
  }

  @Test
  fun refreshingTripsFetchesAgainAndKeepsSelected() = runTest {
    viewModel.onToggleTripSelection(tripC)
    coEvery { tripsRepository.getAllTrips() } returns listOf(tripA, tripB, tripC)

    viewModel.getAllTrips()
    val state = viewModel.uiState.value

    assertEquals(3, state.tripsList.size)
    assertTrue(state.selectedTrips.contains(tripC))
  }
}
