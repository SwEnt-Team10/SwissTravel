package com.android.swisstravel.ui.mytrips

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.ui.mytrips.MyTripsScreen
import com.github.swent.swisstravel.ui.mytrips.MyTripsScreenTestTags
import com.github.swent.swisstravel.ui.mytrips.MyTripsViewModel
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
}
