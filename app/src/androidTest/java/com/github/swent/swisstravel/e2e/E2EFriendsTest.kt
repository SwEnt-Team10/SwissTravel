package com.github.swent.swisstravel.e2e

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.rule.GrantPermissionRule
import com.github.swent.swisstravel.SwissTravelApp
import com.github.swent.swisstravel.model.user.UserRepositoryFirebase
import com.github.swent.swisstravel.ui.authentication.LandingScreenTestTags.SIGN_IN_BUTTON
import com.github.swent.swisstravel.ui.authentication.SignInScreenTestTags.GOOGLE_LOGIN_BUTTON
import com.github.swent.swisstravel.ui.friends.AddFriendsScreenTestTags
import com.github.swent.swisstravel.ui.friends.FriendElementTestTags
import com.github.swent.swisstravel.ui.friends.FriendsScreenTestTags
import com.github.swent.swisstravel.ui.navigation.NavigationTestTags
import com.github.swent.swisstravel.ui.profile.ProfileScreenTestTags
import com.github.swent.swisstravel.ui.profile.ProfileSettingsScreenTestTags
import com.github.swent.swisstravel.ui.theme.SwissTravelTheme
import com.github.swent.swisstravel.utils.E2E_WAIT_TIMEOUT
import com.github.swent.swisstravel.utils.FakeCredentialManager
import com.github.swent.swisstravel.utils.FakeJwtGenerator
import com.github.swent.swisstravel.utils.FirebaseEmulator
import com.github.swent.swisstravel.utils.FirestoreSwissTravelTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Done with the help of AI.
 *
 * End-to-end Friend Flow Test:
 * 1) Alice logs in.
 * 2) Alice checks her UID/Name (to ensure she exists in DB).
 * 3) Alice logs out.
 * 4) Bob logs in.
 * 5) Bob navigates to Friends -> Add Friend.
 * 6) Bob searches for "Alice".
 * 7) Bob sends a friend request to Alice.
 * 8) Bob logs out.
 * 9) Alice logs in again.
 * 10) Alice navigates to Friends.
 * 11) Alice sees a pending request from Bob.
 * 12) Alice accepts the request.
 * 13) Alice verifies Bob is now in her friends list.
 */
class E2EFriendFlowTest : FirestoreSwissTravelTest() {

  @get:Rule val composeTestRule = createComposeRule()

  @get:Rule
  val permissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(
          android.Manifest.permission.ACCESS_FINE_LOCATION,
          android.Manifest.permission.ACCESS_COARSE_LOCATION)

  private val aliceName = "Alice Tester"
  private val aliceEmail = "alice@example.com"

  private val bobName = "Bob Builder"
  private val bobEmail = "bob@example.com"

  @Before
  override fun setUp() {
    super.setUp()
    FirebaseEmulator.auth.signOut()
    FirebaseEmulator.clearAuthEmulator()
  }

  @Test
  fun user_can_send_and_accept_friend_request() {
    // --- STEP 0: SETUP
    // Create tokens for two distinct users
    val aliceToken = FakeJwtGenerator.createFakeGoogleIdToken(name = aliceName, email = aliceEmail)
    val bobToken = FakeJwtGenerator.createFakeGoogleIdToken(name = bobName, email = bobEmail)

    // Sequence the logins: 1. Alice, 2. Bob, 3. Alice
    val fakeCredentialManager = FakeCredentialManager.sequence(aliceToken, bobToken, aliceToken)

    // Start app
    composeTestRule.setContent {
      SwissTravelTheme { SwissTravelApp(credentialManager = fakeCredentialManager) }
    }

    // --- STEP 1: LOGIN AS ALICE ---
    composeTestRule.onNodeWithTag(SIGN_IN_BUTTON).assertExists().performClick()
    composeTestRule.waitForIdle()
    loginWithGoogle()

    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_TAB).performClick()
    waitForTag(ProfileScreenTestTags.DISPLAY_NAME)

