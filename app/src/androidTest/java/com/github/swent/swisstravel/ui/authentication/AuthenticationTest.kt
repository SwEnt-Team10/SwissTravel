package com.github.swent.swisstravel.ui.authentication

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.swent.swisstravel.model.authentication.AuthRepositoryFirebase
import com.github.swent.swisstravel.ui.authentication.SignInScreenTestTags.APP_LOGO
import com.github.swent.swisstravel.ui.authentication.SignInScreenTestTags.GOOGLE_LOGIN_BUTTON
import com.github.swent.swisstravel.ui.authentication.SignInScreenTestTags.LOGIN_BUTTON
import com.github.swent.swisstravel.ui.authentication.SignInScreenTestTags.NAME
import com.github.swent.swisstravel.ui.theme.SwissTravelTheme
import com.github.swent.swisstravel.utils.FakeCredentialManager
import com.github.swent.swisstravel.utils.FakeJwtGenerator
import com.github.swent.swisstravel.utils.FirebaseEmulator
import com.github.swent.swisstravel.utils.FirestoreSwissTravelTest
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthenticationTest : FirestoreSwissTravelTest() {

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  override fun setUp() {
    super.setUp()
    FirebaseEmulator.auth.signOut()
  }

  val viewModel: SignInViewModel = SignInViewModel(AuthRepositoryFirebase(FirebaseEmulator.auth))

  @Test
  fun testSignInScreenDisplaysCorrectly() {
    composeTestRule.setContent { SwissTravelTheme { SignInScreen() } }

    composeTestRule.onNodeWithTag(APP_LOGO).assertIsDisplayed()
    composeTestRule.onNodeWithTag(LOGIN_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NAME).assertIsDisplayed()
  }

  @Test
  fun google_sign_in_is_configured() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    val resourceId =
        context.resources.getIdentifier("default_web_client_id", "string", context.packageName)

    // Skip test if resource doesn't exist (useful for CI environments)
    assumeTrue("Google Sign-In not configured - skipping test", resourceId != 0)

    val clientId = context.getString(resourceId)
    assertTrue(
        "Invalid Google client ID format: $clientId", clientId.endsWith(".googleusercontent.com"))
  }

  @Test
  fun canSignInWithGoogle() {
    val fakeGoogleIdToken =
        FakeJwtGenerator.createFakeGoogleIdToken("12345", email = "test@example.com")

    val fakeCredentialManager = FakeCredentialManager.fake(fakeGoogleIdToken)

    composeTestRule.setContent {
      SignInScreen(viewModel, credentialManager = fakeCredentialManager)
    }
    composeTestRule.onNodeWithTag(GOOGLE_LOGIN_BUTTON).assertIsDisplayed().performClick()
  }
}
