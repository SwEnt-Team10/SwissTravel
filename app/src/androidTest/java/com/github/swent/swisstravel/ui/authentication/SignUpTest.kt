package com.github.swent.swisstravel.ui.authentication

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SignUpTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var mockAuthRepository: MockAuthRepository
  private lateinit var signUpViewModel: SignUpViewModel

  @Before
  fun setUp() {
    // 1. Initialize the mock repository before each test
    mockAuthRepository = MockAuthRepository()
    // 2. Inject the mock into the ViewModel
    signUpViewModel = SignUpViewModel(repository = mockAuthRepository)
  }

  /** Fills the sign-up form with valid data. */
  private fun fillSignUpForm() {
    composeTestRule.onNodeWithTag(SignUpScreenTestTags.FIRST_NAME_FIELD).performTextInput("John")
    composeTestRule.onNodeWithTag(SignUpScreenTestTags.LAST_NAME_FIELD).performTextInput("Doe")
    composeTestRule
        .onNodeWithTag(SignUpScreenTestTags.EMAIL_FIELD)
        .performTextInput("john.doe@example.com")
    composeTestRule.onNodeWithTag(SignUpScreenTestTags.PASSWORD_FIELD).performTextInput("password")
  }

  @Test
  fun successfulSignUp_transitionsTo_pendingVerificationScreen() {
    // Set up the UI with the ViewModel containing the mock
    composeTestRule.setContent { SignUpScreen(signUpViewModel = signUpViewModel) }

    // Fill the form and click the sign-up button
    fillSignUpForm()
    composeTestRule.onNodeWithTag(SignUpScreenTestTags.SIGN_UP_BUTTON).performClick()

    // Verify that the Pending Verification screen is now displayed
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(SignUpScreenTestTags.PENDING_VERIFICATION_SCREEN).assertExists()
    // Also assert the original sign-up button is gone
    composeTestRule.onNodeWithTag(SignUpScreenTestTags.SIGN_UP_BUTTON).assertDoesNotExist()
  }

  @Test
  fun clickingDone_whenEmailIsNotVerified_doesNotTriggerSuccess() {
    var signUpSuccess = false
    // Set up the UI and navigate to the pending verification screen
    composeTestRule.setContent {
      SignUpScreen(signUpViewModel = signUpViewModel, onSignUpSuccess = { signUpSuccess = true })
    }
    fillSignUpForm()
    composeTestRule.onNodeWithTag(SignUpScreenTestTags.SIGN_UP_BUTTON).performClick()

    // Configure the mock: The next check will return `false` (not verified)
    mockAuthRepository.checkVerificationResult = Result.success(false)

    // Click the "Done" button
    composeTestRule.onNodeWithTag(SignUpScreenTestTags.DONE_BUTTON).performClick()

    // The success callback should NOT have been called
    composeTestRule.waitForIdle()
    assertFalse("onSignUpSuccess should not be called when email is not verified.", signUpSuccess)
  }

  @Test
  fun clickingDone_whenEmailIsVerified_triggersSuccessCallback() {
    var signUpSuccess = false
    // Set up the UI and navigate to the pending verification screen
    composeTestRule.setContent {
      SignUpScreen(signUpViewModel = signUpViewModel, onSignUpSuccess = { signUpSuccess = true })
    }
    fillSignUpForm()
    composeTestRule.onNodeWithTag(SignUpScreenTestTags.SIGN_UP_BUTTON).performClick()
    composeTestRule.waitForIdle() // Wait for the UI to transition

    // Should return true
    mockAuthRepository.checkVerificationResult = Result.success(true)

    // Click the "Done" button
    composeTestRule.onNodeWithTag(SignUpScreenTestTags.DONE_BUTTON).performClick()

    // The success callback should be called
    composeTestRule.waitUntil { signUpSuccess }
    assertTrue("onSignUpSuccess should be called after verification.", signUpSuccess)
  }
}
