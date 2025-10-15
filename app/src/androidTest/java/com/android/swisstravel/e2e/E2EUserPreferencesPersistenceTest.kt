package com.android.swisstravel.e2e

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.swisstravel.utils.E2E_WAIT_TIMEOUT
import com.android.swisstravel.utils.FakeCredentialManager
import com.android.swisstravel.utils.FakeJwtGenerator
import com.android.swisstravel.utils.FirebaseEmulator
import com.android.swisstravel.utils.SwissTravelTest
import com.github.swent.swisstravel.SwissTravelApp
import com.github.swent.swisstravel.ui.authentication.SignInScreenTestTags.LOGIN_BUTTON
import com.github.swent.swisstravel.ui.navigation.NavigationTestTags
import com.github.swent.swisstravel.ui.profile.ProfileScreenTestTags
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class E2EUserPreferencesPersistenceTest : SwissTravelTest() {

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  override fun setUp() {
    super.setUp()
    FirebaseEmulator.clearAuthEmulator()
    FirebaseEmulator.auth.signOut()
  }

  @Test
  fun preferences_are_user_specific_across_sign_in_out_cycles() {
    val alice =
        FakeJwtGenerator.createFakeGoogleIdToken(name = "Alice", email = "alice@example.com")
    val bob = FakeJwtGenerator.createFakeGoogleIdToken(name = "Bob", email = "bob@example.com")
    val creds = FakeCredentialManager.sequence(alice, bob, alice)

    // Single setContent for the whole flow
    composeTestRule.setContent { SwissTravelApp(credentialManager = creds) }

    // Sign in as Alice
    composeTestRule.onNodeWithTag(LOGIN_BUTTON).assertExists().performClick()
    waitForMainUi()
    val aliceUid = FirebaseEmulator.auth.currentUser?.uid
    goToProfile()
    togglePreference("Museums")
    // Verify Museums is toggled ON for Alice
    getSwitchForLabel("Museums").assertIsOn()

    // Sign out
    signOutFromProfile()

    // Sign in as Bob (second credential from sequence)
    composeTestRule.onNodeWithTag(LOGIN_BUTTON).assertExists().performClick()
    waitForMainUi()
    val bobUid = FirebaseEmulator.auth.currentUser?.uid
    goToProfile()
    togglePreference("Hiking & Outdoor")
    // Verify Hiking is toggled ON for Bob
    getSwitchForLabel("Hiking & Outdoor").assertIsOn()

    // Sign out
    signOutFromProfile()

    // Sign back in as Alice (third credential from sequence)
    composeTestRule.onNodeWithTag(LOGIN_BUTTON).assertExists().performClick()
    waitForMainUi()
    val aliceAgainUid = FirebaseEmulator.auth.currentUser?.uid
    goToProfile()

    // Verify Alice's preference (Museums) is ON and Bob's (Hiking) is OFF
    getSwitchForLabel("Museums").assertIsOn()
    getSwitchForLabel("Hiking & Outdoor").assertIsOff()

    // Sanity: ensure different users actually signed in
    assert(aliceUid != null && bobUid != null && aliceAgainUid != null)
    assert(aliceUid != bobUid)
    assert(aliceUid == aliceAgainUid)
  }

  private fun waitForMainUi() {
    composeTestRule.waitForIdle()
    Thread.sleep(1000)
    composeTestRule.waitUntil(E2E_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
  }

  private fun goToProfile() {
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_TAB).performClick()
    composeTestRule.waitUntil(E2E_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(ProfileScreenTestTags.PREFERENCES_LIST)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
  }

  private fun signOutFromProfile() {
    composeTestRule
        .onNodeWithText("Sign Out", useUnmergedTree = true)
        .performScrollTo()
        .performClick()
    composeTestRule.waitUntil(E2E_WAIT_TIMEOUT) {
      composeTestRule.onAllNodesWithTag(LOGIN_BUTTON).fetchSemanticsNodes().isNotEmpty()
    }
  }

  private fun togglePreference(label: String) {
    composeTestRule
        .onNode(hasTestTag("preferenceSwitch:$label"), useUnmergedTree = true)
        .performScrollTo()
        .performClick()
  }

  private fun getSwitchForLabel(label: String) =
      composeTestRule.onNode(hasTestTag("preferenceSwitch:$label"), useUnmergedTree = true)
}
