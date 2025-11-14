package com.github.swent.swisstravel.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.github.swent.swisstravel.SwissTravelApp
import com.github.swent.swisstravel.ui.authentication.LandingScreenTestTags
import com.github.swent.swisstravel.ui.profile.ProfileScreenTestTags
import com.github.swent.swisstravel.ui.theme.SwissTravelTheme
import com.github.swent.swisstravel.utils.FirebaseEmulator
import com.github.swent.swisstravel.utils.InMemorySwissTravelTest
import com.github.swent.swisstravel.utils.UI_WAIT_TIMEOUT
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Inspired from the B3 of the SwEnt course at EPFL */
class NavigationTest : InMemorySwissTravelTest() {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Before
  override fun setUp() {
    super.setUp()
    FirebaseEmulator.clearAuthEmulator()
    FirebaseEmulator.auth.signInAnonymously()
    composeTestRule.setContent { SwissTravelTheme { SwissTravelApp() } }
  }

  @Test
  fun testTagsAreCorrectlySet() {
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.CURRENT_TRIP_TAB).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_TAB).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.MY_TRIPS_TAB).assertIsDisplayed()
  }

  @Test
  fun bottomNavigationIsDisplayedForCurrentTrip() {
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()
  }

  @Test
  fun bottomNavigationIsDisplayedForMyTrips() {
    composeTestRule.onNodeWithTag(NavigationTestTags.MY_TRIPS_TAB).performClick()

    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()
  }

  @Test
  fun bottomNavigationIsDisplayedForProfile() {
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_TAB).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()
  }

  @Test
  fun tabsAreClickable() {
    composeTestRule
        .onNodeWithTag(NavigationTestTags.MY_TRIPS_TAB)
        .assertIsDisplayed()
        .performClick()
    composeTestRule
        .onNodeWithTag(NavigationTestTags.CURRENT_TRIP_TAB)
        .assertIsDisplayed()
        .performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_TAB).assertIsDisplayed().performClick()
  }

  @Test
  fun navigationBetweenTabsWorks() {
    composeTestRule.onNodeWithTag(NavigationTestTags.CURRENT_TRIP_TAB).performClick()
    composeTestRule.checkCurrentTripScreenEmptyIsDisplayed()
    composeTestRule.checkMyTripsScreenIsNotDisplayed()
    composeTestRule.checkProfileScreenIsNotDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.MY_TRIPS_TAB).performClick()
    composeTestRule.checkCurrentTripScreenIsNotDisplayed()
    composeTestRule.checkMyTripsScreenIsDisplayed()
    composeTestRule.checkProfileScreenIsNotDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_TAB).performClick()
    composeTestRule.checkCurrentTripScreenIsNotDisplayed()
    composeTestRule.checkMyTripsScreenIsNotDisplayed()
    composeTestRule.checkProfileScreenIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.CURRENT_TRIP_TAB).performClick()
    composeTestRule.checkCurrentTripScreenEmptyIsDisplayed()
    composeTestRule.checkMyTripsScreenIsNotDisplayed()
    composeTestRule.checkProfileScreenIsNotDisplayed()
  }

  @Test
  fun canNavigateBackToMyTripsAndBackToCurrentTripUsingSystemBack() {
    composeTestRule.onNodeWithTag(NavigationTestTags.MY_TRIPS_TAB).performClick()
    composeTestRule.checkMyTripsScreenIsDisplayed()
    composeTestRule.checkCurrentTripScreenIsNotDisplayed()
    composeTestRule.checkProfileScreenIsNotDisplayed()
    pressBack(shouldFinish = false)
    composeTestRule.checkCurrentTripScreenEmptyIsDisplayed()
    composeTestRule.checkMyTripsScreenIsNotDisplayed()
    composeTestRule.checkProfileScreenIsNotDisplayed()
  }

  @Test
  fun whenLoggingOut_backPressExitsApp() {
    // 1. Navigate to the profile screen
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_TAB).performClick()

    // 2. Scroll to the logout button and click it
    composeTestRule
        .onNodeWithTag(ProfileScreenTestTags.LOGOUT_BUTTON)
        .performScrollTo()
        .performClick()

    // 3. Wait for navigation and verify we are on the landing screen
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(LandingScreenTestTags.SIGN_UP_BUTTON).assertIsDisplayed()
    composeTestRule.checkProfileScreenIsNotDisplayed()

    // 4. Press the back button and assert that the activity finishes
    pressBack(shouldFinish = true)
  }

  private fun pressBack(shouldFinish: Boolean) {
    composeTestRule.activityRule.scenario.onActivity { activity ->
      activity.onBackPressedDispatcher.onBackPressed()
    }
    // Give the system a moment to process the back press and activity state change
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule.activity.isFinishing == shouldFinish
    }
    // Final assertion to confirm the state
    assertEquals(shouldFinish, composeTestRule.activity.isFinishing)
  }
}
