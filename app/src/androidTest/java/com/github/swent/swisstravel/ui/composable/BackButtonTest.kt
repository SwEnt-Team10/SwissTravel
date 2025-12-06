package com.github.swent.swisstravel.ui.composable

import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import kotlin.test.Test
import org.junit.Rule

class BackButtonTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun backButtonIsDisplayed() {
    composeTestRule.setContent { BackButton(onBack = {}, contentDescription = "testButton") }
    composeTestRule.onNodeWithTag(BackButtonTestTag.BACK_BUTTON).isDisplayed()
  }

  @Test
  fun backButtonTriggerWhenClick() {
    var isClicked = false
    composeTestRule.setContent {
      BackButton(onBack = { isClicked = true }, contentDescription = "testButton")
    }
    composeTestRule.onNodeWithTag(BackButtonTestTag.BACK_BUTTON).performClick()
    assert(isClicked)
  }
}
