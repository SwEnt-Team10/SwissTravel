package com.github.swent.swisstravel.e2e

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.PickerActions
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.swent.swisstravel.SwissTravelApp
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.ui.composable.DateSelectorTestTags
import com.github.swent.swisstravel.ui.composable.PreferenceSelectorTestTags
import com.github.swent.swisstravel.ui.currenttrip.CurrentTripScreenTestTags
import com.github.swent.swisstravel.ui.navigation.NavigationTestTags
import com.github.swent.swisstravel.ui.profile.ProfileScreenTestTags
import com.github.swent.swisstravel.ui.profile.ProfileSettingsScreenTestTags
import com.github.swent.swisstravel.ui.tripcreation.ArrivalDepartureTestTags
import com.github.swent.swisstravel.ui.tripcreation.TripDateTestTags
import com.github.swent.swisstravel.ui.tripcreation.TripFirstDestinationsTestTags
import com.github.swent.swisstravel.ui.tripcreation.TripPreferencesTestTags
import com.github.swent.swisstravel.ui.tripcreation.TripSummaryTestTags
import com.github.swent.swisstravel.ui.tripcreation.TripTravelersTestTags
import com.github.swent.swisstravel.ui.trips.MyTripsScreenTestTags
import com.github.swent.swisstravel.utils.E2E_WAIT_TIMEOUT
import com.github.swent.swisstravel.utils.FakeCredentialManager
import com.github.swent.swisstravel.utils.FakeJwtGenerator
import com.github.swent.swisstravel.utils.FirebaseEmulator
import com.github.swent.swisstravel.utils.FirestoreSwissTravelTest
import com.google.firebase.Timestamp
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class E2ETripFlowTest : FirestoreSwissTravelTest() {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var todayLabel: String
  private lateinit var otherDayLabel: String

  @Before
  override fun setUp() {
    super.setUp()
    FirebaseEmulator.clearAuthEmulator()
    FirebaseEmulator.auth.signOut()
    val today = LocalDate.now()
    todayLabel = "Trip from $today"
    val other = today.plusDays(5)
    otherDayLabel = "Trip from $other"
  }

  @Test
  fun full_multi_account_trip_and_prefs_flow() {
    val alice =
        FakeJwtGenerator.createFakeGoogleIdToken(name = "Alice", email = "alice@example.com")
    val bob = FakeJwtGenerator.createFakeGoogleIdToken(name = "Bob", email = "bob@example.com")
    val creds = FakeCredentialManager.sequence(alice, bob)
    val tripsRepo = createInitializedRepository()

    composeTestRule.setContent { SwissTravelApp(credentialManager = creds) }

    // 1) Log in as first account
    composeTestRule.loginWithGoogle(true)
    composeTestRule.waitForMainUi()

    // 2) Go to current trip
    composeTestRule.onNodeWithTag(NavigationTestTags.CURRENT_TRIP_TAB).performClick()
    Thread.sleep(1000) // Wait for tab switch animation

    composeTestRule.onNodeWithTag(CurrentTripScreenTestTags.CREATE_TRIP_BUTTON).assertIsDisplayed()

    // 3) Create a trip
    createTrip(selectNonToday = false)

    // 4) See that the trip is displayed in My Trips
    composeTestRule.waitForMainUi()
    composeTestRule
        .onNodeWithTag(NavigationTestTags.MY_TRIPS_TAB, useUnmergedTree = true)
        .assertIsDisplayed()
        .performClick()
    Thread.sleep(1000) // Wait for MyTrips list to fetch

    // 5) Go back to current trip and create another trip with non-today date
    composeTestRule.onNodeWithTag(NavigationTestTags.CURRENT_TRIP_TAB).performClick()
    Thread.sleep(1000)
    val nonTodayTrip =
        createTestTrip(
            uid = "nonToday",
            name = "Awesome trip",
            departureLocation = Location(Coordinate(46.2095, 46.2095), "Café de Paris"),
            arrivalLocation = Location(Coordinate(46.5191, 6.5668), "EPFL"),
            ownerId = FirebaseEmulator.auth.currentUser!!.uid,
            startDate = Timestamp(Timestamp.now().seconds + 1000 * 24 * 5 * 3600, 0),
            endDate = Timestamp(Timestamp.now().seconds + 1000 * 24 * 6 * 3600, 0))
    runBlocking { tripsRepo.addTrip(nonTodayTrip) }

    // 6) Verify second trip is displayed in My Trips
    composeTestRule.waitForMainUi()
    composeTestRule
        .onNodeWithTag(NavigationTestTags.MY_TRIPS_TAB, useUnmergedTree = true)
        .assertIsDisplayed()
        .performClick()
    Thread.sleep(1500) // Wait for Firestore fetch on slow CI

    val tripsAlice = runBlocking { repository.getAllTrips() }
    assertEquals(2, tripsAlice.size)

    // 7) Go to profile and modify a preference
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_TAB).performClick()
    Thread.sleep(1000)
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.SETTINGS_BUTTON).performClick()
    composeTestRule.waitForTag(ProfileSettingsScreenTestTags.EMAIL)
    composeTestRule
        .onNodeWithTag(ProfileSettingsScreenTestTags.PREFERENCES)
        .performScrollToNode(hasTestTag(ProfileSettingsScreenTestTags.PREFERENCES_TOGGLE))
    composeTestRule.onNodeWithTag(ProfileSettingsScreenTestTags.PREFERENCES_TOGGLE).performClick()

    // Wait explicitly until the preference switch node appears in the Compose tree
    composeTestRule.waitUntil(E2E_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(
              PreferenceSelectorTestTags.getTestTagButton(Preference.MUSEUMS),
              useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    // Now safely interact
    composeTestRule
        .onNodeWithTag(
            PreferenceSelectorTestTags.getTestTagButton(Preference.MUSEUMS), useUnmergedTree = true)
        .performScrollTo()
        .performClick()

    // 8) Log out
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_BUTTON).performClick()
    composeTestRule.logout()
    Thread.sleep(1000) // Allow logout cleanup

    // 9) Log in with another account
    composeTestRule.loginWithGoogle(true)
    composeTestRule.waitForMainUi()

    // 10) Verify no other trip is displayed
    composeTestRule.waitForMainUi()
    composeTestRule
        .onNodeWithTag(NavigationTestTags.MY_TRIPS_TAB, useUnmergedTree = true)
        .assertIsDisplayed()
        .performClick()
    Thread.sleep(1000)
    // Expect empty messages to be displayed for a fresh account
    composeTestRule
        .onNodeWithTag(MyTripsScreenTestTags.EMPTY_CURRENT_TRIP_MSG, useUnmergedTree = true)
        .assertIsDisplayed()

    // 11) Create a new trip for second account
    composeTestRule.onNodeWithTag(NavigationTestTags.CURRENT_TRIP_TAB).performClick()
    createTrip(selectNonToday = false)

    // 12) Verify it is displayed in My Trips
    composeTestRule.waitForMainUi()
    composeTestRule
        .onNodeWithTag(NavigationTestTags.MY_TRIPS_TAB, useUnmergedTree = true)
        .assertIsDisplayed()
        .performClick()
    Thread.sleep(2000) // Wait for fetch

    val tripsBob = runBlocking { repository.getAllTrips() }
    assertEquals(1, tripsBob.size)

    // 13) Log out
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_TAB).performClick()
    composeTestRule.logout()
  }

  /** Helper function to create a trip */
  private fun createTrip(selectNonToday: Boolean) {
    composeTestRule.onNodeWithTag(CurrentTripScreenTestTags.CREATE_TRIP_BUTTON).performClick()
    Thread.sleep(1500) // Wait for screen transition
    // TripDateScreen: optionally change start date to non-today
    if (selectNonToday) {
      val dateButtons = composeTestRule.onAllNodesWithTag(DateSelectorTestTags.DATE)
      dateButtons.assertCountEquals(2)
      dateButtons[0].performClick() // open start date picker

      // Pick a different day in the current month (today + 5)
      val target = LocalDate.now().plusDays(5)
      onView(isAssignableFrom(android.widget.DatePicker::class.java))
          .perform(PickerActions.setDate(target.year, target.monthValue, target.dayOfMonth))
      onView(withId(android.R.id.button1)).perform(click())
    }

    composeTestRule.onNodeWithTag(TripDateTestTags.NEXT).performClick()
    composeTestRule.waitForIdle()
    Thread.sleep(500)

    composeTestRule.onNodeWithTag(TripTravelersTestTags.NEXT).performClick()
    composeTestRule.waitForIdle()
    Thread.sleep(500)

    composeTestRule
        .onNodeWithTag(TripPreferencesTestTags.TRIP_PREFERENCE_CONTENT)
        .performScrollToNode(hasTestTag(TripPreferencesTestTags.DONE))
    composeTestRule.onNodeWithTag(TripPreferencesTestTags.DONE).performClick()
    composeTestRule.checkArrivalDepartureScreenIsDisplayed()

    // Fill the text fields
    composeTestRule
        .onNodeWithTag(ArrivalDepartureTestTags.ARRIVAL_TEXTFIELD, useUnmergedTree = true)
        .performTextInput("epfl")
    Thread.sleep(1000) // Wait for Autocomplete Suggestions Network Request
    composeTestRule.performClickOnLocationSuggestion()

    composeTestRule
        .onNodeWithTag(ArrivalDepartureTestTags.DEPARTURE_TEXTFIELD, useUnmergedTree = true)
        .performTextInput("cafe de paris")
    Thread.sleep(1000) // Wait for Autocomplete Suggestions Network Request
    composeTestRule.performClickOnLocationSuggestion()

    composeTestRule.onNodeWithTag(ArrivalDepartureTestTags.NEXT_BUTTON).performClick()

    composeTestRule.onNodeWithTag(TripFirstDestinationsTestTags.NEXT_BUTTON).performClick()
    Thread.sleep(1000)

    // Trip Summary
    composeTestRule.waitUntil(E2E_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(TripSummaryTestTags.TRIP_SUMMARY_SCREEN)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule
        .onNodeWithTag(TripSummaryTestTags.TRIP_SUMMARY_SCREEN)
        .performScrollToNode(hasTestTag(TripSummaryTestTags.TRIP_NAME_FIELD))
    composeTestRule
        .onNodeWithTag(TripSummaryTestTags.TRIP_NAME_FIELD)
        .performTextInput("Awesome trip")
    composeTestRule.onNodeWithTag(TripSummaryTestTags.TRIP_NAME_FIELD).performImeAction()

    composeTestRule
        .onNodeWithTag(TripSummaryTestTags.TRIP_SUMMARY_SCREEN)
        .performScrollToNode(hasTestTag(TripSummaryTestTags.CREATE_TRIP_BUTTON))
    composeTestRule.onNodeWithTag(TripSummaryTestTags.CREATE_TRIP_BUTTON).performClick()

    composeTestRule.waitForIdle()
    Thread.sleep(2000)
  }
}
