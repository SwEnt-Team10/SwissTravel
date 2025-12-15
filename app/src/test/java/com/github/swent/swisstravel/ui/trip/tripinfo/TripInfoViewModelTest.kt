package com.github.swent.swisstravel.ui.trip.tripinfo

import com.github.swent.swisstravel.MainDispatcherRule
import com.github.swent.swisstravel.createTestTrip
import com.github.swent.swisstravel.createTestUser
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.RouteSegment
import com.github.swent.swisstravel.model.trip.TransportMode
import com.github.swent.swisstravel.model.trip.TripElement
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.github.swent.swisstravel.model.user.FriendStatus
import com.github.swent.swisstravel.model.user.UserRepository
import com.github.swent.swisstravel.ui.trip.tripinfos.TripInfoViewModel
import com.google.firebase.Timestamp
import com.mapbox.geojson.Point
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class TripInfoViewModelTest {
  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private lateinit var tripsRepository: TripsRepository
  private lateinit var userRepository: UserRepository
  private lateinit var viewModel: TripInfoViewModel
  val now = Timestamp(1600000000, 0)

  private val dummyTrip =
      createTestTrip(
          uid = "trip1",
          name = "TripName",
          ownerId = "owner1",
          startDate = Timestamp(now.seconds - 3600, 0),
          endDate = Timestamp(now.seconds + 3600, 0))

  private val fakeUser = createTestUser(uid = "123", name = "Test User")

  @Before
  fun setup() {
    mockkStatic(android.util.Log::class)
    every { android.util.Log.d(any(), any()) } returns 0
    every { android.util.Log.e(any(), any(), any()) } returns 0

    tripsRepository = mockk()
    userRepository = mockk()
    viewModel = TripInfoViewModel(tripsRepository, userRepository)
  }

  @Test
  fun `toggleFavorite updates UI state and calls UserRepository to add favorite`() = runTest {
    // Arrange: User does NOT have the trip in favorites initially
    coEvery { userRepository.getCurrentUser() } returns fakeUser
    coEvery { tripsRepository.getTrip(dummyTrip.uid) } returns dummyTrip
    // We expect addFavoriteTrip to be called
    coEvery { userRepository.addFavoriteTrip(fakeUser.uid, dummyTrip.uid) } just Runs

    // Load trip
    viewModel.loadTripInfo(dummyTrip.uid)
    advanceUntilIdle()
    assertFalse(viewModel.uiState.value.isFavorite)

    // Initial state check
    assertFalse("Trip should not be favorite initially", viewModel.uiState.value.isFavorite)

    // Act: Toggle
    viewModel.toggleFavorite()
    advanceUntilIdle() // Advance for debounce/flow collection

    // Assert
    assertTrue("UI state should update to favorite", viewModel.uiState.value.isFavorite)
    coVerify { userRepository.addFavoriteTrip(fakeUser.uid, dummyTrip.uid) }
  }

  @Test
  fun `toggleFavorite calls UserRepository to remove favorite if already favorite`() = runTest {
    // Arrange: User ALREADY has the trip in favorites
    val userWithFavorite = fakeUser.copy(favoriteTripsUids = listOf(dummyTrip.uid))
    coEvery { userRepository.getCurrentUser() } returns userWithFavorite
    coEvery { tripsRepository.getTrip(dummyTrip.uid) } returns dummyTrip
    // We expect removeFavoriteTrip to be called
    coEvery { userRepository.removeFavoriteTrip(fakeUser.uid, dummyTrip.uid) } just Runs

    // Load trip
    viewModel.loadTripInfo(dummyTrip.uid)
    advanceUntilIdle()

    // Initial state check
    assertTrue("Trip should be favorite initially", viewModel.uiState.value.isFavorite)

    // Act: Toggle
    viewModel.toggleFavorite()
    advanceUntilIdle()

    // Assert
    assertFalse("UI state should update to not favorite", viewModel.uiState.value.isFavorite)
    coVerify { userRepository.removeFavoriteTrip(fakeUser.uid, dummyTrip.uid) }
  }

  // --- New Tests ---

  @Test
  fun `loadTripInfo updates UI state successfully`() = runTest {
    // Arrange
    coEvery { tripsRepository.getTrip(dummyTrip.uid) } returns dummyTrip
    coEvery { userRepository.getCurrentUser() } returns fakeUser

    // Act
    viewModel.loadTripInfo(dummyTrip.uid)
    advanceUntilIdle()

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
    advanceUntilIdle()
    // Assert
    // Should not crash, and state should remain default or show error depending on impl.
    // Assuming default empty state:
    assertEquals("Trip Name", viewModel.uiState.value.name)
  }

  @Test
  fun `loadTripInfo sets error message when repository throws exception`() = runTest {
    // Arrange
    val errorMsg = "Network error"
    coEvery { tripsRepository.getTrip(any()) } throws Exception(errorMsg)

    // Act
    viewModel.loadTripInfo("someID")
    advanceUntilIdle()

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
    advanceUntilIdle()
    // Assert
    assertEquals("Failed to load trip info: Error", viewModel.uiState.value.errorMsg)
  }

  @Test
  fun `setCurrentDayIndex updates state`() = runTest {
    val segment1 =
        RouteSegment(
            Location(Coordinate(0.0, 0.0), "A"),
            Location(Coordinate(1.0, 1.0), "B"),
            10,
            TransportMode.WALKING,
            now,
            now)
    val segment2 =
        RouteSegment(
            Location(Coordinate(2.0, 2.0), "C"),
            Location(Coordinate(3.0, 3.0), "D"),
            10,
            TransportMode.WALKING,
            Timestamp(now.seconds + 86400, 0), // Next day
            Timestamp(now.seconds + 86400, 0))
    val tripWithSegments = dummyTrip.copy(routeSegments = listOf(segment1, segment2))
    coEvery { tripsRepository.getTrip(tripWithSegments.uid) } returns tripWithSegments

    viewModel.loadTripInfo(tripWithSegments.uid)
    advanceUntilIdle()

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
                Location(Coordinate(0.0, 0.0), "A"),
                Location(Coordinate(1.0, 1.0), "B"),
                10,
                TransportMode.WALKING,
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
            location = Location(Coordinate(0.0, 0.0), "A"),
            description = "Desc",
            imageUrls = emptyList(),
            estimatedTime = 60)
    viewModel.selectActivity(activity)
    assertEquals(activity, viewModel.uiState.value.selectedActivity)
  }

  @Test
  fun `loadCollaboratorData updates availableFriends and collaborators`() = runTest {
    // Arrange
    val ownerId = fakeUser.uid
    val friend1 = createTestUser(uid = "friend1", name = "Friend One")
    val friend2 = createTestUser(uid = "friend2", name = "Friend Two")
    val tripWithCollaborator =
        dummyTrip.copy(ownerId = ownerId, collaboratorsId = listOf("friend2"))

    // User has friend1 and friend2
    val userWithFriends =
        fakeUser.copy(
            friends =
                listOf(
                    com.github.swent.swisstravel.model.user.Friend(
                        "friend1", FriendStatus.ACCEPTED),
                    com.github.swent.swisstravel.model.user.Friend(
                        "friend2", FriendStatus.ACCEPTED)))

    coEvery { userRepository.getCurrentUser() } returns userWithFriends
    coEvery { tripsRepository.getTrip(tripWithCollaborator.uid) } returns tripWithCollaborator
    coEvery { userRepository.getUserByUid("friend1") } returns friend1
    coEvery { userRepository.getUserByUid("friend2") } returns friend2

    // Initialize VM with the trip
    viewModel.loadTripInfo(tripWithCollaborator.uid)
    advanceUntilIdle()

    // Act
    viewModel.loadCollaboratorData()
    advanceUntilIdle()

    // Assert
    // collaborators should contain friend2
    assertEquals(1, viewModel.uiState.value.collaborators.size)
    assertEquals("friend2", viewModel.uiState.value.collaborators[0].uid)
    // availableFriends should contain friend1 but NOT friend2 (already collaborator)
    assertEquals(1, viewModel.uiState.value.availableFriends.size)
    assertEquals("friend1", viewModel.uiState.value.availableFriends[0].uid)
  }

  @Test
  fun `addCollaborator calls shareTripWithUsers and reloads data`() = runTest {
    // Arrange
    val tripId = "trip1"
    val initialTrip = dummyTrip.copy(uid = tripId, collaboratorsId = emptyList())
    val newCollaborator = createTestUser(uid = "newCollab")

    coEvery { tripsRepository.getTrip(tripId) } returns initialTrip
    coEvery { userRepository.getCurrentUser() } returns fakeUser

    // Mock the specific share method
    coEvery { tripsRepository.shareTripWithUsers(tripId, any()) } just Runs

    // Mock get user for the reload part
    coEvery { userRepository.getUserByUid("newCollab") } returns newCollaborator

    viewModel.loadTripInfo(tripId)
    advanceUntilIdle()

    // Act
    viewModel.addCollaborator(newCollaborator)
    advanceUntilIdle()

    // Assert
    coVerify { tripsRepository.shareTripWithUsers(tripId, listOf(newCollaborator.uid)) }
  }

  @Test
  fun `removeCollaborator calls removeCollaborator and reloads data`() = runTest {
    // Arrange
    val tripId = "trip1"
    val collaboratorToRemove = createTestUser(uid = "collab1")
    val initialTrip = dummyTrip.copy(uid = tripId, collaboratorsId = listOf("collab1", "collab2"))

    coEvery { tripsRepository.getTrip(tripId) } returns initialTrip
    coEvery { userRepository.getCurrentUser() } returns fakeUser

    // Mock the specific remove method
    coEvery { tripsRepository.removeCollaborator(tripId, any()) } just Runs

    // Mock loading the remaining collaborator (for reload)
    coEvery { userRepository.getUserByUid("collab2") } returns fakeUser.copy(uid = "collab2")

    viewModel.loadTripInfo(tripId)
    advanceUntilIdle()

    // Act
    viewModel.removeCollaborator(collaboratorToRemove)
    advanceUntilIdle()

    // Assert
    coVerify { tripsRepository.removeCollaborator(tripId, collaboratorToRemove.uid) }
  }
}
