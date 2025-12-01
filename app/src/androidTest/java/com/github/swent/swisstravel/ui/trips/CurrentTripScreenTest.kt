package com.github.swent.swisstravel.ui.trips

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.ui.currenttrip.CurrentTripScreen
import com.github.swent.swisstravel.ui.currenttrip.CurrentTripScreenTestTags
import com.github.swent.swisstravel.ui.trip.tripinfos.DailyViewScreenTestTags
import com.github.swent.swisstravel.ui.trip.tripinfos.TripInfoScreenTestTags
import com.google.firebase.Timestamp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.Test

/**
 * Tests for the [CurrentTripScreen] composable
 *
 * First, tests that the current trip is displayed when it exists, and that the create trip buttons
 * are not displayed. Second, tests that the current trip is not displayed when it does not exist,
 * and that the create trip buttons are displayed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CurrentTripScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  /**
   * Tests that the current trip is displayed on the current trip tab (when there is a current
   * trip), and also checks that the buttons to create a trip are not displayed.
   */
  @Test
  fun currentTripIsDisplayedWhenItExists() {
    val currentTrip =
        Trip(
            "2",
            "currentTrip",
            "ownerX",
            emptyList(),
            emptyList(),
            emptyList(),
            TripProfile(
                startDate = Timestamp(Timestamp.now().seconds + 7200, 0),
                endDate = Timestamp(Timestamp.now().seconds + 10800, 0),
                preferredLocations = emptyList(),
                preferences = emptyList()),
            isFavorite = false,
            isCurrentTrip = true)

    composeTestRule.setContent {
      val fakeRepo = FakeTripsRepository(mutableListOf(currentTrip))
      val viewModel = MyTripsViewModel(fakeRepo)
      CurrentTripScreen(isLoggedIn = true, myTripsViewModel = viewModel)
    }

    composeTestRule.onNodeWithTag(DailyViewScreenTestTags.TITLE).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CurrentTripScreenTestTags.CREATE_TRIP_BUTTON)
        .assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(CurrentTripScreenTestTags.CREATE_TRIP_TEXT).assertIsNotDisplayed()
  }

  /**
   * Tests that the current trip is not displayed on the current trip tab when there is no current
   * trip, and also checks that the buttons to create a trip are in fact displayed.
   */
  @Test
  fun currentTripIsNotDisplayedWhenItDoesNotExist() {
    val notCurrentTrip =
        Trip(
            "1",
            "currentTrip",
            "ownerX",
            emptyList(),
            emptyList(),
            emptyList(),
            TripProfile(
                startDate = Timestamp(Timestamp.now().seconds + 7200, 0),
                endDate = Timestamp(Timestamp.now().seconds + 10800, 0),
                preferredLocations = emptyList(),
                preferences = emptyList()),
            isFavorite = false,
            isCurrentTrip = false)

    composeTestRule.setContent {
      val fakeRepo = FakeTripsRepository(mutableListOf(notCurrentTrip))
      val viewModel = MyTripsViewModel(fakeRepo)
      CurrentTripScreen(isLoggedIn = true, myTripsViewModel = viewModel)
    }

    composeTestRule.onNodeWithTag(TripInfoScreenTestTags.TITLE).assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(CurrentTripScreenTestTags.CREATE_TRIP_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CurrentTripScreenTestTags.CREATE_TRIP_TEXT).assertIsDisplayed()
  }
}