    // --- STEP 2: LOGOUT ALICE ---
    logout()

    // --- STEP 3: LOGIN AS BOB ---
    loginWithGoogle()

    // --- STEP 4: BOB SENDS REQUEST TO ALICE ---
    // Go to Friends Tab
    composeTestRule.onNodeWithTag(NavigationTestTags.FRIENDS_TAB).performClick()
    waitForTag(FriendsScreenTestTags.ADD_FRIEND_BUTTON)

    // Click Add Friend
    composeTestRule.onNodeWithTag(FriendsScreenTestTags.ADD_FRIEND_BUTTON).performClick()
    waitForTag(AddFriendsScreenTestTags.ADD_FRIEND_SEARCH_FIELD)

    // Search for Alice
    composeTestRule
        .onNodeWithTag(AddFriendsScreenTestTags.ADD_FRIEND_SEARCH_FIELD)
        .performTextInput(aliceName)

    // Wait for results
    composeTestRule.waitUntil(E2E_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(AddFriendsScreenTestTags.ADD_FRIEND_RESULTS_LIST)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Click on Alice in the list
    val userRepo = UserRepositoryFirebase(FirebaseEmulator.auth, FirebaseEmulator.firestore)
    val alice = runBlocking { userRepo.getUserByNameOrEmail(aliceName).first() }
    composeTestRule.onNodeWithTag(FriendElementTestTags.getTestTagForFriend(alice)).performClick()

    composeTestRule.waitForIdle()

    // --- STEP 5: LOGOUT BOB ---
    // Need to go to profile to logout
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_TAB).performClick()
    waitForTag(ProfileScreenTestTags.SETTINGS_BUTTON)
    logout()

    // --- STEP 6: LOGIN AS ALICE AGAIN ---
    loginWithGoogle()

    // --- STEP 7: ALICE ACCEPTS REQUEST ---
    composeTestRule.onNodeWithTag(NavigationTestTags.FRIENDS_TAB).performClick()
    waitForTag(FriendsScreenTestTags.PENDING_SECTION_CARD)

    // Open Pending Section
    composeTestRule.onNodeWithTag(FriendsScreenTestTags.PENDING_SECTION_HEADER).performClick()

    // Verify Bob is in pending list
    composeTestRule.onNodeWithText(bobName).assertIsDisplayed()

    // Accept Bob
    composeTestRule.onNodeWithTag(FriendElementTestTags.ACCEPT_BUTTON).performClick()

    // --- STEP 8: VERIFY FRIENDSHIP ---
    composeTestRule.waitForIdle()

    // Bob should now be in the main friend list
    composeTestRule.waitUntil(E2E_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(FriendsScreenTestTags.FRIENDS_LIST)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithText(bobName).assertIsDisplayed()
  }

  // -- Helper functions --

  private fun loginWithGoogle() {
    composeTestRule.onNodeWithTag(GOOGLE_LOGIN_BUTTON).assertExists().performClick()

    // Wait for main app to load
    composeTestRule.waitUntil(E2E_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
  }

  private fun logout() {
    waitForTag(ProfileScreenTestTags.SETTINGS_BUTTON)
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.SETTINGS_BUTTON).performClick()
    waitForTag(ProfileSettingsScreenTestTags.LOGOUT_BUTTON)
    composeTestRule.onNodeWithTag(ProfileSettingsScreenTestTags.LOGOUT_BUTTON).performClick()

    // Wait for Landing Screen
    composeTestRule.waitUntil(E2E_WAIT_TIMEOUT) {
      composeTestRule.onAllNodesWithTag(GOOGLE_LOGIN_BUTTON).fetchSemanticsNodes().isNotEmpty()
    }
  }

  private fun waitForTag(tag: String) {
    composeTestRule.waitUntil(E2E_WAIT_TIMEOUT) {
      composeTestRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    }
  }
}
