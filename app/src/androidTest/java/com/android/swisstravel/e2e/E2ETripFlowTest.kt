package com.android.swisstravel.e2e

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.swisstravel.utils.SwissTravelTest
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class E2ETripFlowTest : SwissTravelTest() {

  //  @get:Rule val composeTestRule = createComposeRule()
  //
  //  private lateinit var todayLabel: String
  //  private lateinit var otherDayLabel: String
  //
  //  @Before
  //  override fun setUp() {
  //    super.setUp()
  //    FirebaseEmulator.clearAuthEmulator()
  //    FirebaseEmulator.auth.signOut()
  //    val today = LocalDate.now()
  //    todayLabel = "Trip from $today"
  //    val other = today.plusDays(5)
  //    otherDayLabel = "Trip from $other"
  //  }
  //
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
  //    composeTestRule.onNodeWithTag(LOGIN_BUTTON).assertExists().performClick()
  //    waitForMainUi()
  //
  //    // 2) Go to current trip
  //    composeTestRule.onNodeWithTag(NavigationTestTags.CURRENT_TRIP_TAB).performClick()
  //
  // composeTestRule.onNodeWithTag(CurrentTripScreenTestTags.CREATE_TRIP_BUTTON).assertIsDisplayed()
  //
  //    // 3) Create a trip (accept default dates → today)
  //    createTrip(selectNonToday = false)
  //
  //    // 4) See that the trip is displayed in My Trips
  //    waitForMainUi()
  //    composeTestRule
  //        .onNodeWithTag(NavigationTestTags.MY_TRIPS_TAB, useUnmergedTree = true)
  //        .assertIsDisplayed()
  //        .performClick()
  //    composeTestRule.onNodeWithText(todayLabel, useUnmergedTree = true).assertIsDisplayed()
  //
  //    // 5) Go back to current trip and create another trip with non-today date
  //    composeTestRule.onNodeWithTag(NavigationTestTags.CURRENT_TRIP_TAB).performClick()
  //    createTrip(selectNonToday = true)
  //
  //    // 6) Verify second trip is displayed in My Trips
  //    waitForMainUi()
  //    composeTestRule
  //        .onNodeWithTag(NavigationTestTags.MY_TRIPS_TAB, useUnmergedTree = true)
  //        .assertIsDisplayed()
  //        .performClick()
  //    composeTestRule.onNodeWithText(otherDayLabel, useUnmergedTree = true).assertIsDisplayed()
  //
  //    // 7) Go to profile and modify a preference
  //    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_TAB).performClick()
  //
  //    // Wait explicitly until the preference switch node appears in the Compose tree
  //    composeTestRule.waitUntil(E2E_WAIT_TIMEOUT) {
  //      composeTestRule
  //          .onAllNodesWithTag(
  //              ProfileScreenTestTags.preferenceSwitchTag("Museums"), useUnmergedTree = true)
  //          .fetchSemanticsNodes()
  //          .isNotEmpty()
  //    }
  //    // Now safely interact
  //    composeTestRule
  //        .onNodeWithTag(ProfileScreenTestTags.preferenceSwitchTag("Museums"), useUnmergedTree =
  // true)
  //        .performScrollTo()
  //        .performClick()
  //
  //    // 8) Log out
  //    composeTestRule
  //        .onNodeWithText("Sign Out", useUnmergedTree = true)
  //        .performScrollTo()
  //        .performClick()
  //    composeTestRule.waitUntil(E2E_WAIT_TIMEOUT) {
  //      composeTestRule.onAllNodesWithTag(LOGIN_BUTTON).fetchSemanticsNodes().isNotEmpty()
  //    }
  //
  //    // 9) Log in with another account
  //    composeTestRule.onNodeWithTag(LOGIN_BUTTON).assertIsDisplayed().performClick()
  //    waitForMainUi()
  //
  //    // 10) Verify no other trip is displayed
  //    waitForMainUi()
  //    composeTestRule
  //        .onNodeWithTag(NavigationTestTags.MY_TRIPS_TAB, useUnmergedTree = true)
  //        .assertIsDisplayed()
  //        .performClick()
  //    // Expect empty messages to be displayed for a fresh account
  //    composeTestRule
  //        .onNodeWithTag(MyTripsScreenTestTags.EMPTY_CURRENT_TRIP_MSG, useUnmergedTree = true)
  //        .assertIsDisplayed()
  //    composeTestRule
  //        .onNodeWithTag(MyTripsScreenTestTags.EMPTY_UPCOMING_TRIPS_MSG, useUnmergedTree = true)
  //        .assertIsDisplayed()
  //
  //    // 11) Create a new trip for second account
  //    composeTestRule.onNodeWithTag(NavigationTestTags.CURRENT_TRIP_TAB).performClick()
  //    createTrip(selectNonToday = false)
  //
  //    // 12) Verify it is displayed in My Trips
  //    waitForMainUi()
  //    composeTestRule
  //        .onNodeWithTag(NavigationTestTags.MY_TRIPS_TAB, useUnmergedTree = true)
  //        .assertIsDisplayed()
  //        .performClick()
  //    composeTestRule.onNodeWithText(todayLabel, useUnmergedTree = true).assertIsDisplayed()
  //
  //    // 13) Log out
  //    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_TAB).performClick()
  //    composeTestRule
  //        .onNodeWithText("Sign Out", useUnmergedTree = true)
  //        .performScrollTo()
  //        .performClick()
  //  }
  //
  //  private fun waitForMainUi() {
  //    composeTestRule.waitUntil(E2E_WAIT_TIMEOUT) {
  //      composeTestRule
  //          .onAllNodesWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU)
  //          .fetchSemanticsNodes()
  //          .isNotEmpty()
  //    }
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
  //    // Next from date screen
  //    composeTestRule.onNodeWithTag(TripDateTestTags.NEXT).performClick()
  //    composeTestRule.waitForIdle()
  //
  //    // TripTravelersScreen: accept defaults
  //    composeTestRule.onNodeWithTag(TripTravelersTestTags.NEXT).performClick()
  //    composeTestRule.waitForIdle()
  //
  //    // TripPreferencesScreen: save
  //    composeTestRule.onNodeWithTag(TripPreferencesTestTags.DONE).performClick()
  //    composeTestRule.waitForIdle()
  //  }
}
