package com.android.swisstravel.ui.mytrips

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.swent.swisstravel.model.trip.*
import com.github.swent.swisstravel.ui.mytrips.MyTripsScreen
import com.github.swent.swisstravel.ui.mytrips.MyTripsScreenTestTags
import com.github.swent.swisstravel.ui.mytrips.MyTripsViewModel
import com.github.swent.swisstravel.ui.mytrips.TripElementTestTags
import com.github.swent.swisstravel.ui.theme.SwissTravelTheme
import com.google.firebase.Timestamp
import org.junit.Rule
import org.junit.Test

class MyTripsScreenEmulatorTest {

  @get:Rule val composeTestRule = createComposeRule()
  private val now = Timestamp.now()

  private val currentTrip =
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
              preferences = emptyList()))

  private val upcomingTrip =
      Trip(
          "2",
          "Upcoming Trip",
          "ownerX",
          emptyList(),
          emptyList(),
          emptyList(),
          TripProfile(
              startDate = Timestamp(now.seconds + 7200, 0),
              endDate = Timestamp(now.seconds + 10800, 0),
              preferredLocations = emptyList(),
              preferences = emptyList()))

  @Test
  fun displaysCurrentAndUpcomingTrips_usingRealViewModel() {
    val fakeRepo = FakeTripsRepository(listOf(currentTrip, upcomingTrip))
    val viewModel = MyTripsViewModel(fakeRepo)

    composeTestRule.setContent { SwissTravelTheme { MyTripsScreen(myTripsViewModel = viewModel) } }

    // Check current trip
    composeTestRule.onNodeWithTag(MyTripsScreenTestTags.CURRENT_TRIP_TITLE).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(MyTripsScreenTestTags.getTestTagForTrip(currentTrip))
        .assertIsDisplayed()

    // Check upcoming trip
    composeTestRule.onNodeWithTag(MyTripsScreenTestTags.UPCOMING_TRIPS_TITLE).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(MyTripsScreenTestTags.getTestTagForTrip(upcomingTrip))
        .assertIsDisplayed()
  }

  @Test
  fun displaysEmptyMessagesWhenNoTrips() {
    val viewModel = MyTripsViewModel(FakeTripsRepository(emptyList()))

    composeTestRule.setContent { SwissTravelTheme { MyTripsScreen(myTripsViewModel = viewModel) } }

    composeTestRule.onNodeWithTag(MyTripsScreenTestTags.EMPTY_CURRENT_TRIP_MSG).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(MyTripsScreenTestTags.EMPTY_UPCOMING_TRIPS_MSG)
        .assertIsDisplayed()
  }

  @Test
  fun pastTripsButton_clickTriggersCallback() {
    var clicked = false
    val viewModel = MyTripsViewModel(FakeTripsRepository(emptyList()))

    composeTestRule.setContent {
      SwissTravelTheme {
        MyTripsScreen(myTripsViewModel = viewModel, onPastTrips = { clicked = true })
      }
    }

    composeTestRule.onNodeWithTag(MyTripsScreenTestTags.PAST_TRIPS_BUTTON).performClick()
    assert(clicked)
  }

  private val trip1 =
      Trip(
          "1",
          "Trip 1",
          "user",
          emptyList(),
          emptyList(),
          emptyList(),
          TripProfile(
              startDate = Timestamp(now.seconds - 3600, 0),
              endDate = Timestamp(now.seconds + 3600, 0),
              preferredLocations = emptyList(),
              preferences = emptyList()))

  private val trip2 =
      Trip(
          "2",
          "Trip 2",
          "user",
          emptyList(),
          emptyList(),
          emptyList(),
          TripProfile(
              startDate = Timestamp(now.seconds + 7200, 0),
              endDate = Timestamp(now.seconds + 10800, 0),
              preferredLocations = emptyList(),
              preferences = emptyList()))

  /** Helper to launch screen with trips */
  private fun launchScreen(vararg trips: Trip): MyTripsViewModel {
    val viewModel = MyTripsViewModel(FakeTripsRepository(trips.toList()))
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
    composeTestRule.onNodeWithTag(MyTripsScreenTestTags.CONFIRM_DELETE_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MyTripsScreenTestTags.CANCEL_DELETE_BUTTON).assertIsDisplayed()

    // Cancel deletion
    composeTestRule.onNodeWithTag(MyTripsScreenTestTags.CANCEL_DELETE_BUTTON).performClick()

    // Selection should remain
    assert(viewModel.uiState.value.selectedTrips.contains(trip1))
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
        .onNodeWithTag(MyTripsScreenTestTags.CONFIRM_DELETE_BUTTON)
        .performClick() // This triggers deleteSelectedTrips()

    // Verify selection cleared
    assert(viewModel.uiState.value.selectedTrips.isEmpty())
    assert(!viewModel.uiState.value.isSelectionMode)
  }
}
