package com.github.swent.swisstravel.ui.trips

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.user.User
import com.github.swent.swisstravel.model.user.UserStats
import com.github.swent.swisstravel.ui.composable.DeleteTripDialogTestTags
import com.github.swent.swisstravel.ui.composable.SortMenuTestTags
import com.github.swent.swisstravel.ui.composable.TripListTestTags
import com.github.swent.swisstravel.utils.FakeTripsRepository
import com.github.swent.swisstravel.utils.FakeUserRepository
import com.github.swent.swisstravel.utils.InMemorySwissTravelTest
import com.google.firebase.Timestamp
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class MyTripsScreenEmulatorTest : InMemorySwissTravelTest() {

  @get:Rule val composeTestRule = createComposeRule()
  private val now = Timestamp.now()
  val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

  private val currentTrip = trip1.copy(isCurrentTrip = true)

  private val upcomingTrip = trip2

  /** Helper to launch screen with trips and optional collaborator data. */
  private fun launchScreen(trips: List<Trip>, users: List<User> = emptyList()): MyTripsViewModel {
    val fakeTripRepo = FakeTripsRepository(trips.toMutableList())
    val fakeUserRepo = FakeUserRepository()
    users.forEach { fakeUserRepo.addUser(it) }

    // IMPORTANT: Pass BOTH repositories to avoid default Firebase init
    val viewModel = MyTripsViewModel(userRepository = fakeUserRepo, tripsRepository = fakeTripRepo)

    composeTestRule.setContent { MyTripsScreen(myTripsViewModel = viewModel) }
    return viewModel
  }

  @Test
  fun displaysCurrentAndUpcomingTrips_usingRealViewModel() {
    val fakeRepo = FakeTripsRepository(mutableListOf(currentTrip, upcomingTrip))
    val viewModel =
        MyTripsViewModel(userRepository = FakeUserRepository(), tripsRepository = fakeRepo)

    composeTestRule.setContent { MyTripsScreen(myTripsViewModel = viewModel) }

    // Check current trip
    composeTestRule.onNodeWithTag(MyTripsScreenTestTags.CURRENT_TRIP_TITLE).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(MyTripsScreenTestTags.getTestTagForTrip(currentTrip))
        .assertIsDisplayed()

    // Check upcoming trip
    composeTestRule.checkSortedTripListNotEmptyIsDisplayed()
    composeTestRule
        .onNodeWithTag(MyTripsScreenTestTags.getTestTagForTrip(upcomingTrip))
        .assertIsDisplayed()
  }

  @Test
  fun displaysEmptyMessagesWhenNoTrips() {
    val viewModel =
        MyTripsViewModel(
            userRepository = FakeUserRepository(), tripsRepository = FakeTripsRepository())

    composeTestRule.setContent { MyTripsScreen(myTripsViewModel = viewModel) }

    composeTestRule
        .onNodeWithTag(MyTripsScreenTestTags.EMPTY_CURRENT_TRIP_MSG, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun pastTripsButton_clickTriggersCallback() {
    var clicked = false
    val viewModel =
        MyTripsViewModel(
            userRepository = FakeUserRepository(), tripsRepository = FakeTripsRepository())

    composeTestRule.setContent {
      MyTripsScreen(myTripsViewModel = viewModel, onPastTrips = { clicked = true })
    }

    composeTestRule.onNodeWithTag(MyTripsScreenTestTags.PAST_TRIPS_BUTTON).performClick()
    assertTrue(clicked)
  }

  @Test
  fun addTripButton_triggersAddTripCallback() {
    var createClicked = false
    val viewModel =
        MyTripsViewModel(
            userRepository = FakeUserRepository(), tripsRepository = FakeTripsRepository())

    composeTestRule.setContent {
      MyTripsScreen(myTripsViewModel = viewModel, onCreateTrip = { createClicked = true })
    }

    composeTestRule.onNodeWithTag(MyTripsScreenTestTags.CREATE_TRIP_BUTTON).performClick()
    assertTrue(createClicked)
  }

  @Test
  fun addingTrip_updatesUpcomingTripsList() {
    val fakeRepo = FakeTripsRepository(mutableListOf(currentTrip))
    val viewModel =
        MyTripsViewModel(userRepository = FakeUserRepository(), tripsRepository = fakeRepo)

    composeTestRule.setContent { MyTripsScreen(myTripsViewModel = viewModel) }

    // Before adding, no upcoming trips
    composeTestRule.onNodeWithTag(TripListTestTags.TRIP_LIST).assertExists()

    // Add a new upcoming trip inside a coroutine
    val newUpcomingTrip =
        createTestTrip(
            uid = "3",
            name = "New Upcoming",
            startDate = Timestamp(now.seconds + 7200, 0),
            endDate = Timestamp(now.seconds + 10800, 0))

    runBlocking { fakeRepo.addTrip(newUpcomingTrip) }

    // Trigger recomposition and refresh state
    composeTestRule.runOnIdle { viewModel.refreshUIState() }

    composeTestRule.waitForIdle()

    // Check that the new trip appears
    composeTestRule
        .onNodeWithTag(MyTripsScreenTestTags.getTestTagForTrip(newUpcomingTrip))
        .assertIsDisplayed()
  }

  @Test
  fun sortingUpcomingTrips_worksCorrectly() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val now = Timestamp.now()

    val tripA =
        createTestTrip(
            uid = "a",
            name = "Alpha",
            startDate = Timestamp(now.seconds + 10800, 0),
            endDate = Timestamp(now.seconds + 14400, 0))
    val tripB =
        createTestTrip(
            uid = "b",
            name = "Beta",
            startDate = Timestamp(now.seconds + 7200, 0),
            endDate = Timestamp(now.seconds + 10800, 0))

    val fakeRepo = FakeTripsRepository(mutableListOf(tripA, tripB))
    val viewModel =
        MyTripsViewModel(userRepository = FakeUserRepository(), tripsRepository = fakeRepo)

    composeTestRule.setContent { MyTripsScreen(myTripsViewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Initial order (START_DATE_ASC expected)
    val tripANodeInitial =
        composeTestRule
            .onNodeWithTag(MyTripsScreenTestTags.getTestTagForTrip(tripA))
            .fetchSemanticsNode()
    val tripBNodeInitial =
        composeTestRule
            .onNodeWithTag(MyTripsScreenTestTags.getTestTagForTrip(tripB))
            .fetchSemanticsNode()

    assertTrue(
        tripBNodeInitial.positionInRoot.y < tripANodeInitial.positionInRoot.y,
        "Trip B should appear before Trip A when sorted ASC by start date")

    // Change sort to START_DATE_DESC
    composeTestRule.onNodeWithTag(SortMenuTestTags.SORT_DROPDOWN_MENU).performClick()
    composeTestRule.onNodeWithText(context.getString(R.string.start_date_desc)).performClick()

    composeTestRule.waitForIdle()

    // New order (START_DATE_DESC expected)
    val tripANodeAfterSort =
        composeTestRule
            .onNodeWithTag(MyTripsScreenTestTags.getTestTagForTrip(tripA))
            .fetchSemanticsNode()
    val tripBNodeAfterSort =
        composeTestRule
            .onNodeWithTag(MyTripsScreenTestTags.getTestTagForTrip(tripB))
            .fetchSemanticsNode()

    assertTrue(
        tripANodeAfterSort.positionInRoot.y < tripBNodeAfterSort.positionInRoot.y,
        "Trip A should appear before Trip B when sorted DESC by start date")
  }

  @Test
  fun longPressTripEntersSelectionMode() {
    launchScreen(listOf(trip1, trip2))

    // Long press trip1 to enter selection mode
    composeTestRule
        .onNodeWithTag(MyTripsScreenTestTags.getTestTagForTrip(trip1))
        .performTouchInput { longClick() }

    // Check that selection mode UI appears
    composeTestRule.onNodeWithTag(MyTripsScreenTestTags.CANCEL_SELECTION_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MyTripsScreenTestTags.DELETE_SELECTED_BUTTON).assertIsDisplayed()
  }

  @Test
  fun selectAllButtonSelectsAllTrips() {
    launchScreen(listOf(trip1, trip2))

    // Enter selection mode
    composeTestRule
        .onNodeWithTag(MyTripsScreenTestTags.getTestTagForTrip(trip1))
        .performTouchInput { longClick() }

    // Open dropdown menu
    composeTestRule.onNodeWithTag(MyTripsScreenTestTags.MORE_OPTIONS_BUTTON).performClick()

    // Click "Select All"
    composeTestRule.onNodeWithTag(MyTripsScreenTestTags.SELECT_ALL_BUTTON).performClick()

    // Verify all trips are selected
    composeTestRule
        .onNodeWithTag(TripElementTestTags.getTestTagForTripCheckbox(trip1), useUnmergedTree = true)
        .assertIsOn()
    composeTestRule
        .onNodeWithTag(TripElementTestTags.getTestTagForTripCheckbox(trip2), useUnmergedTree = true)
        .assertIsOn()
  }

  @Test
  fun deleteConfirmationDialogAppearsAndCancels() {
    val viewModel = launchScreen(listOf(trip1, trip2))

    // Enter selection mode and select a trip
    composeTestRule
        .onNodeWithTag(MyTripsScreenTestTags.getTestTagForTrip(trip1))
        .performTouchInput { longClick() }

    composeTestRule.onNodeWithTag(MyTripsScreenTestTags.DELETE_SELECTED_BUTTON).performClick()

    // AlertDialog should appear
    composeTestRule
        .onNodeWithTag(DeleteTripDialogTestTags.CONFIRM_DELETE_BUTTON)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(DeleteTripDialogTestTags.CANCEL_DELETE_BUTTON).assertIsDisplayed()

    // Cancel deletion
    composeTestRule.onNodeWithTag(DeleteTripDialogTestTags.CANCEL_DELETE_BUTTON).performClick()

    // Selection should remain
    assertTrue(viewModel.uiState.value.selectedTrips.contains(trip1))
  }

  @Test
  fun deleteSelectedTripsRemovesTrip() {
    val viewModel = launchScreen(listOf(trip1, trip2))

    // Enter selection mode and select a trip
    composeTestRule
        .onNodeWithTag(MyTripsScreenTestTags.getTestTagForTrip(trip1))
        .performTouchInput { longClick() }

    // Click delete
    composeTestRule.onNodeWithTag(MyTripsScreenTestTags.DELETE_SELECTED_BUTTON).performClick()

    // Confirm deletion
    composeTestRule
        .onNodeWithTag(DeleteTripDialogTestTags.CONFIRM_DELETE_BUTTON)
        .performClick() // This triggers deleteSelectedTrips()

    // Verify selection cleared
    assertTrue(viewModel.uiState.value.selectedTrips.isEmpty())
    assertTrue(!viewModel.uiState.value.isSelectionMode)
  }

  @Test
  fun checkEditCurrentTripButtonDisplays() {
    launchScreen(listOf(trip1, trip2))

    composeTestRule
        .onNodeWithTag(MyTripsScreenTestTags.EDIT_CURRENT_TRIP_BUTTON)
        .assertIsDisplayed()
  }

  @Test
  fun checkEditCurrentTripButtonIsNotDisplayedWhenNoTrips() {
    launchScreen(emptyList())

    composeTestRule
        .onNodeWithTag(MyTripsScreenTestTags.EDIT_CURRENT_TRIP_BUTTON)
        .assertDoesNotExist()
  }

  @Test
  fun sortingByFavorites_placesFavoritesFirst() {
    val now = Timestamp.now()

    // Create trips
    val favoriteTrip =
        createTestTrip(
            uid = "fav",
            name = "Favorite Trip",
            ownerId = "ownerX",
            startDate = Timestamp(now.seconds + 3600, 0),
            endDate = Timestamp(now.seconds + 7200, 0))

    val nonFavoriteTrip =
        createTestTrip(
            uid = "nonfav",
            name = "Non-favorite Trip",
            ownerId = "ownerX",
            startDate = Timestamp(now.seconds + 3600, 0),
            endDate = Timestamp(now.seconds + 7200, 0))

    val fakeRepo = FakeTripsRepository(mutableListOf(nonFavoriteTrip, favoriteTrip))
    val fakeUserRepo = FakeUserRepository()
    fakeUserRepo.addUser(createTestUser(uid = "ownerX", name = "Owner X"))
    // Explicitly add the trip to favorites in the user repo
    runBlocking { fakeUserRepo.addFavoriteTrip("ownerX", favoriteTrip.uid) }

    val viewModel = MyTripsViewModel(userRepository = fakeUserRepo, tripsRepository = fakeRepo)

    composeTestRule.setContent { MyTripsScreen(myTripsViewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Open the sort dropdown and select "Favorites"
    composeTestRule.onNodeWithTag(SortMenuTestTags.SORT_DROPDOWN_MENU).performClick()
    composeTestRule.onNodeWithText(context.getString(R.string.favorites_first)).performClick()

    composeTestRule.waitForIdle()

    // Verify that the favorite trip is displayed before the non-favorite trip
    val favTripNode =
        composeTestRule
            .onNodeWithTag(MyTripsScreenTestTags.getTestTagForTrip(favoriteTrip))
            .fetchSemanticsNode()
    val nonFavTripNode =
        composeTestRule
            .onNodeWithTag(MyTripsScreenTestTags.getTestTagForTrip(nonFavoriteTrip))
            .fetchSemanticsNode()

    assertTrue(
        favTripNode.positionInRoot.y < nonFavTripNode.positionInRoot.y,
        "Favorite trip should appear above non-favorite trip when sorting by favorites")
  }

  @Test
  fun favoriteButtonAppearsInSelectionMode() {
    launchScreen(listOf(trip1, trip2))

    composeTestRule
        .onNodeWithTag(MyTripsScreenTestTags.getTestTagForTrip(trip1))
        .performTouchInput { longClick() }

    composeTestRule
        .onNodeWithTag(MyTripsScreenTestTags.FAVORITE_SELECTED_BUTTON)
        .assertIsDisplayed()
  }

  @Test
  fun favoriteSelectedTrips_togglesFavoriteStatus() {
    // We rely on standard trips (which are not in user favorites by default)
    val fakeRepo = FakeTripsRepository(mutableListOf(trip1, trip2))
    val fakeUserRepo = FakeUserRepository()

    val viewModel = MyTripsViewModel(userRepository = fakeUserRepo, tripsRepository = fakeRepo)

    composeTestRule.setContent { MyTripsScreen(myTripsViewModel = viewModel) }

    composeTestRule
        .onNodeWithTag(MyTripsScreenTestTags.getTestTagForTrip(trip1))
        .performTouchInput { longClick() }
    composeTestRule.onNodeWithTag(MyTripsScreenTestTags.getTestTagForTrip(trip2)).performClick()

    composeTestRule
        .onNodeWithTag(MyTripsScreenTestTags.FAVORITE_SELECTED_BUTTON)
        .assertIsDisplayed()

    composeTestRule.onNodeWithTag(MyTripsScreenTestTags.FAVORITE_SELECTED_BUTTON).performClick()

    composeTestRule.waitForIdle()

    assert(!viewModel.uiState.value.isSelectionMode)

    // Verify via User repository
    val updatedUser = runBlocking { fakeUserRepo.getCurrentUser() }
    assertTrue(updatedUser.favoriteTripsUids.contains(trip1.uid), "Trip 1 should be favorite")
    assertTrue(updatedUser.favoriteTripsUids.contains(trip2.uid), "Trip 2 should be favorite")
  }

  @Test
  fun pastTripsButton_clickNavigatesToPastTrips() {
    var pastTripsClicked = false
    val viewModel = MyTripsViewModel(tripsRepository = FakeTripsRepository())

    composeTestRule.setContent {
      MyTripsScreen(
          myTripsViewModel = viewModel,
          onPastTrips = { pastTripsClicked = true } // callback to test
          )
    }

    // Perform click on the Past Trips button
    composeTestRule.onNodeWithTag(MyTripsScreenTestTags.PAST_TRIPS_BUTTON).performClick()

    // Verify that the callback was triggered
    assertTrue(
        pastTripsClicked, "Clicking the Past Trips button should trigger the onPastTrips callback")
  }

  @Test
  fun displaysCollaboratorsOnTripCard() {
    // 1. Create a collaborator
    val collaborator =
        User(
            uid = "collab1",
            name = "Alice Collaborator",
            biography = "",
            email = "alice@test.com",
            profilePicUrl = "",
            preferences = emptyList(),
            friends = emptyList(),
            stats = UserStats(),
            pinnedTripsUids = emptyList(),
            pinnedPicturesUids = emptyList(),
            favoriteTripsUids = emptyList())

    // 2. Create a trip linked to this collaborator
    val sharedTrip =
        upcomingTrip.copy(
            uid = "sharedTrip",
            name = "Shared Adventure",
            collaboratorsId = listOf(collaborator.uid))

    // 3. Launch screen with the trip and user data
    launchScreen(listOf(sharedTrip), listOf(collaborator))

    composeTestRule.waitForIdle()

    // 4. Verify the trip card is displayed
    composeTestRule
        .onNodeWithTag(MyTripsScreenTestTags.getTestTagForTrip(sharedTrip))
        .assertIsDisplayed()

    // 5. Verify the collaborator's name (content description) is displayed
    composeTestRule
        .onNodeWithContentDescription("Alice Collaborator", useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun tripElement_showsCollaboratorIndicator() {
    val dummyTrip = createTestTrip(uid = "1", name = "Test Trip")
    val collaborators =
        listOf(
            TripsViewModel.CollaboratorUi("1", "User1", ""),
            TripsViewModel.CollaboratorUi("2", "User2", ""))

    val tripElementState = TripElementState(trip = dummyTrip, collaborators = collaborators)
    composeTestRule.setContent { TripElement(tripElementState = tripElementState, onClick = {}) }

    // Verify avatars are shown
    composeTestRule.onNodeWithContentDescription("User1").assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("User2").assertIsDisplayed()
    // Verify overflow text is NOT shown
    composeTestRule.onNodeWithText("+", substring = true).assertDoesNotExist()
  }

  @Test
  fun tripElement_showsOverflowIndicator_whenMoreThanThree() {
    val dummyTrip = createTestTrip(uid = "1", name = "Test Trip")
    val collaborators =
        listOf(
            TripsViewModel.CollaboratorUi("1", "User1", ""),
            TripsViewModel.CollaboratorUi("2", "User2", ""),
            TripsViewModel.CollaboratorUi("3", "User3", ""),
            TripsViewModel.CollaboratorUi("4", "User4", ""))

    val tripElementState = TripElementState(trip = dummyTrip, collaborators = collaborators)
    composeTestRule.setContent { TripElement(tripElementState = tripElementState, onClick = {}) }

    // Verify overflow text "+1" is displayed
    composeTestRule.onNodeWithText("+1").assertIsDisplayed()
  }
}
