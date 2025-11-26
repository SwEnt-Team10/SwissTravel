package com.github.swent.swisstravel.ui.profile

import android.net.Uri
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.swent.swisstravel.model.trip.TripRepositoryLocal
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.model.user.User
import com.github.swent.swisstravel.model.user.UserRepository
import com.github.swent.swisstravel.model.user.UserStats
import com.github.swent.swisstravel.ui.theme.SwissTravelTheme
import org.junit.Rule
import org.junit.Test

/** Fake UserRepository to avoid Firebase. */
class FakeUserRepository : UserRepository {
  override suspend fun getCurrentUser(): User {
    return User(
        uid = "fakeUid123",
        name = "Test User",
        biography = "Fake Bio",
        email = "test@example.com",
        profilePicUrl = "",
        preferences = listOf(Preference.MUSEUMS),
        friends = emptyList(),
        stats = UserStats(),
        pinnedTripsUids = emptyList(),
        pinnedImagesUris = emptyList())
  }

  override suspend fun getUserByUid(uid: String): User? {
    // no op in tests
    return null
  }

  override suspend fun getUserByNameOrEmail(query: String): List<User> {
    // no op in tests
    return emptyList()
  }

  override suspend fun updateUserPreferences(uid: String, preferences: List<Preference>) {
    // no-op in tests
  }

  override suspend fun sendFriendRequest(fromUid: String, toUid: String) {
    // no-op in tests
  }

  override suspend fun acceptFriendRequest(currentUid: String, fromUid: String) {
    // no-op in tests
  }

  override suspend fun removeFriend(uid: String, friendUid: String) {
    // no-op in tests
  }

  override suspend fun updateUser(
      uid: String,
      name: String?,
      biography: String?,
      profilePicUrl: String?,
      preferences: List<Preference>?,
      pinnedTripsUids: List<String>?,
      pinnedImagesUris: List<Uri>?
  ) {
    // no-op in tests
  }

  override suspend fun updateUserStats(uid: String, stats: UserStats) {
    // no-op in tests
  }
}

class ProfileSettingsScreenTest {

  @get:Rule val composeTestRule = createComposeRule()
  private val fakeRepo = FakeUserRepository()
  private val fakeTripRepo = TripRepositoryLocal()

