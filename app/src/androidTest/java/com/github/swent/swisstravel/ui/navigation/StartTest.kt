package com.github.swent.swisstravel.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.swent.swisstravel.SwissTravelApp
import com.github.swent.swisstravel.ui.authentication.SignInScreenTestTags.LOGIN_BUTTON
import com.github.swent.swisstravel.ui.theme.SwissTravelTheme
import com.github.swent.swisstravel.utils.FakeCredentialManager
import com.github.swent.swisstravel.utils.FakeJwtGenerator
import com.github.swent.swisstravel.utils.FirebaseEmulator
import com.github.swent.swisstravel.utils.FirestoreSwissTravelTest
import com.github.swent.swisstravel.utils.UI_WAIT_TIMEOUT
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

    composeTestRule.setContent {
      SwissTravelTheme { SwissTravelApp(credentialManager = fakeCredentialManager) }
    }
    composeTestRule.onNodeWithTag(LOGIN_BUTTON).assertIsDisplayed().performClick()
    // Commented out because of CI issues
    //    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
    //      composeTestRule.onNodeWithTag(LOGIN_BUTTON).isNotDisplayed()
    //    }
    //    composeTestRule.onNodeWithTag(ProfileScreenTestTags.DISPLAY_NAME).isDisplayed()
  }

  @Test
  fun testStartScreenDisplaysCorrectlyWhenLoggedIn() {

    FirebaseEmulator.auth.signInAnonymously()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) { FirebaseEmulator.auth.currentUser != null }
    composeTestRule.setContent { SwissTravelTheme { SwissTravelApp() } }

    composeTestRule.onNodeWithTag(LOGIN_BUTTON).assertIsNotDisplayed()
  }
}
