package com.github.swent.swisstravel.utils

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.github.swent.swisstravel.HttpClientProvider
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.model.user.PreferenceCategories
import com.github.swent.swisstravel.ui.composable.CounterTestTags
import com.github.swent.swisstravel.ui.composable.PreferenceSelectorTestTags
import com.github.swent.swisstravel.ui.composable.SortMenuTestTags
import com.github.swent.swisstravel.ui.composable.SortedTripListTestTags
import com.github.swent.swisstravel.ui.currenttrip.CurrentTripScreenTestTags
import com.github.swent.swisstravel.ui.navigation.NavigationTestTags
import com.github.swent.swisstravel.ui.profile.ProfileScreenTestTags
import com.github.swent.swisstravel.ui.tripcreation.ArrivalDepartureTestTags
import com.github.swent.swisstravel.ui.tripcreation.TripDateTestTags
import com.github.swent.swisstravel.ui.tripcreation.TripPreferencesTestTags
import com.github.swent.swisstravel.ui.tripcreation.TripTravelersTestTags
import com.github.swent.swisstravel.ui.trips.MyTripsScreenTestTags
import com.github.swent.swisstravel.ui.trips.SetCurrentTripScreenTestTags
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before

const val UI_WAIT_TIMEOUT = 15_000L
const val E2E_WAIT_TIMEOUT = 15_000L

/**
 * Base class for all SwissTravel tests, providing common setup and utility functions.
 *
 * It also handles automatic sign-in when required, which should be at all time.
 *
 * You should always make sure that the emulator is running before the tests, otherwise the tests
 * will fail.
 *
 * This test is mainly taken from the swent-EPFl repo.
 */
abstract class SwissTravelTest {

  // TODO : Implement the repository here
  // abstract fun createInitializedRepository(): SwissTravelRepository

  open fun initializeHTTPClient(): OkHttpClient = FakeHttpClient.getClient()

  //    val repository: SwissTravelRepository
  //        get() = SwissTravelRepository.repository

  val httpClient
    get() = HttpClientProvider.client

  val shouldSignInAnonymously: Boolean = FirebaseEmulator.isRunning

  val currentUser: FirebaseUser
    get() {
      return FirebaseEmulator.auth.currentUser!!
    }

  init {
    assert(FirebaseEmulator.isRunning) { "FirebaseEmulator must be running when running the tests" }
  }

  @Before
  // TODO : Set up repository when it is implemented
  open fun setUp() {
    // SwissTravelRepository.repository = createInitializedRepository()
    HttpClientProvider.client = initializeHTTPClient()
    if (shouldSignInAnonymously) {
      runTest { FirebaseEmulator.auth.signInAnonymously().await() }
    }
  }

  @After
  open fun tearDown() {
    if (FirebaseEmulator.isRunning) {
      FirebaseEmulator.auth.signOut()
      FirebaseEmulator.clearAuthEmulator()
    }
  }

  /** Two examples trips for testing purposes */
  private val now = Timestamp.now()

