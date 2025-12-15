package com.github.swent.swisstravel.ui.trips

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.swent.swisstravel.model.trip.*
import com.github.swent.swisstravel.ui.composable.DeleteDialogTestTags
import com.github.swent.swisstravel.ui.composable.TripListTestTags
import com.github.swent.swisstravel.utils.FakeTripsRepository
import com.github.swent.swisstravel.utils.FakeUserRepository
import com.github.swent.swisstravel.utils.InMemorySwissTravelTest
import com.google.firebase.Timestamp
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class PastTripsScreenEmulatorTest : InMemorySwissTravelTest() {

  @get:Rule val composeTestRule = createComposeRule()
  private val now = Timestamp.now()

  val pastTrip1 =
      trip1.copy(
          tripProfile =
              trip1.tripProfile.copy(
                  startDate = Timestamp(now.seconds - 7200, 0),
                  endDate = Timestamp(now.seconds - 3600, 0)))

  val pastTrip2 =
      trip2.copy(
          tripProfile =
              trip2.tripProfile.copy(
                  startDate = Timestamp(now.seconds - 10800, 0),
                  endDate = Timestamp(now.seconds - 7200, 0)))

  /** Helper to launch PastTripsScreen with custom trips */
  private fun launchScreen(vararg trips: Trip): PastTripsViewModel {
    val viewModel =
        PastTripsViewModel(
            userRepository = FakeUserRepository(),
            tripsRepository = FakeTripsRepository(trips.toMutableList()))
    composeTestRule.setContent { PastTripsScreen(pastTripsViewModel = viewModel) }
    return viewModel
  }

  @Test
  fun displaysPastTrips_correctly() {
    launchScreen(pastTrip1, pastTrip2)

    composeTestRule
        .onNodeWithTag(PastTripsScreenTestTags.getTestTagForTrip(pastTrip1))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(PastTripsScreenTestTags.getTestTagForTrip(pastTrip2))
        .assertIsDisplayed()
  }

  @Test
  fun emptyStateMessage_showsWhenNoTrips() {
    launchScreen()
    composeTestRule.onNodeWithTag(TripListTestTags.EMPTY_MESSAGE).assertIsDisplayed()
  }

  @Test
  fun longPress_entersSelectionMode() {
    launchScreen(pastTrip1)

    composeTestRule
        .onNodeWithTag(PastTripsScreenTestTags.getTestTagForTrip(pastTrip1))
        .performTouchInput { longClick() }

    composeTestRule
        .onNodeWithTag(PastTripsScreenTestTags.CANCEL_SELECTION_BUTTON)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(PastTripsScreenTestTags.DELETE_SELECTED_BUTTON)
        .assertIsDisplayed()
  }

  @Test
  fun selectAll_selectsAllTrips() {
    launchScreen(pastTrip1, pastTrip2)

    composeTestRule
        .onNodeWithTag(PastTripsScreenTestTags.getTestTagForTrip(pastTrip1))
        .performTouchInput { longClick() }

    composeTestRule.onNodeWithTag(PastTripsScreenTestTags.MORE_OPTIONS_BUTTON).performClick()
    composeTestRule.onNodeWithTag(PastTripsScreenTestTags.SELECT_ALL_BUTTON).performClick()

    composeTestRule
        .onNodeWithTag(
            TripElementTestTags.getTestTagForTripCheckbox(pastTrip1), useUnmergedTree = true)
        .assertIsOn()
    composeTestRule
        .onNodeWithTag(
            TripElementTestTags.getTestTagForTripCheckbox(pastTrip2), useUnmergedTree = true)
        .assertIsOn()
  }

  @Test
  fun deleteSelectedTrips_showsConfirmationAndCancels() {
    val viewModel = launchScreen(pastTrip1)

    composeTestRule
        .onNodeWithTag(PastTripsScreenTestTags.getTestTagForTrip(pastTrip1))
        .performTouchInput { longClick() }

    composeTestRule.onNodeWithTag(PastTripsScreenTestTags.DELETE_SELECTED_BUTTON).performClick()

    composeTestRule.onNodeWithTag(DeleteDialogTestTags.CONFIRM_DELETE_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(DeleteDialogTestTags.CANCEL_DELETE_BUTTON).performClick()

    assertTrue(viewModel.uiState.value.selectedTrips.contains(pastTrip1))
  }

  @Test
  fun favoriteSelectedTrips_togglesFavoriteStatus() {
    val fakeRepo = FakeTripsRepository(mutableListOf(pastTrip1, pastTrip2))
    val fakeUserRepo = FakeUserRepository()
    val viewModel = PastTripsViewModel(userRepository = fakeUserRepo, tripsRepository = fakeRepo)

    composeTestRule.setContent { PastTripsScreen(pastTripsViewModel = viewModel) }

    // Select trips
    composeTestRule
        .onNodeWithTag(PastTripsScreenTestTags.getTestTagForTrip(pastTrip1))
        .performTouchInput { longClick() }
    composeTestRule
        .onNodeWithTag(PastTripsScreenTestTags.getTestTagForTrip(pastTrip2))
        .performClick()

    composeTestRule.onNodeWithTag(PastTripsScreenTestTags.FAVORITE_SELECTED_BUTTON).performClick()

    composeTestRule.waitForIdle()
    assertTrue(viewModel.uiState.value.selectedTrips.isEmpty())

    // Verify via User object
    val currentUser = runBlocking { fakeUserRepo.getCurrentUser() }
    assertTrue(currentUser.favoriteTripsUids.contains(pastTrip1.uid))
    assertTrue(currentUser.favoriteTripsUids.contains(pastTrip2.uid))
  }
}
