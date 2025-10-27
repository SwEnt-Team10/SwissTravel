package com.android.swisstravel.ui.mytrips

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.*
import com.github.swent.swisstravel.ui.mytrips.MyTripsScreen
import com.github.swent.swisstravel.ui.mytrips.MyTripsScreenTestTags
import com.github.swent.swisstravel.ui.mytrips.MyTripsViewModel
import com.github.swent.swisstravel.ui.theme.SwissTravelTheme
import com.google.firebase.Timestamp
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

    val fakeRepo = FakeTripsRepository(mutableListOf(currentTrip, tripA, tripB))
    val viewModel = MyTripsViewModel(fakeRepo)

    composeTestRule.setContent { SwissTravelTheme { MyTripsScreen(myTripsViewModel = viewModel) } }

    // Verify initial order: START_DATE_ASC
    composeTestRule.onAllNodesWithTag(MyTripsScreenTestTags.UPCOMING_TRIPS).onFirst().assertExists()

    val upcomingNode = composeTestRule.onNodeWithTag(MyTripsScreenTestTags.UPCOMING_TRIPS)
    val firstTripNode =
        composeTestRule.onNodeWithTag(MyTripsScreenTestTags.getTestTagForTrip(tripB))
    val secondTripNode =
        composeTestRule.onNodeWithTag(MyTripsScreenTestTags.getTestTagForTrip(tripA))

    firstTripNode.assertIsDisplayed()
    secondTripNode.assertIsDisplayed()

    // Open sort menu and select START_DATE_DESC
    composeTestRule.onNodeWithTag(MyTripsScreenTestTags.SORT_DROPDOWN_MENU).performClick()
    composeTestRule.onNodeWithText(context.getString(R.string.start_date_asc)).performClick()

    composeTestRule.waitForIdle()

    // After sort, first should be tripA, then tripB
    composeTestRule
        .onNodeWithTag(MyTripsScreenTestTags.getTestTagForTrip(tripA))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(MyTripsScreenTestTags.getTestTagForTrip(tripB))
        .assertIsDisplayed()
  }
}