  val tripE2E1 = // TODO
      Trip(
          "e2e-trip-1", // This is wrong but needed to avoid conflicts in E2E tests
          "E2E Trip 1",
          "ownerE2E", // This is wrong but needed to avoid conflicts in E2E tests
          emptyList(),
          emptyList(),
          emptyList(),
          TripProfile(
              startDate = Timestamp(now.seconds - 7200, 0),
              endDate = Timestamp(now.seconds + 7200, 0),
              preferredLocations = emptyList(),
              preferences = emptyList()),
          isFavorite = false,
          isCurrentTrip = false)
  val trip1 =
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
              preferences = emptyList()),
          isFavorite = false,
          isCurrentTrip = false)

  val trip2 =
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
              preferences = emptyList()),
          isFavorite = false,
          isCurrentTrip = false)

  val tripList = listOf(trip1, trip2)

  @Composable fun getStringResource(id: Int): String = stringResource(id)

  // TODO : Declare ComposeTestRules here

  fun ComposeTestRule.checkMyTripsScreenIsDisplayed() {
    onNodeWithTag(MyTripsScreenTestTags.PAST_TRIPS_BUTTON).assertIsDisplayed()
    onNodeWithTag(SortedTripListTestTags.TITLE)
        .assertIsDisplayed()
        .assertTextContains("Upcoming Trips", substring = false, ignoreCase = true)
    onNodeWithTag(SortedTripListTestTags.SORT_DROPDOWN_MENU).assertIsDisplayed()
    onNodeWithTag(MyTripsScreenTestTags.CURRENT_TRIP_TITLE)
        .assertIsDisplayed()
        .assertTextContains("Current Trip", substring = false, ignoreCase = true)
  }

  // Can be used to check if there is no trip displayed as well
  fun ComposeTestRule.checkMyTripsScreenIsDisplayedWithNoCurrentTrips() {
    onNodeWithTag(MyTripsScreenTestTags.PAST_TRIPS_BUTTON).assertIsDisplayed()
    onNodeWithTag(SortedTripListTestTags.TITLE)
        .assertIsDisplayed()
        .assertTextContains("Upcoming Trips", substring = false, ignoreCase = true)
    onNodeWithTag(SortedTripListTestTags.SORT_DROPDOWN_MENU).assertIsDisplayed()
    onNodeWithTag(MyTripsScreenTestTags.CURRENT_TRIP_TITLE)
        .assertIsDisplayed()
        .assertTextContains("Current Trip", substring = false, ignoreCase = true)
    onNodeWithTag(MyTripsScreenTestTags.EDIT_CURRENT_TRIP_BUTTON).assertIsNotDisplayed()
    onNodeWithTag(MyTripsScreenTestTags.EMPTY_CURRENT_TRIP_MSG).assertIsDisplayed()
  }

  fun ComposeTestRule.checkMyTripsWithATripAsCurrent(tripList: List<Trip>) {
    onNodeWithTag(SortedTripListTestTags.TRIP_LIST).assertIsDisplayed()
    for (trip in tripList) {
      onNodeWithTag(MyTripsScreenTestTags.getTestTagForTrip(trip)).assertIsDisplayed()
    }
    onNodeWithTag(MyTripsScreenTestTags.PAST_TRIPS_BUTTON).assertIsDisplayed()
    onNodeWithTag(SortedTripListTestTags.TITLE)
        .assertIsDisplayed()
        .assertTextContains("Upcoming Trips", substring = false, ignoreCase = true)
    onNodeWithTag(SortedTripListTestTags.SORT_DROPDOWN_MENU).assertIsDisplayed()
    onNodeWithTag(MyTripsScreenTestTags.CURRENT_TRIP_TITLE)
        .assertIsDisplayed()
        .assertTextContains("Current Trip", substring = false, ignoreCase = true)
    onNodeWithTag(MyTripsScreenTestTags.EDIT_CURRENT_TRIP_BUTTON).assertIsDisplayed()
    onNodeWithTag(MyTripsScreenTestTags.EMPTY_CURRENT_TRIP_MSG).assertIsNotDisplayed()
  }

  fun ComposeTestRule.checkMyTripsInSelectionMode() {
    onNodeWithTag(MyTripsScreenTestTags.PAST_TRIPS_BUTTON).assertIsNotDisplayed()
    onNodeWithTag(MyTripsScreenTestTags.MORE_OPTIONS_BUTTON).assertIsDisplayed()
    onNodeWithTag(MyTripsScreenTestTags.DELETE_SELECTED_BUTTON).assertIsDisplayed()
    onNodeWithTag(MyTripsScreenTestTags.CANCEL_SELECTION_BUTTON).assertIsDisplayed()
    onNodeWithTag(MyTripsScreenTestTags.FAVORITE_SELECTED_BUTTON).assertIsDisplayed()
  }

  fun ComposeTestRule.checkMyTripsNotInSelectionMode() {
    onNodeWithTag(MyTripsScreenTestTags.PAST_TRIPS_BUTTON).assertIsDisplayed()
    onNodeWithTag(MyTripsScreenTestTags.MORE_OPTIONS_BUTTON).assertIsNotDisplayed()
    onNodeWithTag(MyTripsScreenTestTags.DELETE_SELECTED_BUTTON).assertIsNotDisplayed()
    onNodeWithTag(MyTripsScreenTestTags.CANCEL_SELECTION_BUTTON).assertIsNotDisplayed()
    onNodeWithTag(MyTripsScreenTestTags.FAVORITE_SELECTED_BUTTON).assertIsNotDisplayed()
  }

  fun ComposeTestRule.checkMyTripsScreenIsNotDisplayed() {
    onNodeWithTag(MyTripsScreenTestTags.PAST_TRIPS_BUTTON).assertDoesNotExist()
    onNodeWithTag(SortedTripListTestTags.TITLE).assertDoesNotExist()
    onNodeWithTag(SortedTripListTestTags.SORT_DROPDOWN_MENU).assertDoesNotExist()
    onNodeWithTag(MyTripsScreenTestTags.CURRENT_TRIP_TITLE).assertDoesNotExist()
  }

  fun ComposeTestRule.checkCurrentTripScreenIsDisplayed() {
    onNodeWithTag(CurrentTripScreenTestTags.CREATE_TRIP_TEXT)
        .assertIsDisplayed()
        .assertTextContains("Create a trip", substring = false, ignoreCase = true)
    onNodeWithTag(CurrentTripScreenTestTags.CREATE_TRIP_BUTTON).assertIsDisplayed()
  }

  fun ComposeTestRule.checkCurrentTripScreenIsNotDisplayed() {
    onNodeWithTag(CurrentTripScreenTestTags.CREATE_TRIP_TEXT).assertDoesNotExist()
    onNodeWithTag(CurrentTripScreenTestTags.CREATE_TRIP_BUTTON).assertDoesNotExist()
  }

  fun ComposeTestRule.checkProfileScreenIsDisplayed() {
    onNodeWithTag(ProfileScreenTestTags.DISPLAY_NAME).assertIsDisplayed()
    onNodeWithTag(ProfileScreenTestTags.PREFERENCES_LIST).assertIsDisplayed()
    onNodeWithTag(ProfileScreenTestTags.EMAIL).assertIsDisplayed()
    onNodeWithTag(ProfileScreenTestTags.GREETING).assertIsDisplayed()
  }

  fun ComposeTestRule.checkProfileScreenIsNotDisplayed() {
    onNodeWithTag(ProfileScreenTestTags.PREFERENCES_LIST).assertDoesNotExist()
    onNodeWithTag(ProfileScreenTestTags.DISPLAY_NAME).assertDoesNotExist()
    onNodeWithTag(ProfileScreenTestTags.EMAIL).assertDoesNotExist()
    onNodeWithTag(ProfileScreenTestTags.GREETING).assertDoesNotExist()
  }

  fun ComposeTestRule.checkNavigationMenuIsDisplayed() {
    onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()
  }

  fun ComposeTestRule.checkTopBarIsDisplayed() {
    onNodeWithTag(NavigationTestTags.TOP_BAR).assertIsDisplayed()
    onNodeWithTag(NavigationTestTags.TOP_BAR_BUTTON).assertIsDisplayed()
  }

  fun ComposeTestRule.checkTopBarIsNotDisplayed() {
    onNodeWithTag(NavigationTestTags.TOP_BAR).assertDoesNotExist()
    onNodeWithTag(NavigationTestTags.TOP_BAR_BUTTON).assertDoesNotExist()
  }

  fun ComposeTestRule.checkSortedTripListNotEmptyIsDisplayed() {
    onNodeWithTag(SortedTripListTestTags.SORTED_TRIP_LIST).assertIsDisplayed()
    onNodeWithTag(SortedTripListTestTags.TITLE_BUTTON_ROW).assertIsDisplayed()
    onNodeWithTag(SortedTripListTestTags.SORT_DROPDOWN_MENU).assertIsDisplayed()
    onNodeWithTag(SortedTripListTestTags.TITLE).assertIsDisplayed()
    onNodeWithTag(SortedTripListTestTags.TRIP_LIST).assertIsDisplayed()
  }

  fun ComposeTestRule.checkSetCurrentTripIsDisplayed() {
    onNodeWithTag(SetCurrentTripScreenTestTags.TOP_BAR).assertIsDisplayed()
    onNodeWithTag(SetCurrentTripScreenTestTags.TOP_BAR_TITLE).assertIsDisplayed()
    onNodeWithTag(SetCurrentTripScreenTestTags.TOP_BAR_CLOSE_BUTTON).assertIsDisplayed()
    onNodeWithTag(SortMenuTestTags.SORT_DROPDOWN_MENU).assertIsDisplayed()
  }

  fun ComposeTestRule.checkTripDateScreenIsDisplayed() {
    onNodeWithTag(TripDateTestTags.TRIP_DATE_SCREEN).assertIsDisplayed()
    onNodeWithTag(TripDateTestTags.NEXT).assertIsDisplayed()
  }

  fun ComposeTestRule.checkTripTravelersIsDisplayed(adultsLabel: String, childrenLabel: String) {
    onNodeWithTag(TripTravelersTestTags.TRIP_TRAVELERS_SCREEN).assertIsDisplayed()
    onNodeWithTag(TripTravelersTestTags.NEXT).assertIsDisplayed()
    onNodeWithTag(adultsLabel + CounterTestTags.COUNTER).assertIsDisplayed()
    onNodeWithTag(adultsLabel + CounterTestTags.INCREMENT).assertIsDisplayed()
    onNodeWithTag(adultsLabel + CounterTestTags.DECREMENT).assertIsDisplayed()
    onNodeWithTag(adultsLabel + CounterTestTags.DECREMENT).assertIsDisplayed()
    onNodeWithTag(childrenLabel + CounterTestTags.COUNTER).assertIsDisplayed()
    onNodeWithTag(childrenLabel + CounterTestTags.INCREMENT).assertIsDisplayed()
    onNodeWithTag(childrenLabel + CounterTestTags.DECREMENT).assertIsDisplayed()
    onNodeWithTag(childrenLabel + CounterTestTags.DECREMENT).assertIsDisplayed()
  }

  fun ComposeTestRule.checkTripPreferencesIsDisplayed() {
    onNodeWithTag(TripPreferencesTestTags.TRIP_PREFERENCES_SCREEN).assertIsDisplayed()
    onNodeWithTag(TripPreferencesTestTags.TRIP_PREFERENCES_TITLE).assertIsDisplayed()
    onNodeWithTag(TripPreferencesTestTags.TRIP_PREFERENCE_CONTENT).assertIsDisplayed()
    onNodeWithTag(TripPreferencesTestTags.TRIP_PREFERENCES_SCREEN).assertExists()
    for (category in
        PreferenceCategories.Category.values().filter {
          it != PreferenceCategories.Category.DEFAULT
        }) {
      val tag = PreferenceSelectorTestTags.getTestTagCategory(category)
      onNodeWithTag(PreferenceSelectorTestTags.PREFERENCE_SELECTOR)
          .performScrollToNode(hasTestTag(tag))
      onNodeWithTag(tag).assertIsDisplayed()
    }
  }

  fun ComposeTestRule.checkArrivalDepartureScreenIsDisplayed() {
    onNodeWithTag(ArrivalDepartureTestTags.DEPARTURE_TEXTFIELD).assertIsDisplayed()
    onNodeWithTag(ArrivalDepartureTestTags.ARRIVAL_TEXTFIELD).assertIsDisplayed()
    onNodeWithTag(ArrivalDepartureTestTags.NEXT_BUTTON).assertIsDisplayed()
  }

  fun ComposeTestRule.performClickPreferences(preferenceList: List<Preference>) {
    for (preference in preferenceList) {
      val tag = PreferenceSelectorTestTags.getTestTagButton(preference)
      onNodeWithTag(PreferenceSelectorTestTags.PREFERENCE_SELECTOR)
          .performScrollToNode(hasTestTag(tag))
      onNodeWithTag(tag).assertIsDisplayed()
      onNodeWithTag(tag).performClick()
    }
  }

  fun <A : ComponentActivity> AndroidComposeTestRule<ActivityScenarioRule<A>, A>
      .checkActivityStateOnPressBack(shouldFinish: Boolean) {
    activityRule.scenario.onActivity { activity ->
      activity.onBackPressedDispatcher.onBackPressed()
    }
    waitUntil { activity.isFinishing == shouldFinish }
    assertEquals(shouldFinish, activity.isFinishing)
  }

  // TODO : Create helper/companions functions here

}
