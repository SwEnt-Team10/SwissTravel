package com.github.swent.swisstravel.ui.tripcreation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoadingScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun loadingScreen_displaysCorrectly() {
    composeTestRule.setContent { LoadingScreen() }

    composeTestRule.onNodeWithTag(LoadingTestTags.LOADING).assertIsDisplayed()
    composeTestRule.onNodeWithTag(LoadingTestTags.LOADING_TEXT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(LoadingTestTags.PROGRESS_BAR).assertIsDisplayed()
  }
}
