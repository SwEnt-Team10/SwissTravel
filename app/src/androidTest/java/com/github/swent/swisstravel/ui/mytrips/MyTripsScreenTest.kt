package com.github.swent.swisstravel.ui.mytrips

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.*
import com.github.swent.swisstravel.ui.composable.DeleteTripDialogTestTags
import com.github.swent.swisstravel.ui.composable.SortedTripListTestTags
import com.github.swent.swisstravel.ui.theme.SwissTravelTheme
import com.github.swent.swisstravel.utils.SwissTravelTest
import com.google.firebase.Timestamp
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

/** Fake TripsRepository to feed the ViewModel without touching Firestore. */
class FakeTripsRepository(private val trips: MutableList<Trip> = mutableListOf()) :
    TripsRepository {
  override suspend fun getAllTrips(): List<Trip> = trips

  override suspend fun getTrip(tripId: String): Trip {
    return trips.find { it.uid == tripId } ?: throw Exception("Trip not found: $tripId")
  }

  override suspend fun addTrip(trip: Trip) {
    trips.add(trip)
  }

  override suspend fun deleteTrip(tripId: String) {
    trips.removeIf { it.uid == tripId }
  }

  override suspend fun editTrip(tripId: String, updatedTrip: Trip) {
    trips.removeIf { it.uid == tripId }
    trips.add(updatedTrip)
  }

  override fun getNewUid(): String = "fake-uid-${trips.size + 1}"
}

class MyTripsScreenEmulatorTest : SwissTravelTest() {

  @get:Rule val composeTestRule = createComposeRule()
  private val now = Timestamp.now()
  val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

  private val currentTrip = trip1.copy(isCurrentTrip = true)

  private val upcomingTrip = trip2

  @Test
  fun displaysCurrentAndUpcomingTrips_usingRealViewModel() {
    val fakeRepo = FakeTripsRepository(mutableListOf(currentTrip, upcomingTrip))
    val viewModel = MyTripsViewModel(fakeRepo)

    composeTestRule.setContent { SwissTravelTheme { MyTripsScreen(myTripsViewModel = viewModel) } }

    // Check current trip
    composeTestRule.onNodeWithTag(MyTripsScreenTestTags.CURRENT_TRIP_TITLE).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(MyTripsScreenTestTags.getTestTagForTrip(currentTrip))
        .assertIsDisplayed()

    // Check upcoming trip
    composeTestRule.checkSortedTripListIsDisplayed()
    composeTestRule
        .onNodeWithTag(MyTripsScreenTestTags.getTestTagForTrip(upcomingTrip))
        .assertIsDisplayed()
  }

