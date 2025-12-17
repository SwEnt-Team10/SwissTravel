package com.github.swent.swisstravel.ui.trips

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.utils.FakeTripsRepository
import com.github.swent.swisstravel.utils.FakeUserRepository
import com.github.swent.swisstravel.utils.InMemorySwissTravelTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/** The tests were human tests passed into chatGPT when the screen changed. */
@OptIn(ExperimentalCoroutinesApi::class)
class SetCurrentTripScreenTests : InMemorySwissTravelTest() {

  @get:Rule val composeTestRule = createComposeRule()

  /**
   * Small data class to hold flags for test callbacks (e.g., to verify if the screen was closed).
   */
  private data class TestFlags(var screenClosed: Boolean = false)

  /** Launches the screen with a fake repository and returns the ViewModel. */
  private fun launchScreen(
      vararg trips: Trip,
      currentTripUid: String = "",
      flags: TestFlags = TestFlags()
  ): MyTripsViewModel {
    val fakeRepo = FakeTripsRepository(trips.toMutableList())
    val fakeUserRepo = FakeUserRepository()
    val viewModel =
        MyTripsViewModel(userRepository = FakeUserRepository(), tripsRepository = fakeRepo)

    val user = createTestUser(uid = "current", currentTrip = currentTripUid)
    fakeUserRepo.addUser(user)

    composeTestRule.setContent {
      SetCurrentTripScreen(
          viewModel = viewModel,
          onClose = { flags.screenClosed = true },
          isSelected = { it.uid == viewModel.uiState.value.currentTrip?.uid })
    }
    return viewModel
  }

  @Test
  fun screenDisplaysCorrectly() {
    composeTestRule.setContent { SetCurrentTripScreen(title = "Set Current Trip") }
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
    composeTestRule.onNodeWithTag(TripElementTestTags.getTestTagForTrip(trip2)).performTouchInput {
      longClick()
    }
    composeTestRule.waitForIdle()
    viewModel.refreshUIState()
    val uiState = viewModel.uiState.value

    assertEquals("2", uiState.currentTrip?.uid)
    assertEquals("2", uiState.currentTrip?.uid, "Trip 2 should now be current.")
  }
}
