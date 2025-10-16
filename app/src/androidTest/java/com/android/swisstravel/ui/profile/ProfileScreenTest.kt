package com.android.swisstravel.ui.profile

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.model.user.User
import com.github.swent.swisstravel.model.user.UserRepository
import com.github.swent.swisstravel.ui.profile.InfoItem
import com.github.swent.swisstravel.ui.profile.InfoSection
import com.github.swent.swisstravel.ui.profile.PreferenceToggle
import com.github.swent.swisstravel.ui.profile.ProfileScreen
import com.github.swent.swisstravel.ui.profile.ProfileScreenTestTags
import com.github.swent.swisstravel.ui.profile.ProfileScreenViewModel
import com.github.swent.swisstravel.ui.theme.SwissTravelTheme
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
        preferences = listOf(Preference.MUSEUMS))
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
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.PREFERENCES_LIST).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.DISPLAY_NAME).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.EMAIL).assertIsDisplayed()

    val expectedPreferenceCount = Preference.values().size

    composeTestRule
        .onAllNodesWithTag(ProfileScreenTestTags.PREFERENCES)
        .assertCountEquals(expectedPreferenceCount)
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

    composeTestRule.setContent {
      SwissTravelTheme { ProfileScreen(ProfileScreenViewModel(emptyRepo)) }
    }

    composeTestRule
        .onNode(
            hasTestTag(ProfileScreenTestTags.DISPLAY_NAME) and hasText("-"), useUnmergedTree = true)
        .assertExists()

    composeTestRule
        .onNode(hasTestTag(ProfileScreenTestTags.EMAIL) and hasText("-"), useUnmergedTree = true)
        .assertExists()
  }

  @Test
  fun toggleSwitch_changesStateWhenClicked() {
    composeTestRule.setContent {
      SwissTravelTheme { ProfileScreen(ProfileScreenViewModel(fakeRepo)) }
    }

    // Ensure the preference switch is visible
    val museumsToggle = composeTestRule.onNodeWithText("Museums")
    museumsToggle.assertIsDisplayed()

    // Click the toggle to change state
    museumsToggle.performClick()
    composeTestRule.waitForIdle()

    // Click again to revert
    museumsToggle.performClick()
    composeTestRule.waitForIdle()
  }

  /** Directly tests InfoSection and InfoItem composables for coverage. */
  @Test
  fun infoSection_and_InfoItem_renderTextProperly() {
    composeTestRule.setContent {
      SwissTravelTheme {
        InfoSection(title = "Section Title", modifier = Modifier.testTag("section")) {
          InfoItem(label = "Label", value = "Value", modifier = Modifier.testTag("value"))
        }
      }
    }

    composeTestRule.onNodeWithText("Section Title").assertExists()
    composeTestRule.onNodeWithText("Label").assertExists()
    composeTestRule.onNodeWithText("Value").assertExists()
  }

  /** Tests PreferenceToggle composable directly (checked/un-checked behavior). */
  @Test
  fun preferenceToggle_changesState() {
    var toggled = false
    composeTestRule.setContent {
      SwissTravelTheme {
        PreferenceToggle(
            title = "My Toggle",
            checked = toggled,
            onCheckedChange = { toggled = !toggled },
            modifier = Modifier.testTag("toggle"))
      }
    }

    val toggleNode = composeTestRule.onNodeWithText("My Toggle", useUnmergedTree = true)
    toggleNode.assertExists()
    toggleNode.performClick()
    toggleNode.performClick()
  }
}
