package com.github.swent.swisstravel.ui.mytrips

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.*
import com.github.swent.swisstravel.ui.theme.SwissTravelTheme
import com.google.firebase.Timestamp
import kotlin.test.DefaultAsserter.assertTrue
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
    // No-op for testing
  }

  override fun getNewUid(): String = "fake-uid-${trips.size + 1}"
}

class MyTripsScreenEmulatorTest {

  @get:Rule val composeTestRule = createComposeRule()
  private val now = Timestamp.now()
  val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

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
    val fakeRepo = FakeTripsRepository(mutableListOf(currentTrip, upcomingTrip))
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
    val viewModel = MyTripsViewModel(FakeTripsRepository())

    composeTestRule.setContent { SwissTravelTheme { MyTripsScreen(myTripsViewModel = viewModel) } }

    composeTestRule.onNodeWithTag(MyTripsScreenTestTags.EMPTY_CURRENT_TRIP_MSG).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(MyTripsScreenTestTags.EMPTY_UPCOMING_TRIPS_MSG)
        .assertIsDisplayed()
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
    assert(clicked)
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
    assert(createClicked)
  }

  @Test
  fun addingTrip_updatesUpcomingTripsList() {
    val fakeRepo = FakeTripsRepository(mutableListOf(currentTrip))
    val viewModel = MyTripsViewModel(fakeRepo)

    composeTestRule.setContent { SwissTravelTheme { MyTripsScreen(myTripsViewModel = viewModel) } }

    // Before adding, no upcoming trips
    composeTestRule.onNodeWithTag(MyTripsScreenTestTags.UPCOMING_TRIPS).assertDoesNotExist()

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
                preferences = emptyList()))

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
                preferences = emptyList()))

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
                preferences = emptyList()))

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
    composeTestRule.onNodeWithTag(MyTripsScreenTestTags.SORT_DROPDOWN_MENU).performClick()
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
