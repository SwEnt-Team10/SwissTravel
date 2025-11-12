package com.github.swent.swisstravel.e2e

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.SwissTravelApp
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.model.user.PreferenceCategories
import com.github.swent.swisstravel.ui.authentication.LandingScreenTestTags.SIGN_IN_BUTTON
import com.github.swent.swisstravel.ui.authentication.SignInScreenTestTags.GOOGLE_LOGIN_BUTTON
import com.github.swent.swisstravel.ui.composable.CounterTestTags
import com.github.swent.swisstravel.ui.composable.PreferenceSelectorTestTags
import com.github.swent.swisstravel.ui.navigation.NavigationTestTags
import com.github.swent.swisstravel.ui.profile.ProfileScreenTestTags
import com.github.swent.swisstravel.ui.theme.SwissTravelTheme
import com.github.swent.swisstravel.ui.tripcreation.ArrivalDepartureTestTags
import com.github.swent.swisstravel.ui.tripcreation.TripDateTestTags
import com.github.swent.swisstravel.ui.tripcreation.TripPreferencesTestTags
import com.github.swent.swisstravel.ui.tripcreation.TripTravelersTestTags
import com.github.swent.swisstravel.ui.trips.MyTripsScreenTestTags
import com.github.swent.swisstravel.utils.E2E_WAIT_TIMEOUT
import com.github.swent.swisstravel.utils.FakeCredentialManager
import com.github.swent.swisstravel.utils.FakeJwtGenerator
import com.github.swent.swisstravel.utils.FirebaseEmulator
import com.github.swent.swisstravel.utils.SwissTravelTest
import com.google.firebase.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * End-to-end trip creation flow:
 * 1) Start logged out, see login button
 * 2) Click Google sign-in (mocked)
 * 3) See bottom navigation and should be on profile screen
 * 4) Change preferences in profile
 * 5) Navigate to my trips which should be empty
 * 6) Click on the create trip button (bottom right)
 * 7) Fill the trip creation form
 * 8) Submit and see the newly created trip in my trips list
 * 9) Click on it and see trip details
 * 10) Edit the trip
 * 11) Save and go back to my trips
 * 12) Long click on the trip
 * 13) Favorite trip
 * 14) Click on the edit button and set as current trip
 * 15) Click on the trip and change some information
 * 16) Save and go back to trip info
 * 17) Click on edit trip
 * 18) Delete trip
 * 19) Go back to my trips and assert that the trip is gone and that the edit button isn't available
 * 20) Go to profile
 * 21) Log out
 */
class E2ETripCreationFlowTest : SwissTravelTest() {

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  override fun setUp() {
    super.setUp()
    FirebaseEmulator.auth.signOut()
    FirebaseEmulator.clearAuthEmulator()
  }

  @Test
  fun user_can_create_a_trip_and_edit_it() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    val fakeGoogleIdToken =
        FakeJwtGenerator.createFakeGoogleIdToken(name = "Test User", email = "test@example.com")
    val fakeCredentialManager = FakeCredentialManager.fake(fakeGoogleIdToken)

    // Start app logged out
    composeTestRule.setContent {
      SwissTravelTheme { SwissTravelApp(credentialManager = fakeCredentialManager) }
    }
    composeTestRule.onNodeWithTag(SIGN_IN_BUTTON).assertExists().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(GOOGLE_LOGIN_BUTTON).assertExists().performClick()

    // Wait for main navigation to appear (indicates successful sign-in + main UI shown)
    composeTestRule.waitUntil(E2E_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify bottom navigation visible
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertExists()

    // Verify that we are on the profile screen
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_TAB).performClick()
    composeTestRule.waitForIdle()
    Thread.sleep(
        5000) // Otherwise it is checks too fast the screen so it doesn't have time to pull the
              // elements
    composeTestRule.checkProfileScreenIsDisplayed()

