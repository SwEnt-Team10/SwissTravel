package com.github.swent.swisstravel.ui.navigation

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.swent.swisstravel.ui.theme.SwissTravelTheme
import org.junit.Rule
import org.junit.Test

class TopBarTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun topBarIsDisplayedCorrectly() {
    val titleText = "My Screen Title"

    composeTestRule.setContent { SwissTravelTheme { TopBar(onClick = {}, title = titleText) } }

    // Displays the top bar
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR).assertExists()
    // Displays the title
    composeTestRule.onNodeWithTag(TopBarTestTags.getTestTagTitle(titleText)).assertExists()
    composeTestRule.onNodeWithText(titleText).assertIsDisplayed()
    // Displays the button
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_BUTTON).assertHasClickAction()
  }

  @Test
  fun topBarBackButtonWorks() {
    var clicked = false
    composeTestRule.setContent {
      SwissTravelTheme { TopBar(onClick = { clicked = true }, title = "Test Title") }
    }
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_BUTTON).performClick()
    assert(clicked)
  }
}
