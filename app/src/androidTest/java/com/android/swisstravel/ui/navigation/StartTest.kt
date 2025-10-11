package com.android.swisstravel.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.swisstravel.utils.FakeCredentialManager
import com.android.swisstravel.utils.FakeJwtGenerator
import com.android.swisstravel.utils.FirebaseEmulator
import com.android.swisstravel.utils.FirestoreSwissTravelTest
import com.android.swisstravel.utils.UI_WAIT_TIMEOUT
import com.github.swent.swisstravel.SwissTravelApp
import com.github.swent.swisstravel.ui.authentication.SignInScreenTestTags.LOGIN_BUTTON
import com.github.swent.swisstravel.ui.profile.ProfileScreenTestTags
import com.google.firebase.auth.FirebaseAuth
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartTest : FirestoreSwissTravelTest() {

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  override fun setUp() {
    super.setUp()
    FirebaseEmulator.auth.signOut()
  }

  @Test
  fun testStartScreenDisplaysCorrectlyWhenLoggedOut() {
    assert(FirebaseEmulator.auth.currentUser == null)

    val fakeGoogleIdToken =
        FakeJwtGenerator.createFakeGoogleIdToken("12345", email = "test@example.com")

    val fakeCredentialManager = FakeCredentialManager.fake(fakeGoogleIdToken)

    composeTestRule.setContent { SwissTravelApp(credentialManager = fakeCredentialManager) }
    composeTestRule.onNodeWithTag(LOGIN_BUTTON).assertIsDisplayed().performClick()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) { FirebaseEmulator.auth.currentUser != null }

    composeTestRule.onNodeWithTag(ProfileScreenTestTags.DISPLAY_NAME).assertIsDisplayed()
  }

  @Test
  fun testStartScreenDisplaysCorrectlyWhenLoggedIn() {

    FirebaseAuth.getInstance().signInAnonymously()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) { FirebaseEmulator.auth.currentUser != null }
    composeTestRule.setContent { SwissTravelApp() }

    composeTestRule.onNodeWithTag(LOGIN_BUTTON).assertIsNotDisplayed()
  }
}
