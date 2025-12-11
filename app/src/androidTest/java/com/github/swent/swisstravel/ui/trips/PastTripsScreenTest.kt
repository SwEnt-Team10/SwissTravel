package com.github.swent.swisstravel.ui.trips

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.github.swent.swisstravel.model.trip.*
import com.github.swent.swisstravel.ui.composable.DeleteTripDialogTestTags
import com.github.swent.swisstravel.ui.composable.SortedTripListTestTags
import com.github.swent.swisstravel.ui.theme.SwissTravelTheme
import com.github.swent.swisstravel.utils.InMemorySwissTravelTest
import com.google.firebase.Timestamp
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

/** Fake repository for past trips tests */
class FakePastTripsRepository(private val trips: MutableList<Trip> = mutableListOf()) :
    TripsRepository {
  override suspend fun getAllTrips(): List<Trip> = trips

  override suspend fun getTrip(tripId: String): Trip =
      trips.find { it.uid == tripId } ?: throw Exception("Trip not found: $tripId")

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

  override suspend fun removeCollaborator(tripId: String, userId: String) {
    // no-op
  }

  override suspend fun shareTripWithUsers(tripId: String, userIds: List<String>) {
    // no-op
  }
}

/** Tests for the past trips screen. */
class PastTripsScreenEmulatorTest : InMemorySwissTravelTest() {

  @get:Rule val composeTestRule = createComposeRule()
  private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
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
            tripsRepository = FakePastTripsRepository(trips.toMutableList()))
    composeTestRule.setContent {
      SwissTravelTheme { PastTripsScreen(pastTripsViewModel = viewModel) }
    }
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
    composeTestRule.onNodeWithTag(SortedTripListTestTags.EMPTY_MESSAGE).assertIsDisplayed()
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

    composeTestRule
        .onNodeWithTag(DeleteTripDialogTestTags.CONFIRM_DELETE_BUTTON)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(DeleteTripDialogTestTags.CANCEL_DELETE_BUTTON).performClick()

    assertTrue(viewModel.uiState.value.selectedTrips.contains(pastTrip1))
  }

  @Test
  fun favoriteSelectedTrips_togglesFavoriteStatus() {
    val fakeRepo = FakePastTripsRepository(mutableListOf(pastTrip1, pastTrip2))
    val fakeUserRepo = FakeUserRepository()
    val viewModel = PastTripsViewModel(userRepository = fakeUserRepo, tripsRepository = fakeRepo)

    composeTestRule.setContent {
      SwissTravelTheme { PastTripsScreen(pastTripsViewModel = viewModel) }
    }

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
