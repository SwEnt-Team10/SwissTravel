package com.android.swisstravel.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.swent.swisstravel.ui.ProfileScreen
import com.github.swent.swisstravel.ui.ProfileScreenTestTags
import org.junit.Rule
import org.junit.Test

class ProfileScreenUITest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun allKeyUIElementsAreDisplayed() {
    composeTestRule.setContent { ProfileScreen() }

    composeTestRule.onNodeWithTag(ProfileScreenTestTags.PROFILE_PIC).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.GREETING).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.DROPDOWN_PREFERENCES).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.DISPLAY_NAME).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.EMAIL).assertIsDisplayed()
  }

  @Test
  fun dropdown_opensWhenClicked() {
    composeTestRule.setContent { ProfileScreen() }

    composeTestRule.onNodeWithTag(ProfileScreenTestTags.DROPDOWN_PREFERENCES).performClick()

    composeTestRule.onAllNodesWithText("Hiking & Outdoor").fetchSemanticsNodes().let {
      assert(it.isNotEmpty())
    }
  }

  @Test
  fun dropdown_closesWhenClickedAgain() {
    composeTestRule.setContent { ProfileScreen() }

    val dropdown = composeTestRule.onNodeWithTag(ProfileScreenTestTags.DROPDOWN_PREFERENCES)

    dropdown.performClick()
    composeTestRule.waitForIdle()
    dropdown.performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onAllNodesWithText("Museums").assertCountEquals(0)
  }

  @Test
  fun profileDisplaysFallbackValuesWhenEmpty() {
    composeTestRule.setContent { ProfileScreen() }

    composeTestRule
        .onNode(
            hasTestTag(ProfileScreenTestTags.DISPLAY_NAME) and hasText("-"), useUnmergedTree = true)
        .assertExists()

    composeTestRule
        .onNode(hasTestTag(ProfileScreenTestTags.EMAIL) and hasText("-"), useUnmergedTree = true)
        .assertExists()
  }
}
