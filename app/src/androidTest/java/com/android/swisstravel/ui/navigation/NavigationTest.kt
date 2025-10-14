package com.android.swisstravel.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.android.swisstravel.utils.SwissTravelTest
import com.github.swent.swisstravel.SwissTravelApp
import com.github.swent.swisstravel.ui.navigation.NavigationTestTags
import com.google.firebase.auth.FirebaseAuth
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Inspired from the B3 of the SwEnt course at EPFL */
class NavigationTest : SwissTravelTest() {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Before
  override fun setUp() {
    super.setUp()
    FirebaseAuth.getInstance().signInAnonymously()
    composeTestRule.setContent { SwissTravelApp() }
  }

  @Test
  fun testTagsAreCorrectlySet() {
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.CURRENT_TRIP_TAB).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_TAB).assertIsDisplayed()
    // composeTestRule.onNodeWithTag(NavigationTestTags.MY_TRIPS_TAB).assertIsDisplayed()
  }

  @Test
  fun bottomNavigationIsDisplayedForCurrentTrip() {
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()
  }

  // TODO Uncomment this when My trips has been implemented
  //    @Test
  //    fun bottomNavigationIsDisplayedForMyTrips() {
  //        composeTestRule.onNodeWithTag(NavigationTestTags.MY_TRIPS_TAB).performClick()
  //
  // composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()
  //    }

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
    //        composeTestRule.onNodeWithTag(NavigationTestTags.CURRENT_TRIP_TAB).performClick()
    //        composeTestRule.checkCurrentTripScreenIsDisplayed()
    //        composeTestRule.checkMyTripsScreenIsNotDisplayed()
    //        composeTestRule.checkProfileScreenIsNotDisplayed()
    //        composeTestRule.onNodeWithTag(NavigationTestTags.MY_TRIPS_TAB).performClick()
    //        composeTestRule.checkCurrentTripScreenIsNotDisplayed()
    //        composeTestRule.checkMyTripsScreenIsDisplayed()
    //        composeTestRule.checkProfileScreenIsNotDisplayed()
    //        composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_TAB).performClick()
    //        composeTestRule.checkCurrentTripScreenIsNotDisplayed()
    //        composeTestRule.checkMyTripsScreenIsNotDisplayed()
    //        composeTestRule.checkProfileScreenIsDisplayed()
    //        composeTestRule.onNodeWithTag(NavigationTestTags.CURRENT_TRIP_TAB).performClick()
    //        composeTestRule.checkCurrentTripScreenIsDisplayed()
    //        composeTestRule.checkMyTripsScreenIsNotDisplayed()
    //        composeTestRule.checkProfileScreenIsNotDisplayed()
  }

  @Test
  fun canNavigateBackToMyTripsAndBackToCurrentTripUsingSystemBack() {
    //        composeTestRule.onNodeWithTag(NavigationTestTags.MY_TRIPS_TAB).performClick()
    //        composeTestRule.checkMyTripsScreenIsDisplayed()
    //        composeTestRule.checkCurrentTripScreenIsNotDisplayed()
    //        composeTestRule.checkProfileScreenIsNotDisplayed()
    //        pressBack(shouldFinish = false)
    //        composeTestRule.checkCurrentTripScreenIsDisplayed()
    //        composeTestRule.checkMyTripsScreenIsNotDisplayed()
    //        composeTestRule.checkProfileScreenIsNotDisplayed()
  }

  private fun pressBack(shouldFinish: Boolean) {
    composeTestRule.activityRule.scenario.onActivity { activity ->
      activity.onBackPressedDispatcher.onBackPressed()
    }
    composeTestRule.waitUntil { composeTestRule.activity.isFinishing == shouldFinish }
    assertEquals(shouldFinish, composeTestRule.activity.isFinishing)
  }
}
