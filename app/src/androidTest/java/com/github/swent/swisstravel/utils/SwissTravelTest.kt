package com.github.swent.swisstravel.utils

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.github.swent.swisstravel.HttpClientProvider
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.trip.TripsRepositoryProvider
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.model.user.PreferenceCategories
import com.github.swent.swisstravel.model.user.displayStringRes
import com.github.swent.swisstravel.ui.activities.LikedActivitiesScreenTestTags
import com.github.swent.swisstravel.ui.activities.SwipeActivitiesScreenTestTags
import com.github.swent.swisstravel.ui.authentication.LandingScreenTestTags
import com.github.swent.swisstravel.ui.composable.BackButtonTestTag
import com.github.swent.swisstravel.ui.composable.CounterTestTags
import com.github.swent.swisstravel.ui.composable.ErrorScreenTestTags
import com.github.swent.swisstravel.ui.composable.PreferenceSelectorTestTags
import com.github.swent.swisstravel.ui.composable.SortMenuTestTags
import com.github.swent.swisstravel.ui.composable.SortedTripListTestTags
import com.github.swent.swisstravel.ui.composable.TripListTestTags
import com.github.swent.swisstravel.ui.currenttrip.CurrentTripScreenTestTags
import com.github.swent.swisstravel.ui.geocoding.LocationTextTestTags
import com.github.swent.swisstravel.ui.navigation.NavigationTestTags
import com.github.swent.swisstravel.ui.profile.ProfileScreenTestTags
import com.github.swent.swisstravel.ui.profile.ProfileSettingsScreenTestTags
import com.github.swent.swisstravel.ui.profile.selectpinnedpictures.SelectPinnedPicturesScreenTestTags.ADD_PICTURE_BUTTON
import com.github.swent.swisstravel.ui.profile.selectpinnedpictures.SelectPinnedPicturesScreenTestTags.LOADING_INDICATOR
import com.github.swent.swisstravel.ui.profile.selectpinnedpictures.SelectPinnedPicturesScreenTestTags.MAIN_SCREEN
import com.github.swent.swisstravel.ui.profile.selectpinnedpictures.SelectPinnedPicturesScreenTestTags.SAVE_BUTTON
import com.github.swent.swisstravel.ui.profile.selectpinnedpictures.SelectPinnedPicturesScreenTestTags.VERTICAL_GRID
import com.github.swent.swisstravel.ui.profile.selectpinnedtrips.SelectPinnedTripsScreenTestTags
import com.github.swent.swisstravel.ui.trip.edittrip.EditTripScreenTestTags
import com.github.swent.swisstravel.ui.trip.tripinfos.TripInfoScreenTestTags
import com.github.swent.swisstravel.ui.trip.tripinfos.photos.AddPhotosScreenTestTags
import com.github.swent.swisstravel.ui.trip.tripinfos.photos.EditPhotosScreenTestTags
import com.github.swent.swisstravel.ui.tripcreation.ArrivalDepartureTestTags
import com.github.swent.swisstravel.ui.tripcreation.TripDateTestTags
import com.github.swent.swisstravel.ui.tripcreation.TripFirstDestinationsTestTags
import com.github.swent.swisstravel.ui.tripcreation.TripPreferenceIconTestTags
import com.github.swent.swisstravel.ui.tripcreation.TripPreferencesTestTags
import com.github.swent.swisstravel.ui.tripcreation.TripSummaryTestTags
import com.github.swent.swisstravel.ui.tripcreation.TripTravelersTestTags
import com.github.swent.swisstravel.ui.trips.MyTripsScreenTestTags
import com.github.swent.swisstravel.ui.trips.SetCurrentTripScreenTestTags
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
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

  /** Creates and returns an initialized in-memory repository for testing. */
  abstract fun createInitializedRepository(): TripsRepository

  open fun initializeHTTPClient(): OkHttpClient = FakeHttpClient.getClient()

  open val repository: TripsRepository
    get() = TripsRepositoryProvider.repository

  val httpClient
    get() = HttpClientProvider.client

  val shouldSignInAnonymously: Boolean = FirebaseEmulator.isRunning

  val currentUser: FirebaseUser
    get() {
      return FirebaseEmulator.auth.currentUser!!
    }

  init {
    assert(FirebaseEmulator.isRunning) { "FirebaseEmulator must be running when running thetests" }
  }

  @Before
  // TODO : Set up repository when it is implemented
  open fun setUp() {
    TripsRepositoryProvider.repository = createInitializedRepository()
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
          isCurrentTrip = false,
          uriLocation = emptyMap(),
          collaboratorsId = emptyList())

  val dummyLocation = Location(Coordinate(0.0, 0.0), "Test Location")

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
          isCurrentTrip = false,
          uriLocation = emptyMap(),
          collaboratorsId = emptyList())

  val tripList = listOf(trip1, trip2)

  // TODO : Declare ComposeTestRules here

  fun ComposeTestRule.checkMyTripsScreenIsDisplayed() {
    onNodeWithTag(MyTripsScreenTestTags.PAST_TRIPS_BUTTON).assertIsDisplayed()
    onNodeWithTag(SortedTripListTestTags.TITLE, useUnmergedTree = true)
        .assertIsDisplayed()
        .assertTextContains("Upcoming Trips", substring = false, ignoreCase = true)
    onNodeWithTag(SortedTripListTestTags.SORT_DROPDOWN_MENU).assertIsDisplayed()
    onNodeWithTag(MyTripsScreenTestTags.CURRENT_TRIP_TITLE)
        .assertIsDisplayed()
        .assertTextContains("Current Trip", substring = false, ignoreCase = true)
  }

  // Can be used to check if there is no trip displayed as well
  fun ComposeTestRule.checkMyTripsScreenIsDisplayedWithNoTrips() {
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

  fun ComposeTestRule.checkMyTripsInSelectionMode() {
    onNodeWithTag(MyTripsScreenTestTags.MORE_OPTIONS_BUTTON).assertIsDisplayed()
    onNodeWithTag(MyTripsScreenTestTags.DELETE_SELECTED_BUTTON).assertIsDisplayed()
    onNodeWithTag(MyTripsScreenTestTags.CANCEL_SELECTION_BUTTON).assertIsDisplayed()
    onNodeWithTag(MyTripsScreenTestTags.FAVORITE_SELECTED_BUTTON).assertIsDisplayed()
  }

  fun ComposeTestRule.checkMyTripsNotInSelectionMode() {
    waitForIdle()
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

  fun ComposeTestRule.checkCurrentTripScreenEmptyIsDisplayed() {
    onNodeWithTag(CurrentTripScreenTestTags.CREATE_TRIP_TEXT)
        .assertIsDisplayed()
        .assertTextContains("Create a trip", substring = false, ignoreCase = true)
    onNodeWithTag(CurrentTripScreenTestTags.CREATE_TRIP_BUTTON).assertIsDisplayed()
  }

  fun ComposeTestRule.checkCurrentTripScreenIsDisplayed(trip: Trip) {
    checkTripInfoScreenIsDisplayedWithTrip(trip)
  }

  fun ComposeTestRule.checkCurrentTripScreenIsNotDisplayed() {
    onNodeWithTag(CurrentTripScreenTestTags.CREATE_TRIP_TEXT).assertDoesNotExist()
    onNodeWithTag(CurrentTripScreenTestTags.CREATE_TRIP_BUTTON).assertDoesNotExist()
  }

  fun ComposeTestRule.checkProfileScreenIsDisplayed() {
    // Profile header
    waitUntil(
        timeoutMillis = UI_WAIT_TIMEOUT,
        condition = {
          onNodeWithTag(ProfileScreenTestTags.PROFILE_PIC).isDisplayed() &&
              onNodeWithTag(ProfileScreenTestTags.SETTINGS_BUTTON).isDisplayed()
        })
    onNodeWithTag(ProfileScreenTestTags.PROFILE_PIC).assertIsDisplayed()

    // Biography (not displayed cause it's empty)
    // onNodeWithTag(ProfileScreenTestTags.BIOGRAPHY).assertIsDisplayed()

    // Settings button (own profile)
    onNodeWithTag(ProfileScreenTestTags.SETTINGS_BUTTON).assertIsDisplayed()
    onNodeWithTag(ProfileScreenTestTags.UNFRIEND_BUTTON).assertDoesNotExist()

    // Pinned trips section
    onNodeWithTag(ProfileScreenTestTags.PINNED_TRIPS_TITLE).assertIsDisplayed()
    onNodeWithTag(ProfileScreenTestTags.PINNED_TRIPS_EDIT_BUTTON).assertExists()

    // Pinned images section
    onNodeWithTag(ProfileScreenTestTags.PINNED_PICTURES_TITLE).assertIsDisplayed()
    onNodeWithTag(ProfileScreenTestTags.EMPTY_PINNED_PICTURES).assertExists()
    onNodeWithTag(ProfileScreenTestTags.PINNED_PICTURES_EDIT_BUTTON).assertExists()
  }

  fun ComposeTestRule.checkProfileScreenIsNotDisplayed() {
    // Profile header
    onNodeWithTag(ProfileScreenTestTags.PROFILE_PIC).assertIsNotDisplayed()

    // Biography
    onNodeWithTag(ProfileScreenTestTags.BIOGRAPHY).assertIsNotDisplayed()

    // Achievements
    onNodeWithTag(ProfileScreenTestTags.ACHIEVEMENTS).assertIsNotDisplayed()

    // Settings button (own profile)
    onNodeWithTag(ProfileScreenTestTags.SETTINGS_BUTTON).assertIsNotDisplayed()
    onNodeWithTag(ProfileScreenTestTags.UNFRIEND_BUTTON).assertIsNotDisplayed()

    // Pinned trips section
    onNodeWithTag(ProfileScreenTestTags.PINNED_TRIPS_TITLE).assertIsNotDisplayed()
    onNodeWithTag(ProfileScreenTestTags.PINNED_TRIPS_EDIT_BUTTON).assertDoesNotExist()

    // Pinned images section
    onNodeWithTag(ProfileScreenTestTags.PINNED_PICTURES_TITLE).assertIsNotDisplayed()
    onNodeWithTag(ProfileScreenTestTags.PINNED_PICTURES_LIST).assertDoesNotExist()
    onNodeWithTag(ProfileScreenTestTags.PINNED_PICTURES_EDIT_BUTTON).assertDoesNotExist()
  }

  fun ComposeTestRule.checkNavigationMenuIsDisplayed() {
    onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()
    onNodeWithTag(NavigationTestTags.CURRENT_TRIP_TAB).assertIsDisplayed()
    onNodeWithTag(NavigationTestTags.PROFILE_TAB).assertIsDisplayed()
    onNodeWithTag(NavigationTestTags.MY_TRIPS_TAB).assertIsDisplayed()
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

  // Trip creation screens
  fun ComposeTestRule.checkTripDateScreenIsDisplayed() {
    checkTopBarIsDisplayed()
    onNodeWithTag(TripDateTestTags.TRIP_DATE_SCREEN).assertIsDisplayed()
    onNodeWithTag(TripDateTestTags.NEXT).assertIsDisplayed()
  }

  fun ComposeTestRule.checkTravelerCounterIsDisplayed(adultsLabel: String, childrenLabel: String) {
    onNodeWithTag(adultsLabel + CounterTestTags.COUNTER).assertIsDisplayed()
    onNodeWithTag(adultsLabel + CounterTestTags.INCREMENT).assertIsDisplayed()
    onNodeWithTag(adultsLabel + CounterTestTags.DECREMENT).assertIsDisplayed()
    onNodeWithTag(childrenLabel + CounterTestTags.COUNTER).assertIsDisplayed()
    onNodeWithTag(childrenLabel + CounterTestTags.INCREMENT).assertIsDisplayed()
    onNodeWithTag(childrenLabel + CounterTestTags.DECREMENT).assertIsDisplayed()
  }

  fun ComposeTestRule.checkTripTravelersIsDisplayed(adultsLabel: String, childrenLabel: String) {
    checkTopBarIsDisplayed()
    onNodeWithTag(TripTravelersTestTags.TRIP_TRAVELERS_SCREEN).assertIsDisplayed()
    onNodeWithTag(TripTravelersTestTags.NEXT).assertIsDisplayed()
    checkTravelerCounterIsDisplayed(adultsLabel, childrenLabel)
  }

  fun ComposeTestRule.checkPreferenceSelectorIsDisplayed() {
    for (category in
        PreferenceCategories.Category.values().filter {
          it != PreferenceCategories.Category.DEFAULT
        }) {
      val tag = PreferenceSelectorTestTags.getTestTagCategory(category)
      onNodeWithTag(PreferenceSelectorTestTags.PREFERENCE_SELECTOR)
          .performScrollToNode(hasTestTag(tag))
      onNodeWithTag(tag).assertIsDisplayed()
    }
    for (preferences in Preference.values()) {
      val tag = PreferenceSelectorTestTags.getTestTagButton(preferences)
      onNodeWithTag(PreferenceSelectorTestTags.PREFERENCE_SELECTOR)
          .performScrollToNode(hasTestTag(tag))
      onNodeWithTag(tag).assertIsDisplayed()
    }
  }

  fun ComposeTestRule.checkTripPreferencesIsDisplayed() {
    checkTopBarIsDisplayed()
    onNodeWithTag(TripPreferencesTestTags.TRIP_PREFERENCES_SCREEN).assertIsDisplayed()
    onNodeWithTag(TripPreferencesTestTags.TRIP_PREFERENCES_TITLE).assertIsDisplayed()
    onNodeWithTag(TripPreferencesTestTags.TRIP_PREFERENCE_CONTENT).assertIsDisplayed()
    onNodeWithTag(TripPreferencesTestTags.TRIP_PREFERENCES_SCREEN).assertExists()
    checkPreferenceSelectorIsDisplayed()
  }

  fun ComposeTestRule.checkArrivalDepartureScreenIsDisplayed() {
    onNodeWithTag(ArrivalDepartureTestTags.DEPARTURE_TEXTFIELD).assertIsDisplayed()
    onNodeWithTag(ArrivalDepartureTestTags.ARRIVAL_TEXTFIELD).assertIsDisplayed()
    onNodeWithTag(ArrivalDepartureTestTags.NEXT_BUTTON).assertIsDisplayed()
  }

  fun ComposeTestRule.checkDestinationScreenIsDisplayed() {
    onNodeWithTag(TripFirstDestinationsTestTags.FIRST_DESTINATIONS_TITLE).assertIsDisplayed()
    onNodeWithTag(TripFirstDestinationsTestTags.NEXT_BUTTON).assertIsDisplayed()
    onNodeWithTag(TripFirstDestinationsTestTags.NEXT_BUTTON).assertIsDisplayed()
  }

  fun ComposeTestRule.checkLandingScreenIsDisplayed() {
    onNodeWithTag(LandingScreenTestTags.APP_LOGO).assertIsDisplayed()
    onNodeWithTag(LandingScreenTestTags.APP_NAME).assertIsDisplayed()
    onNodeWithTag(LandingScreenTestTags.SIGN_IN_BUTTON).assertIsDisplayed()
    onNodeWithTag(LandingScreenTestTags.SIGN_UP_BUTTON).assertIsDisplayed()
  }

  fun ComposeTestRule.checkTripInfoScreenIsDisplayed() {
    onNodeWithTag(TripInfoScreenTestTags.TITLE).assertIsDisplayed()
    onNodeWithTag(TripInfoScreenTestTags.BACK_BUTTON).assertIsDisplayed()
    onNodeWithTag(TripInfoScreenTestTags.FAVORITE_BUTTON).assertIsDisplayed()
    onNodeWithTag(TripInfoScreenTestTags.EDIT_BUTTON).assertIsDisplayed()

    onNodeWithTag(TripInfoScreenTestTags.LAZY_COLUMN).assertIsDisplayed()
    // If there are no locations the screen shows a no-locations message; usually not visible for
    // populated trips.
    onNodeWithTag(TripInfoScreenTestTags.NO_LOCATIONS).assertIsNotDisplayed()

    onNodeWithTag(TripInfoScreenTestTags.CURRENT_STEP).assertIsDisplayed()
    onNodeWithTag(TripInfoScreenTestTags.LOCATION_NAME).assertIsDisplayed()

    // Map related elements
    onNodeWithTag(TripInfoScreenTestTags.MAP_CARD).assertIsDisplayed()
    onNodeWithTag(TripInfoScreenTestTags.MAP_CONTAINER).assertIsDisplayed()
    onNodeWithTag(TripInfoScreenTestTags.FULLSCREEN_BUTTON).assertIsDisplayed()

    // Fullscreen map / exit should not be visible by default
    onNodeWithTag(TripInfoScreenTestTags.FULLSCREEN_MAP).assertDoesNotExist()
    onNodeWithTag(TripInfoScreenTestTags.FULLSCREEN_EXIT).assertDoesNotExist()
  }

  // Done with AI
  fun ComposeTestRule.checkTripInfoScreenIsDisplayedWithTrip(
      trip: Trip,
      context: Context = ApplicationProvider.getApplicationContext<Context>()
  ) {
    // --- Top App Bar ---
    onNodeWithTag(TripInfoScreenTestTags.TITLE).assertIsDisplayed().assertTextEquals(trip.name)

    onNodeWithTag(TripInfoScreenTestTags.BACK_BUTTON).assertIsDisplayed()

    onNodeWithTag(TripInfoScreenTestTags.EDIT_BUTTON).assertIsDisplayed()

    onNodeWithTag(TripInfoScreenTestTags.FAVORITE_BUTTON).assertIsDisplayed()

    // --- Current Step section ---
    onNodeWithTag(TripInfoScreenTestTags.CURRENT_STEP)
        .assertIsDisplayed()
        .assertTextContains(context.getString(R.string.current_step), substring = true)

    onNodeWithTag(TripInfoScreenTestTags.NEXT_STEP).assertIsDisplayed()

    // --- Map card ---
    onNodeWithTag(TripInfoScreenTestTags.MAP_CARD).assertIsDisplayed()

    // Map container itself
    onNodeWithTag(TripInfoScreenTestTags.MAP_CONTAINER).assertIsDisplayed()

    // --- Lazy column with steps ---
    onNodeWithTag(TripInfoScreenTestTags.LAZY_COLUMN).assertIsDisplayed()

    Thread.sleep(3000)
    // The trip has at least two locations, find them and test
    trip.locations.take(2).forEach { location ->
      onAllNodesWithText(location.name, substring = true)
          .onFirst()
          .assertExists("Expected location '${location.name}' to be shown in TripInfoScreen")
    }

    // --- Map fullscreen button ---
    onNodeWithTag(TripInfoScreenTestTags.FULLSCREEN_BUTTON).assertIsDisplayed()
  }

  fun ComposeTestRule.checkEditTripScreenIsDisplayed(
      trip: Trip,
      adultsLabel: String,
      childrenLabel: String
  ) {
    onNodeWithTag(EditTripScreenTestTags.SCREEN).assertIsDisplayed()

    onNodeWithTag(EditTripScreenTestTags.TRIP_NAME).assertIsDisplayed().assertTextEquals(trip.name)
    onNodeWithTag(EditTripScreenTestTags.CONFIRM_TOP_BAR).assertIsDisplayed()

    checkTravelerCounterIsDisplayed(adultsLabel, childrenLabel)

    checkPreferenceSelectorIsDisplayed()

    onNodeWithTag(EditTripScreenTestTags.DELETE).performScrollTo().assertIsDisplayed()
  }

  // Made with AI
  fun ComposeTestRule.changeTripNameAndSaveInEditTrip(newName: String) {
    // Wait until loading spinner disappears
    onNodeWithTag(EditTripScreenTestTags.LOADING).assertDoesNotExist()

    // Change text field value
    onNodeWithTag(EditTripScreenTestTags.TRIP_NAME)
        .performScrollTo()
        .assertIsDisplayed()
        .performTextClearance()
    onNodeWithTag(EditTripScreenTestTags.TRIP_NAME).performTextInput(newName)

    // Verify text updated
    onNodeWithTag(EditTripScreenTestTags.TRIP_NAME).assertTextEquals(newName)

    // Click confirm (top bar)
    onNodeWithTag(EditTripScreenTestTags.CONFIRM_TOP_BAR).assertIsDisplayed().performClick()
  }

  // Done with the help of AI
  fun ComposeTestRule.checkTripSummaryScreenIsDisplayed(
      expectedAdults: Int = 1,
      expectedChildren: Int = 0,
      expectedDeparture: String =
          "Café de Paris, 26 Rue du Mont-Blanc, 1201 Genève, Genève, Suisse",
      expectedArrival: String =
          "École Polytechnique Fédérale de Lausanne (EPFL), Route Cantonale, 1015 Lausanne, Vaud, Suisse",
      expectedPreferences: List<Preference> = emptyList(),
      startDate: Timestamp = Timestamp.now(),
      endDate: Timestamp = Timestamp(Timestamp.now().seconds + 24 * 60 * 60, 0)
  ) {
    onNodeWithTag(TripSummaryTestTags.TRIP_SUMMARY_SCREEN).assertIsDisplayed()
    val context = ApplicationProvider.getApplicationContext<Context>()
    val adultSingular = context.getString(R.string.adult)
    val adultPlural = context.getString(R.string.adults)
    val childSingular = context.getString(R.string.child)
    val childPlural = context.getString(R.string.children)
    val fromDate = context.getString(R.string.from_summary)
    val toDate = context.getString(R.string.to_summary)
    // Dates: today and tomorrow
    val zone = ZoneId.systemDefault()
    val startLocalDate = Instant.ofEpochSecond(startDate.seconds).atZone(zone).toLocalDate()
    val endLocalDate = Instant.ofEpochSecond(endDate.seconds).atZone(zone).toLocalDate()

    val formatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRANCE)
    val fromDateStr = startLocalDate.format(formatter)
    val toDateStr = endLocalDate.format(formatter)

    // Dates
    onNodeWithTag(TripSummaryTestTags.FROM_DATE)
        .assertIsDisplayed()
        .assertTextEquals("$fromDate $fromDateStr")
    onNodeWithTag(TripSummaryTestTags.TO_DATE)
        .assertIsDisplayed()
        .assertTextEquals("$toDate $toDateStr")

    // Travelers
    onNodeWithTag(TripSummaryTestTags.ADULTS_COUNT)
        .assertIsDisplayed()
        .assertTextEquals(
            "$expectedAdults ${if (expectedAdults == 1) adultSingular else adultPlural}")
    onNodeWithTag(TripSummaryTestTags.CHILDREN_COUNT)
        .assertIsDisplayed()
        .assertTextEquals(
            "$expectedChildren ${if (expectedChildren == 1) childSingular else childPlural}")

    // Preferences
    for (preference in expectedPreferences) {
      val tag = TripPreferenceIconTestTags.getTestTag(preference)
      onNodeWithTag(TripSummaryTestTags.TRIP_SUMMARY_SCREEN).performScrollToNode(hasTestTag(tag))
      val textTag = context.getString(preference.displayStringRes())
      onNodeWithText(textTag).assertIsDisplayed()
    }

    // Departure / Arrival
    val departureTag = "${TripSummaryTestTags.DEPARTURE_LABEL}_value"
    onNodeWithTag(TripSummaryTestTags.TRIP_SUMMARY_SCREEN)
        .performScrollToNode(hasTestTag(departureTag))
    onNodeWithTag(departureTag).assertIsDisplayed().assertTextEquals(expectedDeparture)

    val arrivalTag = "${TripSummaryTestTags.ARRIVAL_LABEL}_value"
    onNodeWithTag(TripSummaryTestTags.TRIP_SUMMARY_SCREEN)
        .performScrollToNode(hasTestTag(arrivalTag))
    onNodeWithTag(arrivalTag).assertIsDisplayed().assertTextEquals(expectedArrival)
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

  fun ComposeTestRule.performClickOnLocationSuggestion() {
    waitUntil(E2E_WAIT_TIMEOUT) {
      onAllNodesWithTag(LocationTextTestTags.LOCATION_SUGGESTION).fetchSemanticsNodes().isNotEmpty()
    }

    onAllNodesWithTag(LocationTextTestTags.LOCATION_SUGGESTION).onFirst().performClick()
  }

  fun <A : ComponentActivity> AndroidComposeTestRule<ActivityScenarioRule<A>, A>
      .checkActivityStateOnPressBack(shouldFinish: Boolean) {
    activityRule.scenario.onActivity { activity ->
      activity.onBackPressedDispatcher.onBackPressed()
    }
    waitUntil { activity.isFinishing == shouldFinish }
    assertEquals(shouldFinish, activity.isFinishing)
  }

  fun ComposeTestRule.checkProfileSettingsScreenIsDisplayed() {
    // Static bits
    onNodeWithTag(ProfileSettingsScreenTestTags.PROFILE_PIC).assertIsDisplayed()
    onNodeWithTag(ProfileSettingsScreenTestTags.PROFILE_INFO).assertIsDisplayed()
    onNodeWithTag(ProfileSettingsScreenTestTags.PERSONAL_INFO).assertIsDisplayed()
    onNodeWithTag(ProfileSettingsScreenTestTags.EMAIL).assertIsDisplayed()
    val fields = listOf("NAME", "BIOGRAPHY")
    fields.forEach { prefix ->
      onNodeWithTag(ProfileSettingsScreenTestTags.label(prefix)).assertIsDisplayed()
      onNodeWithTag(ProfileSettingsScreenTestTags.text(prefix)).assertIsDisplayed()
      onNodeWithTag(ProfileSettingsScreenTestTags.editButton(prefix)).assertIsDisplayed()
    }
    onNodeWithTag(ProfileSettingsScreenTestTags.LOGOUT_BUTTON).performScrollTo().assertIsDisplayed()

    // Preferences container present
    onNodeWithTag(ProfileSettingsScreenTestTags.PREFERENCES_LIST).assertIsDisplayed()
    // Header row present
    onNodeWithTag(ProfileSettingsScreenTestTags.PREFERENCES).assertIsDisplayed()
    // Collapsed by default: a well-known preference label should NOT be visible yet
    onNodeWithTag(PreferenceSelectorTestTags.getTestTagButton(Preference.MUSEUMS))
        .assertDoesNotExist()
  }

  fun ComposeTestRule.checkFriendProfileScreenIsDisplayed() {
    // Profile header
    onNodeWithTag(ProfileScreenTestTags.PROFILE_PIC).assertIsDisplayed()

    // Biography
    onNodeWithTag(ProfileScreenTestTags.BIOGRAPHY).assertIsDisplayed()

    // Achievements
    onNodeWithTag(ProfileScreenTestTags.ACHIEVEMENTS).assertIsDisplayed()

    // Unfriend button (friend profile)
    onNodeWithTag(ProfileScreenTestTags.UNFRIEND_BUTTON).assertIsDisplayed()
    onNodeWithTag(ProfileScreenTestTags.SETTINGS_BUTTON).assertIsNotDisplayed()

    // Pinned trips section
    onNodeWithTag(ProfileScreenTestTags.PINNED_TRIPS_TITLE).assertIsDisplayed()
    onNodeWithTag(ProfileScreenTestTags.PINNED_TRIPS_EDIT_BUTTON).assertDoesNotExist()

    // Pinned images section
    onNodeWithTag(ProfileScreenTestTags.PINNED_PICTURES_TITLE).assertIsDisplayed()
    onNodeWithTag(ProfileScreenTestTags.PINNED_PICTURES_LIST).assertExists()
    onNodeWithTag(ProfileScreenTestTags.PINNED_PICTURES_EDIT_BUTTON).assertDoesNotExist()
  }

  fun ComposeTestRule.addPhotosScreenIsDisplayed() {
    onNodeWithTag(AddPhotosScreenTestTags.MAIN_SCREEN).assertIsDisplayed()
    onNodeWithTag(AddPhotosScreenTestTags.TOP_APP_BAR).assertIsDisplayed()
    onNodeWithTag(AddPhotosScreenTestTags.TOP_APP_BAR_TITLE).assertIsDisplayed()
    onNodeWithTag(AddPhotosScreenTestTags.BOTTOM_BAR).assertIsDisplayed()
    onNodeWithTag(AddPhotosScreenTestTags.ADD_PHOTOS_BUTTON).assertIsDisplayed()
    onNodeWithTag(AddPhotosScreenTestTags.EDIT_BUTTON).assertIsDisplayed()
  }

  fun ComposeTestRule.editPhotosScreenIsDisplayed() {
    onNodeWithTag(EditPhotosScreenTestTags.EDIT_SCAFFOLD).assertIsDisplayed()
    onNodeWithTag(EditPhotosScreenTestTags.EDIT_TOP_BAR).assertIsDisplayed()
    onNodeWithTag(EditPhotosScreenTestTags.EDIT_TOP_BAR_TITLE).assertIsDisplayed()
    onNodeWithTag(EditPhotosScreenTestTags.EDIT_CANCEL_BUTTON).assertIsDisplayed()
    onNodeWithTag(EditPhotosScreenTestTags.EDIT_BOTTOM_BAR).assertIsDisplayed()
    onNodeWithTag(EditPhotosScreenTestTags.EDIT_REMOVE_BUTTON).assertIsDisplayed()
  }

  fun ComposeTestRule.checkSwipeActivityScreenIsDisplayed() {
    onNodeWithTag(SwipeActivitiesScreenTestTags.SWIPE_ACTIVITIES_SCREEN).assertIsDisplayed()
    onNodeWithTag(SwipeActivitiesScreenTestTags.LIKE_BUTTON).assertIsDisplayed()
    onNodeWithTag(SwipeActivitiesScreenTestTags.DISLIKE_BUTTON).assertIsDisplayed()
  }

  fun ComposeTestRule.checkLikedActivitiesScreenIsDisplayed() {
    onNodeWithTag(LikedActivitiesScreenTestTags.SCREEN_TITLE).assertIsDisplayed()
    onNodeWithTag(LikedActivitiesScreenTestTags.BACK_BUTTON).assertIsDisplayed()
    onNodeWithTag(LikedActivitiesScreenTestTags.UNLIKE_BUTTON).assertIsDisplayed()
    onNodeWithTag(LikedActivitiesScreenTestTags.SCHEDULE_BUTTON).assertIsDisplayed()
  }

  fun ComposeTestRule.selectPinnedTripsScreenIsDisplayed() {
    onNodeWithTag(SelectPinnedTripsScreenTestTags.TOP_APP_BAR).assertIsDisplayed()
    onNodeWithTag(SelectPinnedTripsScreenTestTags.SAVE_SELECTED_TRIPS_FAB).assertIsDisplayed()
    onNodeWithTag(TripListTestTags.TRIP_LIST).assertIsDisplayed()

    // Ensure pinned trip is displayed
    onNodeWithText("Trip One").assertIsDisplayed()
    // Non-pinned trips may appear in available list
    onNodeWithText("Trip Two").assertIsDisplayed()
  }

  fun ComposeTestRule.selectPinnedPicturesScreenIsDisplayed() {
    // Verify Screen Content
    onNodeWithTag(MAIN_SCREEN).assertIsDisplayed()
    onNodeWithTag(VERTICAL_GRID).assertIsDisplayed()

    // Verify buttons
    onNodeWithTag(ADD_PICTURE_BUTTON).assertIsDisplayed()
    onNodeWithTag(SAVE_BUTTON).assertIsDisplayed()

    onNodeWithTag(LOADING_INDICATOR).assertDoesNotExist()
  }

  fun ComposeTestRule.clickOnBackButton() {
    onNodeWithTag(BackButtonTestTag.BACK_BUTTON).performClick()
  }

  fun ComposeTestRule.clickOnRetryButton() {
    onNodeWithTag(ErrorScreenTestTags.RETRY_BUTTON).performClick()
  }

  fun ComposeTestRule.clickOnEditPhotos() {
    onNodeWithTag(AddPhotosScreenTestTags.EDIT_BUTTON).performClick()
  }

  fun ComposeTestRule.clickOnAddPhotos() {
    onNodeWithTag(AddPhotosScreenTestTags.ADD_PHOTOS_BUTTON).performClick()
  }

  fun ComposeTestRule.clickOnRemovePhotos() {
    onNodeWithTag(EditPhotosScreenTestTags.EDIT_REMOVE_BUTTON).performClick()
  }

  fun ComposeTestRule.exitEditPhotos() {
    onNodeWithTag(EditPhotosScreenTestTags.EDIT_CANCEL_BUTTON).performClick()
  }
  // TODO : Create helper/companions functions here

}
