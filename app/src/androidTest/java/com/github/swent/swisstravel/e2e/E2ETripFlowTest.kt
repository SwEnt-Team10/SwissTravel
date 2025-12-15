package com.github.swent.swisstravel.e2e

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.swent.swisstravel.utils.FirebaseEmulator
import com.github.swent.swisstravel.utils.FirestoreSwissTravelTest
import java.time.LocalDate
import org.junit.Before
import org.junit.Rule
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

  //  @Test
  //  fun full_multi_account_trip_and_prefs_flow() {
  //    val alice =
  //        FakeJwtGenerator.createFakeGoogleIdToken(name = "Alice", email = "alice@example.com")
  //    val bob = FakeJwtGenerator.createFakeGoogleIdToken(name = "Bob", email = "bob@example.com")
  //    val creds = FakeCredentialManager.sequence(alice, bob)
  //
  //    composeTestRule.setContent { SwissTravelApp(credentialManager = creds) }
  //
  //    // 1) Log in as first account
  //    composeTestRule.loginWithGoogle(true)
  //    composeTestRule.waitForMainUi()
  //
  //    // 2) Go to current trip
  //    composeTestRule.onNodeWithTag(NavigationTestTags.CURRENT_TRIP_TAB).performClick()
  //
  //
  // composeTestRule.onNodeWithTag(CurrentTripScreenTestTags.CREATE_TRIP_BUTTON).assertIsDisplayed()
  //
  //    // 3) Create a trip
  //    createTrip(selectNonToday = false)
  //
  //    // 4) See that the trip is displayed in My Trips
  //    composeTestRule.waitForMainUi()
  //    composeTestRule
  //        .onNodeWithTag(NavigationTestTags.MY_TRIPS_TAB, useUnmergedTree = true)
  //        .assertIsDisplayed()
  //        .performClick()
  //
  //    // 5) Go back to current trip and create another trip with non-today date
  //    composeTestRule.onNodeWithTag(NavigationTestTags.CURRENT_TRIP_TAB).performClick()
  //    createTrip(selectNonToday = true)
  //
  //    // 6) Verify second trip is displayed in My Trips
  //    composeTestRule.waitForMainUi()
  //    composeTestRule
  //        .onNodeWithTag(NavigationTestTags.MY_TRIPS_TAB, useUnmergedTree = true)
  //        .assertIsDisplayed()
  //        .performClick()
  //
  //    val tripsAlice = runBlocking { repository.getAllTrips() }
  //    assertEquals(2, tripsAlice.size)
  //
  //    // 7) Go to profile and modify a preference
  //    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_TAB).performClick()
  //    composeTestRule.onNodeWithTag(ProfileScreenTestTags.SETTINGS_BUTTON).performClick()
  //    composeTestRule.waitForTag(ProfileSettingsScreenTestTags.EMAIL)
  //    composeTestRule
  //        .onNodeWithTag(ProfileSettingsScreenTestTags.PREFERENCES)
  //        .performScrollToNode(hasTestTag(ProfileSettingsScreenTestTags.PREFERENCES_TOGGLE))
  //
  // composeTestRule.onNodeWithTag(ProfileSettingsScreenTestTags.PREFERENCES_TOGGLE).performClick()
  //
  //    // Wait explicitly until the preference switch node appears in the Compose tree
  //    composeTestRule.waitUntil(E2E_WAIT_TIMEOUT) {
  //      composeTestRule
  //          .onAllNodesWithTag(
  //              PreferenceSelectorTestTags.getTestTagButton(Preference.MUSEUMS),
  //              useUnmergedTree = true)
  //          .fetchSemanticsNodes()
  //          .isNotEmpty()
  //    }
  //    // Now safely interact
  //    composeTestRule
  //        .onNodeWithTag(
  //            PreferenceSelectorTestTags.getTestTagButton(Preference.MUSEUMS), useUnmergedTree =
  // true)
  //        .performScrollTo()
  //        .performClick()
  //
  //    // 8) Log out
  //    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_BUTTON).performClick()
  //    composeTestRule.logout()
  //
  //    // 9) Log in with another account
  //    composeTestRule.loginWithGoogle(true)
  //    composeTestRule.waitForMainUi()
  //
  //    // 10) Verify no other trip is displayed
  //    composeTestRule.waitForMainUi()
  //    composeTestRule
  //        .onNodeWithTag(NavigationTestTags.MY_TRIPS_TAB, useUnmergedTree = true)
  //        .assertIsDisplayed()
  //        .performClick()
  //    // Expect empty messages to be displayed for a fresh account
  //    composeTestRule
  //        .onNodeWithTag(MyTripsScreenTestTags.EMPTY_CURRENT_TRIP_MSG, useUnmergedTree = true)
  //        .assertIsDisplayed()
  //
  //    // 11) Create a new trip for second account
  //    composeTestRule.onNodeWithTag(NavigationTestTags.CURRENT_TRIP_TAB).performClick()
  //    createTrip(selectNonToday = false)
  //
  //    // 12) Verify it is displayed in My Trips
  //    composeTestRule.waitForMainUi()
  //    composeTestRule
  //        .onNodeWithTag(NavigationTestTags.MY_TRIPS_TAB, useUnmergedTree = true)
  //        .assertIsDisplayed()
  //        .performClick()
  //    val tripsBob = runBlocking { repository.getAllTrips() }
  //    assertEquals(1, tripsBob.size)
  //
  //    // 13) Log out
  //    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_TAB).performClick()
  //    composeTestRule.logout()
  //  }
  //
  //  private fun createTrip(selectNonToday: Boolean) {
  //    composeTestRule.onNodeWithTag(CurrentTripScreenTestTags.CREATE_TRIP_BUTTON).performClick()
  //    Thread.sleep(1000)
  //    // TripDateScreen: optionally change start date to non-today
  //    if (selectNonToday) {
  //      val dateButtons = composeTestRule.onAllNodesWithTag(DateSelectorTestTags.DATE)
  //      dateButtons.assertCountEquals(2)
  //      dateButtons[0].performClick() // open start date picker
  //
  //      // Pick a different day in the current month (today + 5)
  //      val target = LocalDate.now().plusDays(5)
  //      onView(isAssignableFrom(android.widget.DatePicker::class.java))
  //          .perform(PickerActions.setDate(target.year, target.monthValue, target.dayOfMonth))
  //      onView(withId(android.R.id.button1)).perform(click())
  //    }
  //
  //    composeTestRule.onNodeWithTag(TripDateTestTags.NEXT).performClick()
  //    composeTestRule.waitForIdle()
  //
  //    composeTestRule.onNodeWithTag(TripTravelersTestTags.NEXT).performClick()
  //    composeTestRule.waitForIdle()
  //
  //    composeTestRule
  //        .onNodeWithTag(TripPreferencesTestTags.TRIP_PREFERENCE_CONTENT)
  //        .performScrollToNode(hasTestTag(TripPreferencesTestTags.DONE))
  //    composeTestRule.onNodeWithTag(TripPreferencesTestTags.DONE).performClick()
  //    composeTestRule.checkArrivalDepartureScreenIsDisplayed()
  //
  //    // Fill the text fields
  //    composeTestRule
  //        .onNodeWithTag(ArrivalDepartureTestTags.ARRIVAL_TEXTFIELD, useUnmergedTree = true)
  //        .performTextInput("epfl")
  //    composeTestRule.performClickOnLocationSuggestion()
  //
  //    composeTestRule
  //        .onNodeWithTag(ArrivalDepartureTestTags.DEPARTURE_TEXTFIELD, useUnmergedTree = true)
  //        .performTextInput("cafe de paris")
  //    composeTestRule.performClickOnLocationSuggestion()
  //
  //    composeTestRule.onNodeWithTag(ArrivalDepartureTestTags.NEXT_BUTTON).performClick()
  //
  //    composeTestRule.onNodeWithTag(TripFirstDestinationsTestTags.NEXT_BUTTON).performClick()
  //
  //    // Trip Summary
  //    composeTestRule.waitUntil(E2E_WAIT_TIMEOUT) {
  //      composeTestRule
  //          .onAllNodesWithTag(TripSummaryTestTags.TRIP_SUMMARY_SCREEN)
  //          .fetchSemanticsNodes()
  //          .isNotEmpty()
  //    }
  //    composeTestRule
  //        .onNodeWithTag(TripSummaryTestTags.TRIP_SUMMARY_SCREEN)
  //        .performScrollToNode(hasTestTag(TripSummaryTestTags.TRIP_NAME_FIELD))
  //    composeTestRule
  //        .onNodeWithTag(TripSummaryTestTags.TRIP_NAME_FIELD)
  //        .performTextInput("Awesome trip")
  //    composeTestRule.onNodeWithTag(TripSummaryTestTags.TRIP_NAME_FIELD).performImeAction()
  //
  //    composeTestRule
  //        .onNodeWithTag(TripSummaryTestTags.TRIP_SUMMARY_SCREEN)
  //        .performScrollToNode(hasTestTag(TripSummaryTestTags.CREATE_TRIP_BUTTON))
  //    composeTestRule.onNodeWithTag(TripSummaryTestTags.CREATE_TRIP_BUTTON).performClick()
  //    composeTestRule.waitForIdle()
  //  }
}