    // Open preferences
    composeTestRule
        .onNodeWithTag(ProfileScreenTestTags.PREFERENCES)
        .performScrollToNode(hasTestTag(ProfileScreenTestTags.PREFERENCES_TOGGLE))
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.PREFERENCES_TOGGLE).performClick()

    // Preferences to change
    val preferences =
        listOf(
            Preference.MUSEUMS,
            Preference.HIKE,
            Preference.NIGHTLIFE,
            Preference.SHOPPING,
            Preference.WHEELCHAIR_ACCESSIBLE,
            Preference.NIGHT_OWL)
    // Assert that all categories exist
    for (category in
        PreferenceCategories.Category.values().filter {
          it != PreferenceCategories.Category.DEFAULT
        }) {
      val tag = PreferenceSelectorTestTags.getTestTagCategory(category)
      composeTestRule
          .onNodeWithTag(ProfileScreenTestTags.PREFERENCES_LIST)
          .performScrollToNode(hasTestTag(tag))
      composeTestRule.onNodeWithTag(tag).assertIsDisplayed()
    }
    // Find the preferences from the list and click on them
    composeTestRule.performClickPreferences(preferences)

    // Add EARLY_BIRD preference -> should remove NIGHT_OWL
    val tag = PreferenceSelectorTestTags.getTestTagButton(Preference.EARLY_BIRD)
    composeTestRule
        .onNodeWithTag(PreferenceSelectorTestTags.PREFERENCE_SELECTOR)
        .performScrollToNode(hasTestTag(tag))
    composeTestRule.onNodeWithTag(tag).assertIsDisplayed()
    composeTestRule.onNodeWithTag(tag).performClick()

    // Go to my trips
    composeTestRule.onNodeWithTag(NavigationTestTags.MY_TRIPS_TAB).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.checkMyTripsScreenIsDisplayedWithNoCurrentTrips()

    // Trip creation (the trip used will be stored in SwissTravelTest.kt)
    composeTestRule.onNodeWithTag(MyTripsScreenTestTags.CREATE_TRIP_BUTTON).performClick()

    composeTestRule.waitForIdle()
    // Trip date
    composeTestRule.checkTripDateScreenIsDisplayed()
    composeTestRule.onNodeWithTag(TripDateTestTags.NEXT).performClick()
    composeTestRule.waitForIdle()

    // Travelers
    val adultsLabel = context.getString(R.string.nb_adults)
    val childrenLabel = context.getString(R.string.nb_children)
    composeTestRule.checkTripTravelersIsDisplayed(adultsLabel, childrenLabel)

    // Adults increment 2 times
    repeat(2) {
      composeTestRule.onNodeWithTag(adultsLabel + CounterTestTags.INCREMENT).performClick()
    }

    // Adults decrement 1 time
    composeTestRule.onNodeWithTag(adultsLabel + CounterTestTags.DECREMENT).performClick()

    // Children increment 4 times
    repeat(4) {
      composeTestRule.onNodeWithTag(childrenLabel + CounterTestTags.INCREMENT).performClick()
    }

    // Children decrement 2 times
    repeat(2) {
      composeTestRule.onNodeWithTag(childrenLabel + CounterTestTags.DECREMENT).performClick()
    }

    // Check the adult counter displays 2
    composeTestRule.onNodeWithTag(adultsLabel + CounterTestTags.COUNT).assertTextEquals("2")

    // Check the children counter displays 2
    composeTestRule.onNodeWithTag(childrenLabel + CounterTestTags.COUNT).assertTextEquals("2")

    composeTestRule.onNodeWithTag(TripTravelersTestTags.NEXT).performClick()
    composeTestRule.waitForIdle()

    // Preferences
    val newPreferences =
        listOf(
            Preference.GROUP,
            Preference.CHILDREN_FRIENDLY,
            Preference.MUSEUMS, // Remove museums since already activated in the profile
            Preference.EARLY_BIRD // should remove NIGHT_OWL
            )
    composeTestRule.checkTripPreferencesIsDisplayed()
    // Assert that all categories exist
    for (category in
        PreferenceCategories.Category.values().filter {
          it != PreferenceCategories.Category.DEFAULT
        }) {
      val tag = PreferenceSelectorTestTags.getTestTagCategory(category)
      composeTestRule
          .onNodeWithTag(TripPreferencesTestTags.TRIP_PREFERENCE_CONTENT)
          .performScrollToNode(hasTestTag(tag))
      composeTestRule.onNodeWithTag(tag).assertIsDisplayed()
    }
    // Find the preferences from the list and click on them
    composeTestRule.performClickPreferences(newPreferences)
    composeTestRule
        .onNodeWithTag(TripPreferencesTestTags.TRIP_PREFERENCE_CONTENT)
        .performScrollToNode(hasTestTag(TripPreferencesTestTags.DONE))
    composeTestRule.onNodeWithTag(TripPreferencesTestTags.DONE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TripPreferencesTestTags.DONE).performClick()
    composeTestRule.waitForIdle()

    // Arrival - destination (using nominatim)
    composeTestRule.checkArrivalDepartureScreenIsDisplayed()
    composeTestRule
        .onNodeWithTag(ArrivalDepartureTestTags.NEXT_BUTTON)
        .performClick() // should do nothing
    composeTestRule.checkArrivalDepartureScreenIsDisplayed() // still on the same screen

    // Destination (using MySwitzerland)

    // Trip Summary

    // Back to my trips

    // Long click

    // Selection mode

    // Save as favorite

    // Edit current trip

    // Set current trip screen

    // Set as current trip

    // Back to my trips

    // Click on the trip

    // Trip info screen

    // Compare the current step

    // Click on edit trip

    // Edit trip screen

    // Verify all the infos

    // Change some data (name because easier to test)

    // Save

    // Back to the trip info screen

    // Click on edit trip

    // Delete the trip

    // Back to my trips

    // Assert no trips

    // Go back to profile

    // Log out

    // Check that we are on landing screen

    // TODO: assert that the preferences have been saved in the profile screen by looking that
    // EARLY_BIRD is there but not NIGHT_OWL
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }

  /**
   * Done using AI Returns a Firebase Timestamp representing midnight (00:00) of the same day as the
   * given [timestamp] in the system default time zone.
   */
  private fun getMidnightTimestamp(
      timestamp: Timestamp,
      zone: ZoneId = ZoneId.systemDefault()
  ): Timestamp {
    val localDate: LocalDate = Instant.ofEpochSecond(timestamp.seconds).atZone(zone).toLocalDate()
    val midnightInstant = localDate.atStartOfDay(zone).toInstant()
    return Timestamp(midnightInstant.epochSecond, midnightInstant.nano)
  }
}
