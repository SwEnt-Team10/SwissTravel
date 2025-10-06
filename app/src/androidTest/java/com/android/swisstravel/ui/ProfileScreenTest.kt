package com.android.swisstravel.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.swent.swisstravel.ui.MultiSelectDropdown
import com.github.swent.swisstravel.ui.ProfileScreen
import com.github.swent.swisstravel.ui.ProfileScreenTestTags
import org.hamcrest.MatcherAssert.assertThat
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

  @Test
  fun multiSelectDropdown_togglesSelection_addsAndRemoves() {
    composeTestRule.setContent {
      // Start with "Museums" selected so we can hit both branches
      var selected = remember { mutableStateOf(listOf("Museums")) }
      Column {
        MultiSelectDropdown(
          allPreferences = listOf("Hiking & Outdoor", "Museums"),
          selectedPreferences = selected.value,
          onSelectionChanged = { selected.value = it }
        )
        // Helper text to assert on
        Text("Selected: " + selected.value.joinToString())
      }
    }

    // Open the dropdown
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.DROPDOWN_PREFERENCES).performClick()

    // Click an unselected item -> covers the "else" branch (add)
    composeTestRule.onNodeWithText("Hiking & Outdoor", useUnmergedTree = true).performClick()
    composeTestRule.onNodeWithText("Selected: Museums, Hiking & Outdoor").assertExists()

    // Open again
    // Click a selected item -> covers the "if" branch (remove)
    composeTestRule.onNodeWithText("Museums", useUnmergedTree = true).performClick()
    composeTestRule.onNodeWithText("Selected: Hiking & Outdoor").assertExists()
  }


}
