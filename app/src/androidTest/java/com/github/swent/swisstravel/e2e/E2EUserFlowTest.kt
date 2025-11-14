package com.github.swent.swisstravel.e2e

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.swent.swisstravel.SwissTravelApp
import com.github.swent.swisstravel.ui.authentication.LandingScreenTestTags.SIGN_IN_BUTTON
import com.github.swent.swisstravel.ui.authentication.SignInScreenTestTags.GOOGLE_LOGIN_BUTTON
import com.github.swent.swisstravel.ui.navigation.NavigationTestTags
import com.github.swent.swisstravel.ui.theme.SwissTravelTheme
import com.github.swent.swisstravel.utils.E2E_WAIT_TIMEOUT
import com.github.swent.swisstravel.utils.FakeCredentialManager
import com.github.swent.swisstravel.utils.FakeJwtGenerator
import com.github.swent.swisstravel.utils.FirebaseEmulator
import com.github.swent.swisstravel.utils.FirestoreSwissTravelTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end happy-path user flow:
 * 1) Start logged out, see login button
 * 2) Click Google sign-in (mocked)
 * 3) See bottom navigation, navigate between tabs and assert screens
 */
@RunWith(AndroidJUnit4::class)
class E2EUserFlowTest : FirestoreSwissTravelTest() {

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  override fun setUp() {
    super.setUp()
    FirebaseEmulator.auth.signOut()
    FirebaseEmulator.clearAuthEmulator()
    val fakeGoogleIdToken =
        FakeJwtGenerator.createFakeGoogleIdToken(name = "Test User 2", email = "test2@example.com")
    val fakeCredentialManager = FakeCredentialManager.fake(fakeGoogleIdToken)

    // Start app logged out
    composeTestRule.setContent {
      SwissTravelTheme { SwissTravelApp(credentialManager = fakeCredentialManager) }
    }
  }

  @Test
  fun user_can_sign_in_and_navigate_across_tabs() {
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

    // Navigate to Current Trip and verify
    composeTestRule.onNodeWithTag(NavigationTestTags.CURRENT_TRIP_TAB).performClick()
    composeTestRule.checkCurrentTripScreenEmptyIsDisplayed()
    composeTestRule.checkMyTripsScreenIsNotDisplayed()
    composeTestRule.checkProfileScreenIsNotDisplayed()

    // Navigate to My Trips and verify
    composeTestRule.onNodeWithTag(NavigationTestTags.MY_TRIPS_TAB).performClick()
    composeTestRule.checkCurrentTripScreenIsNotDisplayed()
    composeTestRule.checkMyTripsScreenIsDisplayed()
    composeTestRule.checkProfileScreenIsNotDisplayed()

    // Navigate to Profile and verify
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_TAB).performClick()
    composeTestRule.waitForIdle()
    Thread.sleep(5000)
    composeTestRule.checkCurrentTripScreenIsNotDisplayed()
    composeTestRule.checkMyTripsScreenIsNotDisplayed()
    composeTestRule.checkProfileScreenIsDisplayed()
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }
}