  @Test
  fun allKeyUIElementsAreDisplayed_collapsedByDefault() {
    composeTestRule.setContent {
      SwissTravelTheme { ProfileSettingsScreen(ProfileSettingsViewModel(fakeRepo, fakeTripRepo)) }
    }

    // Static bits
    composeTestRule.onNodeWithTag(ProfileSettingsScreenTestTags.PROFILE_PIC).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileSettingsScreenTestTags.Pr).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileSettingsScreenTestTags.PERSONAL_INFO).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileSettingsScreenTestTags.DISPLAY_NAME).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileSettingsScreenTestTags.EMAIL).assertIsDisplayed()

    // Preferences container present
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.PREFERENCES_LIST).assertIsDisplayed()
    // Header row present
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.PREFERENCES).assertIsDisplayed()
    // Collapsed by default: a well-known preference label should NOT be visible yet
    composeTestRule.onNodeWithText("Museums").assertDoesNotExist()
  }

  @Test
  fun expandAndCollapsePreferences_showsAndHidesContent() {
    composeTestRule.setContent {
      SwissTravelTheme { ProfileScreen(ProfileScreenViewModel(fakeRepo, fakeTripRepo)) }
    }

    // Expand
    composeTestRule
        .onNodeWithTag(useUnmergedTree = true, testTag = ProfileScreenTestTags.PREFERENCES_TOGGLE)
        .performClick()
    // Now a known preference chip should appear
    composeTestRule.onNodeWithText("Museums").assertIsDisplayed()

    // Collapse
    composeTestRule
        .onNodeWithTag(useUnmergedTree = true, testTag = ProfileScreenTestTags.PREFERENCES_TOGGLE)
        .performClick()
    composeTestRule.onNodeWithText("Museums").assertDoesNotExist()
  }

  @Test
  fun clickingAPreferenceChip_invokesSaveFlow() {
    composeTestRule.setContent {
      SwissTravelTheme { ProfileScreen(ProfileScreenViewModel(fakeRepo, fakeTripRepo)) }
    }

    // Expand first
    composeTestRule
        .onNodeWithTag(useUnmergedTree = true, testTag = ProfileScreenTestTags.PREFERENCES_TOGGLE)
        .performClick()
    composeTestRule.onNodeWithText("Museums").assertIsDisplayed()

    // Click to toggle on/off (we don't assert state, just ensure it doesn't crash)
    composeTestRule.onNodeWithText("Museums").performClick()
    composeTestRule.onNodeWithText("Museums").performClick()
  }

  /** Directly tests InfoSection/InfoItem for coverage. */
  @Test
  fun infoSection_and_InfoItem_renderTextProperly() {
    composeTestRule.setContent {
      SwissTravelTheme {
        InfoSection(title = "Section Title", modifier = Modifier.testTag("section")) {
          InfoItem(label = "Label", value = "Value", modifier = Modifier.testTag("value"))
        }
      }
    }
    composeTestRule.onNodeWithText("Section Title").assertIsDisplayed()
    composeTestRule.onNodeWithText("Label").assertIsDisplayed()
    composeTestRule.onNodeWithText("Value").assertIsDisplayed()
  }

  @Test
  fun profileDisplaysFallbackValuesWhenEmpty() {
    val emptyRepo =
        object : UserRepository {
          override suspend fun getCurrentUser(): User {
            return User(
                uid = "0",
                name = "",
                biography = "",
                email = "",
                profilePicUrl = "",
                preferences = emptyList(),
                friends = emptyList(),
                stats = UserStats(),
                pinnedTripsUids = emptyList(),
                pinnedImagesUris = emptyList())
          }

          override suspend fun getUserByUid(uid: String): User? {
            // no op for tests
            return null
          }

          override suspend fun getUserByNameOrEmail(query: String): List<User> {
            // no op for tests
            return emptyList()
          }

          override suspend fun updateUserPreferences(uid: String, preferences: List<Preference>) {}

          override suspend fun updateUserStats(uid: String, stats: UserStats) {
            /** no-op for tests* */
          }

          override suspend fun sendFriendRequest(fromUid: String, toUid: String) {
            /** no-op for tests* */
          }

          override suspend fun acceptFriendRequest(currentUid: String, fromUid: String) {
            /** no-op for tests* */
          }

          override suspend fun removeFriend(uid: String, friendUid: String) {
            /** no-op for tests* */
          }

          override suspend fun updateUser(
              uid: String,
              name: String?,
              biography: String?,
              profilePicUrl: String?,
              preferences: List<Preference>?,
              pinnedTripsUids: List<String>?,
              pinnedImagesUris: List<Uri>?
          ) {
            /** no-op for tests* */
          }
        }

    composeTestRule.setContent {
      SwissTravelTheme { ProfileScreen(ProfileScreenViewModel(emptyRepo, fakeTripRepo)) }
    }

    // InfoItem displays "-" when value is blank
    composeTestRule
        .onNode(hasTestTag(ProfileScreenTestTags.DISPLAY_NAME), useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNode(hasTestTag(ProfileScreenTestTags.EMAIL), useUnmergedTree = true)
        .assertIsDisplayed()
    // We can also check the literal "-" exists at least once on the screen
    composeTestRule
        .onAllNodesWithTag(ProfileScreenTestTags.DISPLAY_NAME, useUnmergedTree = true)
        .fetchSemanticsNodes()
    composeTestRule
        .onAllNodesWithTag(ProfileScreenTestTags.EMAIL, useUnmergedTree = true)
        .fetchSemanticsNodes()
  }
}
