package com.android.swisstravel.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.swent.swisstravel.model.user.User
import com.github.swent.swisstravel.model.user.UserPreference
import com.github.swent.swisstravel.model.user.UserRepository
import com.github.swent.swisstravel.ui.MultiSelectDropdown
import com.github.swent.swisstravel.ui.ProfileScreen
import com.github.swent.swisstravel.ui.ProfileScreenTestTags
import com.github.swent.swisstravel.ui.ProfileScreenViewModel
import org.junit.Rule
import org.junit.Test

/** Fake UserRepository to avoid hitting Firebase or triggering Toasts. */
class FakeUserRepository : UserRepository {
  override suspend fun getCurrentUser(): User {
    return User(
        uid = "fakeUid123",
        name = "Test User",
        email = "test@example.com",
        profilePicUrl = "",
        preferences = listOf(UserPreference.MUSEUMS))
  }

  override suspend fun updateUserPreferences(uid: String, preferences: List<String>) {
    // No-op for tests — we just simulate the call.
  }
}

class ProfileScreenUITest {

  @get:Rule val composeTestRule = createComposeRule()

  private val fakeRepo = FakeUserRepository()

  @Test
  fun allKeyUIElementsAreDisplayed() {
    composeTestRule.setContent { ProfileScreen(ProfileScreenViewModel(fakeRepo)) }

    composeTestRule.onNodeWithTag(ProfileScreenTestTags.PROFILE_PIC).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.GREETING).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.DROPDOWN_PREFERENCES).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.DISPLAY_NAME).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.EMAIL).assertIsDisplayed()
  }

  @Test
  fun dropdown_opensWhenClicked() {
    composeTestRule.setContent { ProfileScreen(ProfileScreenViewModel(FakeUserRepository())) }

    composeTestRule.onNodeWithTag(ProfileScreenTestTags.DROPDOWN_PREFERENCES).performClick()

    composeTestRule.onAllNodesWithText("Hiking & Outdoor").fetchSemanticsNodes().let {
      assert(it.isNotEmpty())
    }
  }

  @Test
  fun dropdown_closesWhenClickedAgain() {
    composeTestRule.setContent { ProfileScreen(ProfileScreenViewModel(fakeRepo)) }

    val dropdown = composeTestRule.onNodeWithTag(ProfileScreenTestTags.DROPDOWN_PREFERENCES)

    dropdown.performClick()
    composeTestRule.waitForIdle()
    dropdown.performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onAllNodesWithText("Museums").assertCountEquals(0)
  }

  @Test
  fun profileDisplaysFallbackValuesWhenEmpty() {
    // Create a ViewModel with no user data (simulates logged-out or empty user)
    val emptyRepo =
        object : UserRepository {
          override suspend fun getCurrentUser(): User {
            return User(
                uid = "0", name = "", email = "", profilePicUrl = "", preferences = emptyList())
          }

          override suspend fun updateUserPreferences(uid: String, preferences: List<String>) {
            // No-op for tests — we just simulate the call.
          }
        }

    composeTestRule.setContent { ProfileScreen(ProfileScreenViewModel(emptyRepo)) }

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
            onSelectionChanged = { selected.value = it })
        // Helper text to assert on
        Text("Selected: " + selected.value.joinToString())
      }
    }

    // Open the dropdown
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.DROPDOWN_PREFERENCES).performClick()

    // Click an unselected item -> covers the "else" branch (add)
    composeTestRule.onNodeWithText("Hiking & Outdoor", useUnmergedTree = true).performClick()
    composeTestRule.onNodeWithText("Selected: Museums, Hiking & Outdoor").assertExists()

    // Open again and click a selected item -> covers the "if" branch (remove)
    composeTestRule.onNodeWithText("Museums", useUnmergedTree = true).performClick()
    composeTestRule.onNodeWithText("Selected: Hiking & Outdoor").assertExists()
  }
}
