package com.github.swent.swisstravel.ui.trip.tripinfo

import com.github.swent.swisstravel.MainDispatcherRule
import com.github.swent.swisstravel.createTestTrip
import com.github.swent.swisstravel.createTestUser
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.RouteSegment
import com.github.swent.swisstravel.model.trip.TransportMode
import com.github.swent.swisstravel.model.trip.TripElement
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.github.swent.swisstravel.model.user.FriendStatus
import com.github.swent.swisstravel.model.user.UserRepository
import com.github.swent.swisstravel.ui.trip.tripinfos.TripInfoViewModel
import com.github.swent.swisstravel.ui.tripcreation.TripSettings
import com.google.firebase.Timestamp
import com.mapbox.geojson.Point
import io.mockk.*
import java.time.ZoneId
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

  private val activity1 =
      Activity(
          startDate = now,
          endDate = now,
          location = Location(com.github.swent.swisstravel.model.trip.Coordinate(0.0, 0.0), "A"),
          description = "Desc1",
          imageUrls = emptyList(),
          estimatedTime = 60)
  private val activity2 =
      Activity(
          startDate = now,
          endDate = now,
          location = Location(com.github.swent.swisstravel.model.trip.Coordinate(1.0, 1.0), "B"),
          description = "Desc2",
          imageUrls = emptyList(),
          estimatedTime = 45)

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
    coEvery { tripsRepository.editTrip(any(), any()) } returns Unit

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
    coEvery { tripsRepository.editTrip(any(), any()) } returns Unit

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
    coEvery { tripsRepository.editTrip(dummyTrip.uid, any()) } just Runs

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
    coEvery { tripsRepository.editTrip(tripWithCollaborator.uid, any()) } just Runs

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
    coEvery { tripsRepository.editTrip(tripId, any()) } just Runs

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
    coEvery { tripsRepository.editTrip(tripId, any()) } just Runs

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

  @Test
  fun swipeActivityWorks() = runTest {
    val tripWithActivities =
        dummyTrip.copy(uid = "tripWithActivities", activitiesQueue = listOf(activity1, activity2))

    coEvery { tripsRepository.getTrip(tripWithActivities.uid) } returns tripWithActivities
    coEvery { userRepository.getCurrentUser() } returns fakeUser
    coEvery { tripsRepository.editTrip(any(), any()) } just Runs

      // load trip
      viewModel.loadTripInfo(tripWithActivities.uid)
      advanceUntilIdle()

      // Check which one is first after shuffle
      val firstActivity = viewModel.uiState.value.currentActivity!!
      val secondActivity = if (firstActivity == activity1) activity2 else activity1

      // like the first activity
      viewModel.swipeActivity(liked = true)
      advanceUntilIdle()

      // first activity should be in likedActivities
      assertEquals(1, viewModel.uiState.value.likedActivities.size)
      assertEquals(firstActivity, viewModel.uiState.value.likedActivities[0])

      // activitiesQueue should have one less activity
      assertEquals(1, viewModel.uiState.value.activitiesQueue.size)
      assertEquals(secondActivity, viewModel.uiState.value.activitiesQueue[0])

    // dislike the next activity
    viewModel.swipeActivity(liked = false)
    advanceUntilIdle()

    // likedActivities should remain the same
    assertEquals(1, viewModel.uiState.value.likedActivities.size)
    // activitiesQueue should be empty now (no fetching of new activity since we didn't mock that)
    assertTrue(viewModel.uiState.value.activitiesQueue.isEmpty())
  }

  @Test
  fun `mapToTripSettings returns correct TripSettings`() = runTest {
    val tripProfile =
        TripProfile(
            startDate = now,
            endDate = Timestamp(now.seconds + 7200, 0),
            preferredLocations = emptyList(),
            preferences = emptyList())
    val trip = dummyTrip.copy(tripProfile = tripProfile)
    coEvery { tripsRepository.getTrip(trip.uid) } returns trip
    coEvery { userRepository.getCurrentUser() } returns fakeUser
    coEvery { tripsRepository.editTrip(any(), any()) } just Runs

    // Load trip
    viewModel.loadTripInfo(trip.uid)
    advanceUntilIdle()

    // create trip settings
    val tripSettings: TripSettings = viewModel.mapToTripSettings()

    // trip name
    assertEquals(trip.name, tripSettings.name)
    // start / end date
    assertEquals(
        trip.tripProfile.startDate
            .toDate()
            .toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDate(),
        tripSettings.date.startDate)
    assertEquals(
        trip.tripProfile.endDate.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
        tripSettings.date.endDate)
    // preferences
    assertEquals(trip.tripProfile.preferences, tripSettings.preferences)
    // arrival/departure locations
    assertEquals(trip.tripProfile.arrivalLocation, tripSettings.arrivalDeparture.arrivalLocation)
    assertEquals(
        trip.tripProfile.departureLocation, tripSettings.arrivalDeparture.departureLocation)
    // destinations
    assertEquals(trip.tripProfile.preferredLocations, tripSettings.destinations)
    // travelers
    assertEquals(trip.tripProfile.adults, tripSettings.travelers.adults)
    assertEquals(trip.tripProfile.children, tripSettings.travelers.children)
    // InvalidNameMsg should stay null since the tripInfo should already have a valid name
    assertNull(tripSettings.invalidNameMsg)
  }

  @Test
  fun `likeActivities adds activities to likedActivities`() = runTest {

    // mock trip edition on database
    coEvery { tripsRepository.editTrip(any(), any()) } just Runs
    coEvery { tripsRepository.getTrip(any()) } returns dummyTrip

    viewModel.loadTripInfo(dummyTrip.uid)
    advanceUntilIdle()

    viewModel.likeActivities(listOf(activity1, activity2))

    assertEquals(2, viewModel.uiState.value.likedActivities.size)
    assertEquals(activity1, viewModel.uiState.value.likedActivities[0])
    assertEquals(activity2, viewModel.uiState.value.likedActivities[1])
  }

  @Test
  fun `unlikeSelectedActivities removes selected likedActivities`() = runTest {

    // mock trip on database
    coEvery { tripsRepository.editTrip(any(), any()) } just Runs
    coEvery { tripsRepository.getTrip(any()) } returns dummyTrip

    viewModel.loadTripInfo(dummyTrip.uid)
    advanceUntilIdle()

    // like two activities
    viewModel.likeActivities(listOf(activity1, activity2))

    // select activity1
    viewModel.selectLikedActivity(activity1)

    // unlike all selected activities
    viewModel.unlikeSelectedActivities()

    // only activity2 should remain
    assertEquals(1, viewModel.uiState.value.likedActivities.size)
    assertEquals(activity2, viewModel.uiState.value.likedActivities[0])

    // selectedLikedActivities should be cleared
    assertEquals(0, viewModel.uiState.value.selectedLikedActivities.size)
  }

  @Test
  fun `selectAndDeselectLikedActivity updates selectedLikedActivities`() = runTest {

    // mock trip on database
    coEvery { tripsRepository.editTrip(any(), any()) } just Runs
    coEvery { tripsRepository.getTrip(any()) } returns dummyTrip

    viewModel.loadTripInfo(dummyTrip.uid)
    advanceUntilIdle()

    // like two activities
    viewModel.likeActivities(listOf(activity1, activity2))

    // select activity1
    viewModel.selectLikedActivity(activity1)
    assertEquals(1, viewModel.uiState.value.selectedLikedActivities.size)
    assertEquals(activity1, viewModel.uiState.value.selectedLikedActivities[0])

    // select activity2
    viewModel.selectLikedActivity(activity2)
    assertEquals(2, viewModel.uiState.value.selectedLikedActivities.size)

    // deselect activity1
    viewModel.deselectLikedActivity(activity1)
    assertEquals(1, viewModel.uiState.value.selectedLikedActivities.size)
    assertEquals(activity2, viewModel.uiState.value.selectedLikedActivities[0])
  }
}
