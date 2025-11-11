package com.github.swent.swisstravel.ui.authentication

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.swent.swisstravel.model.authentication.AuthRepository
import com.github.swent.swisstravel.model.authentication.AuthRepositoryFirebase
import com.github.swent.swisstravel.utils.FirebaseEmulator
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.model.Statement

@RunWith(AndroidJUnit4::class)
class SignUpTest {

  @get:Rule val composeTestRule = createComposeRule()

  @get:Rule
  val firebaseEmulatorRule = TestRule { base, _ ->
    object : Statement() {
      override fun evaluate() {
        FirebaseEmulator.clearAuthEmulator()
        base.evaluate()
      }
    }
  }

  private lateinit var authRepository: AuthRepository

  @Before
  fun setUp() {
    authRepository = AuthRepositoryFirebase(FirebaseEmulator.auth)
  }

  @Test
  fun signUpEndsWithSuccess() {
    var signUpSuccess = false

    composeTestRule.setContent {
      SignUpScreen(
          signUpViewModel = SignUpViewModel(repository = authRepository),
          onSignUpSuccess = { signUpSuccess = true })
    }
    // load the text fields with the correct credentials
    composeTestRule.onNodeWithTag(SignUpScreenTestTags.FIRST_NAME_FIELD).performTextInput("John")
    composeTestRule.onNodeWithTag(SignUpScreenTestTags.LAST_NAME_FIELD).performTextInput("Doe")
    composeTestRule
        .onNodeWithTag(SignUpScreenTestTags.EMAIL_FIELD)
        .performTextInput("john.doe@example.com")
    composeTestRule.onNodeWithTag(SignUpScreenTestTags.PASSWORD_FIELD).performTextInput("password")

    composeTestRule.onNodeWithTag(SignUpScreenTestTags.SIGN_UP_BUTTON).performClick()
    // checks whether the log in leads to a success -> it should pass
    composeTestRule.waitUntil(timeoutMillis = 10000) { signUpSuccess }
    assertTrue("Sign up success callback was not invoked.", signUpSuccess)
  }
}