  @Test
  fun displaysEmptyMessagesWhenNoTrips() {
    val viewModel = MyTripsViewModel(FakeTripsRepository())

    composeTestRule.setContent { SwissTravelTheme { MyTripsScreen(myTripsViewModel = viewModel) } }

    composeTestRule.onNodeWithTag(MyTripsScreenTestTags.EMPTY_CURRENT_TRIP_MSG).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SortedTripListTestTags.EMPTY_MESSAGE).assertIsDisplayed()
  }

  @Test
  fun pastTripsButton_clickTriggersCallback() {
    var clicked = false
    val viewModel = MyTripsViewModel(FakeTripsRepository())

    composeTestRule.setContent {
      SwissTravelTheme {
        MyTripsScreen(myTripsViewModel = viewModel, onPastTrips = { clicked = true })
      }
    }

    composeTestRule.onNodeWithTag(MyTripsScreenTestTags.PAST_TRIPS_BUTTON).performClick()
    assertTrue(clicked)
  }

  @Test
  fun addTripButton_triggersAddTripCallback() {
    var createClicked = false
    val viewModel = MyTripsViewModel(FakeTripsRepository())

    composeTestRule.setContent {
      SwissTravelTheme {
        MyTripsScreen(myTripsViewModel = viewModel, onCreateTrip = { createClicked = true })
      }
    }

    composeTestRule.onNodeWithTag(MyTripsScreenTestTags.CREATE_TRIP_BUTTON).performClick()
    assertTrue(createClicked)
  }

  @Test
  fun addingTrip_updatesUpcomingTripsList() {
    val fakeRepo = FakeTripsRepository(mutableListOf(currentTrip))
    val viewModel = MyTripsViewModel(fakeRepo)

    composeTestRule.setContent { SwissTravelTheme { MyTripsScreen(myTripsViewModel = viewModel) } }

    // Before adding, no upcoming trips
    composeTestRule.onNodeWithTag(SortedTripListTestTags.TRIP_LIST).assertDoesNotExist()

    // Add a new upcoming trip inside a coroutine
    val newUpcomingTrip =
        Trip(
            "3",
            "New Upcoming",
            "ownerX",
            emptyList(),
            emptyList(),
            emptyList(),
            TripProfile(
                startDate = Timestamp(now.seconds + 7200, 0),
                endDate = Timestamp(now.seconds + 10800, 0),
                preferredLocations = emptyList(),
                preferences = emptyList()),
            isFavorite = false,
            isCurrentTrip = false)

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
        Trip(
            "a",
            "Alpha",
            "ownerX",
            emptyList(),
            emptyList(),
            emptyList(),
            TripProfile(
                startDate = Timestamp(now.seconds + 10800, 0), // later
                endDate = Timestamp(now.seconds + 14400, 0),
                preferredLocations = emptyList(),
                preferences = emptyList()),
            isFavorite = false,
            isCurrentTrip = false)

    val tripB =
        Trip(
            "b",
            "Beta",
            "ownerX",
            emptyList(),
            emptyList(),
            emptyList(),
            TripProfile(
                startDate = Timestamp(now.seconds + 7200, 0), // earlier
                endDate = Timestamp(now.seconds + 10800, 0),
                preferredLocations = emptyList(),
                preferences = emptyList()),
            isFavorite = false,
            isCurrentTrip = false)

    val fakeRepo = FakeTripsRepository(mutableListOf(tripA, tripB))
    val viewModel = MyTripsViewModel(fakeRepo)

    composeTestRule.setContent { SwissTravelTheme { MyTripsScreen(myTripsViewModel = viewModel) } }

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
    composeTestRule.onNodeWithTag(SortedTripListTestTags.SORT_DROPDOWN_MENU).performClick()
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

  /** Helper to launch screen with trips */
  private fun launchScreen(vararg trips: Trip): MyTripsViewModel {
    val viewModel = MyTripsViewModel(FakeTripsRepository(trips.toMutableList()))
    composeTestRule.setContent { SwissTravelTheme { MyTripsScreen(myTripsViewModel = viewModel) } }
    return viewModel
  }

  @Test
  fun longPressTripEntersSelectionMode() {
    launchScreen(trip1, trip2)

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
    launchScreen(trip1, trip2)

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
    val viewModel = launchScreen(trip1, trip2)

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
    val viewModel = launchScreen(trip1, trip2)

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
    launchScreen(trip1, trip2)

    composeTestRule
        .onNodeWithTag(MyTripsScreenTestTags.EDIT_CURRENT_TRIP_BUTTON)
        .assertIsDisplayed()
  }

  @Test
  fun checkEditCurrentTripButtonIsNotDisplayedWhenNoTrips() {
    launchScreen()

    composeTestRule
        .onNodeWithTag(MyTripsScreenTestTags.EDIT_CURRENT_TRIP_BUTTON)
        .assertDoesNotExist()
  }

  @Test
  fun sortingByFavorites_placesFavoritesFirst() {
    val now = Timestamp.now()

    // Create trips with different favorite states
    val favoriteTrip =
        Trip(
            uid = "fav",
            name = "Favorite Trip",
            ownerId = "ownerX",
            locations = emptyList(),
            routeSegments = emptyList(),
            activities = emptyList(),
            tripProfile =
                TripProfile(
                    startDate = Timestamp(now.seconds + 3600, 0),
                    endDate = Timestamp(now.seconds + 7200, 0),
                    preferredLocations = emptyList(),
                    preferences = emptyList()),
            isFavorite = true,
            isCurrentTrip = false)

    val nonFavoriteTrip =
        Trip(
            uid = "nonfav",
            name = "Non-Favorite Trip",
            ownerId = "ownerX",
            locations = emptyList(),
            routeSegments = emptyList(),
            activities = emptyList(),
            tripProfile =
                TripProfile(
                    startDate = Timestamp(now.seconds + 3600, 0),
                    endDate = Timestamp(now.seconds + 7200, 0),
                    preferredLocations = emptyList(),
                    preferences = emptyList()),
            isFavorite = false,
            isCurrentTrip = false)

    val fakeRepo = FakeTripsRepository(mutableListOf(nonFavoriteTrip, favoriteTrip))
    val viewModel = MyTripsViewModel(fakeRepo)

    composeTestRule.setContent { SwissTravelTheme { MyTripsScreen(myTripsViewModel = viewModel) } }

    composeTestRule.waitForIdle()

    // Open the sort dropdown and select "Favorites"
    composeTestRule.onNodeWithTag(SortedTripListTestTags.SORT_DROPDOWN_MENU).performClick()
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
    launchScreen(trip1, trip2)

    composeTestRule
        .onNodeWithTag(MyTripsScreenTestTags.getTestTagForTrip(trip1))
        .performTouchInput { longClick() }

    composeTestRule
        .onNodeWithTag(MyTripsScreenTestTags.FAVORITE_SELECTED_BUTTON)
        .assertIsDisplayed()
  }

  @Test
  fun favoriteSelectedTrips_togglesFavoriteStatus() {
    val fakeRepo =
        FakeTripsRepository(
            mutableListOf(trip1.copy(isFavorite = false), trip2.copy(isFavorite = false)))
    val viewModel = MyTripsViewModel(fakeRepo)

    composeTestRule.setContent { SwissTravelTheme { MyTripsScreen(myTripsViewModel = viewModel) } }

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

    val updatedTrips = runBlocking { fakeRepo.getAllTrips() }
    assertTrue(updatedTrips.all { it.isFavorite }, "All selected trips should now be favorites")
  }

  @Test
  fun pastTripsButton_clickNavigatesToPastTrips() {
    var pastTripsClicked = false
    val viewModel = MyTripsViewModel(FakeTripsRepository())

    composeTestRule.setContent {
      SwissTravelTheme {
        MyTripsScreen(
            myTripsViewModel = viewModel,
            onPastTrips = { pastTripsClicked = true } // callback to test
            )
      }
    }

    // Perform click on the Past Trips button
    composeTestRule.onNodeWithTag(MyTripsScreenTestTags.PAST_TRIPS_BUTTON).performClick()

    // Verify that the callback was triggered
    assertTrue(
        pastTripsClicked, "Clicking the Past Trips button should trigger the onPastTrips callback")
  }
}
