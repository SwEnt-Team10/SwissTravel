package com.android.swisstravel.e2e

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.swisstravel.utils.E2E_WAIT_TIMEOUT
import com.android.swisstravel.utils.FakeCredentialManager
import com.android.swisstravel.utils.FakeJwtGenerator
import com.android.swisstravel.utils.FirebaseEmulator
import com.android.swisstravel.utils.SwissTravelTest
import com.github.swent.swisstravel.SwissTravelApp
import com.github.swent.swisstravel.ui.authentication.SignInScreenTestTags.LOGIN_BUTTON
import com.github.swent.swisstravel.ui.navigation.NavigationTestTags
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
class E2EUserFlowTest : SwissTravelTest() {

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  override fun setUp() {
    super.setUp()
    FirebaseEmulator.auth.signOut()
  }

  @Test
  fun user_can_sign_in_and_navigate_across_tabs() {
    val fakeGoogleIdToken =
        FakeJwtGenerator.createFakeGoogleIdToken(name = "Test User", email = "test@example.com")
    val fakeCredentialManager = FakeCredentialManager.fake(fakeGoogleIdToken)

    // Start app logged out
    composeTestRule.setContent { SwissTravelApp(credentialManager = fakeCredentialManager) }
    composeTestRule.onNodeWithTag(LOGIN_BUTTON).assertExists().performClick()

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
    composeTestRule.checkCurrentTripScreenIsDisplayed()
    composeTestRule.checkMyTripsScreenIsNotDisplayed()
    composeTestRule.checkProfileScreenIsNotDisplayed()

    // Navigate to My Trips and verify
    composeTestRule.onNodeWithTag(NavigationTestTags.MY_TRIPS_TAB).performClick()
    composeTestRule.checkCurrentTripScreenIsNotDisplayed()
    composeTestRule.checkMyTripsScreenIsDisplayed()
    composeTestRule.checkProfileScreenIsNotDisplayed()

    // Navigate to Profile and verify
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_TAB).performClick()
    composeTestRule.checkCurrentTripScreenIsNotDisplayed()
    composeTestRule.checkMyTripsScreenIsNotDisplayed()
    composeTestRule.checkProfileScreenIsDisplayed()
  }
}
