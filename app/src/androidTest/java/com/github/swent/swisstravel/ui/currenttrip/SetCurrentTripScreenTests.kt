package com.github.swent.swisstravel.ui.currenttrip

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.ui.mytrips.FakeTripsRepository
import com.github.swent.swisstravel.ui.mytrips.MyTripsViewModel
import com.github.swent.swisstravel.ui.mytrips.TripElementTestTags
import com.github.swent.swisstravel.ui.theme.SwissTravelTheme
import com.github.swent.swisstravel.utils.SwissTravelTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/** The tests were human tests passed into chatGPT when the screen changed. */
@OptIn(ExperimentalCoroutinesApi::class)
class SetCurrentTripScreenTests : SwissTravelTest() {

  @get:Rule val composeTestRule = createComposeRule()

  /**
   * Small data class to hold flags for test callbacks (e.g., to verify if the screen was closed).
   */
  private data class TestFlags(var screenClosed: Boolean = false)

  /** Launches the screen with a fake repository and returns the ViewModel. */
  private fun launchScreen(vararg trips: Trip, flags: TestFlags = TestFlags()): MyTripsViewModel {
    val fakeRepo = FakeTripsRepository(trips.toMutableList())
    val viewModel = MyTripsViewModel(fakeRepo)

    composeTestRule.setContent {
      SwissTravelTheme {
        SetCurrentTripScreen(
            viewModel = viewModel,
            onClose = { flags.screenClosed = true },
            isSelected = { it.isCurrentTrip })
      }
    }
    return viewModel
  }

  @Test
  fun screenDisplaysCorrectly() {
    composeTestRule.setContent {
      SwissTravelTheme { SetCurrentTripScreen(title = "Set Current Trip") }
    }
    composeTestRule.checkSetCurrentTripIsDisplayed()
  }

  @Test
  fun clickingTripSetsItAsCurrent() = runTest {
    val viewModel = launchScreen(trip1, trip2)

    // Perform click on trip2
    composeTestRule.onNodeWithTag(TripElementTestTags.getTestTagForTrip(trip2)).performClick()

    // Wait for coroutines to finish
    composeTestRule.waitForIdle()

    // Refresh UI from repository
    viewModel.refreshUIState()
    composeTestRule.waitForIdle()

    val uiState = viewModel.uiState.value

    assertEquals(trip2.uid, uiState.currentTrip?.uid, "Trip 2 should now be current.")
    assertTrue(uiState.currentTrip?.isCurrentTrip == true, "Current trip flag should be true.")
    assertTrue(
        uiState.upcomingTrips.none { it.isCurrentTrip }, "No other trip should be marked current.")
  }

  @Test
  fun clickingCloseButtonTriggersCloseCallback() {
    val flags = TestFlags()
    launchScreen(trip1, trip2, flags = flags)

    composeTestRule.onNodeWithTag(SetCurrentTripScreenTestTags.TOP_BAR_CLOSE_BUTTON).performClick()

    assertTrue(flags.screenClosed, "Close callback was not called.")
  }

  @Test
  fun selectingMultipleTripsStillSetsOnlyOneAsCurrent() = runTest {
    val viewModel = launchScreen(trip1, trip2)

    // Click trip1
    composeTestRule.onNodeWithTag(TripElementTestTags.getTestTagForTrip(trip1)).performClick()
    composeTestRule.waitForIdle()
    viewModel.refreshUIState()
    assertEquals("1", viewModel.uiState.value.currentTrip?.uid)

    // Click trip2
    composeTestRule.onNodeWithTag(TripElementTestTags.getTestTagForTrip(trip2)).performClick()
    composeTestRule.waitForIdle()
    viewModel.refreshUIState()
    val uiState = viewModel.uiState.value

    assertEquals("2", uiState.currentTrip?.uid, "Trip 2 should now be current.")
    assertTrue(uiState.upcomingTrips.none { it.isCurrentTrip && it.uid != "2" })
  }
}
