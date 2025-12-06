package com.github.swent.swisstravel.ui.composable

import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class ErrorScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun errorScreenIsDisplayed() {
    composeTestRule.setContent {
      ErrorScreen(
          message = "error screen test",
          topBarTitle = "Test",
          backButtonDescription = "testButton",
          onRetry = {},
          onBack = {})
    }
    composeTestRule.onNodeWithTag(ErrorScreenTestTags.ERROR_SCREEN_TOP_BAR).isDisplayed()
    composeTestRule.onNodeWithTag(ErrorScreenTestTags.ERROR_SCREEN_TOP_BAR_TITLE).isDisplayed()
    composeTestRule.onNodeWithTag(ErrorScreenTestTags.ERROR_MESSAGE).isDisplayed()
    composeTestRule.onNodeWithTag(ErrorScreenTestTags.RETRY_BUTTON).isDisplayed()
  }

  @Test
  fun clickOnRetryWork() {
    var retry = false
    composeTestRule.setContent {
      ErrorScreen(
          message = "error screen test",
          topBarTitle = "Retry test",
          backButtonDescription = "testButton",
          onBack = {},
          onRetry = { retry = true })
    }
    composeTestRule.onNodeWithTag(ErrorScreenTestTags.RETRY_BUTTON).performClick()
    assert(retry)
  }
}
