package com.github.swent.swisstravel.ui.profile

import android.net.Uri
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.github.swent.swisstravel.model.trip.TripRepositoryLocal
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.model.user.User
import com.github.swent.swisstravel.model.user.UserRepository
import com.github.swent.swisstravel.model.user.UserStats
import com.github.swent.swisstravel.ui.composable.PreferenceSelectorTestTags
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
        preferences = listOf(Preference.MUSEUMS, Preference.QUICK),
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

val emptyUserRepo =
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

class ProfileSettingsScreenTest {

  @get:Rule val composeTestRule = createComposeRule()
  private val fakeUserRepo = FakeUserRepository()
  private val fakeTripRepo = TripRepositoryLocal()

  private fun setContentHelper(fakeUserRepository: UserRepository = fakeUserRepo) {
    composeTestRule.setContent {
      SwissTravelTheme {
        ProfileSettingsScreen(ProfileSettingsViewModel(fakeUserRepository, fakeTripRepo))
      }
    }
  }

  @Test
  fun allKeyUIElementsAreDisplayed_collapsedByDefault() {
    setContentHelper()
    // Static bits
    composeTestRule.onNodeWithTag(ProfileSettingsScreenTestTags.PROFILE_PIC).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileSettingsScreenTestTags.PROFILE_INFO).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileSettingsScreenTestTags.PERSONAL_INFO).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileSettingsScreenTestTags.EMAIL).assertIsDisplayed()
    val fields = listOf("NAME", "BIOGRAPHY")
    fields.forEach { prefix ->
      composeTestRule.onNodeWithTag(ProfileSettingsScreenTestTags.label(prefix)).assertIsDisplayed()
      composeTestRule.onNodeWithTag(ProfileSettingsScreenTestTags.text(prefix)).assertIsDisplayed()
      composeTestRule
          .onNodeWithTag(ProfileSettingsScreenTestTags.editButton(prefix), useUnmergedTree = true)
          .assertIsDisplayed()
    }
    // Logout button present
    composeTestRule
        .onNodeWithTag(ProfileSettingsScreenTestTags.LOGOUT_BUTTON)
        .performScrollTo()
        .assertIsDisplayed()

    // Preferences container present
    composeTestRule
        .onNodeWithTag(ProfileSettingsScreenTestTags.PREFERENCES_LIST)
        .assertIsDisplayed()
    // Header row present
    composeTestRule.onNodeWithTag(ProfileSettingsScreenTestTags.PREFERENCES).assertIsDisplayed()
    // Collapsed by default: a well-known preference label should NOT be visible yet
    composeTestRule
        .onNodeWithTag(PreferenceSelectorTestTags.getTestTagButton(Preference.MUSEUMS))
        .assertDoesNotExist()
  }

  @Test
  fun expandAndCollapsePreferences_showsAndHidesContent() {
    setContentHelper()

    // Expand
    composeTestRule
        .onNodeWithTag(
            useUnmergedTree = true, testTag = ProfileSettingsScreenTestTags.PREFERENCES_TOGGLE)
        .performClick()
    composeTestRule.onNodeWithTag(ProfileSettingsScreenTestTags.LOGOUT_BUTTON).performScrollTo()
    // Now a known preference chip should appear
    composeTestRule
        .onNodeWithTag(PreferenceSelectorTestTags.getTestTagButton(Preference.QUICK))
        .assertIsDisplayed()

    composeTestRule.onNodeWithTag(ProfileSettingsScreenTestTags.PERSONAL_INFO).performScrollTo()
    // Collapse
    composeTestRule
        .onNodeWithTag(
            useUnmergedTree = true, testTag = ProfileSettingsScreenTestTags.PREFERENCES_TOGGLE)
        .performClick()
    composeTestRule
        .onNodeWithTag(PreferenceSelectorTestTags.getTestTagButton(Preference.QUICK))
        .assertDoesNotExist()
  }

  @Test
  fun clickingAPreferenceChip_invokesSaveFlow() {
    setContentHelper()

    // Expand first
    composeTestRule
        .onNodeWithTag(
            useUnmergedTree = true, testTag = ProfileSettingsScreenTestTags.PREFERENCES_TOGGLE)
        .performClick()
    composeTestRule.onNodeWithTag(ProfileSettingsScreenTestTags.LOGOUT_BUTTON).performScrollTo()
    composeTestRule
        .onNodeWithTag(PreferenceSelectorTestTags.getTestTagButton(Preference.QUICK))
        .assertIsDisplayed()

    // Click to toggle on/off (we don't assert state, just ensure it doesn't crash)
    composeTestRule
        .onNodeWithTag(PreferenceSelectorTestTags.getTestTagButton(Preference.QUICK))
        .performClick()
    composeTestRule
        .onNodeWithTag(PreferenceSelectorTestTags.getTestTagButton(Preference.QUICK))
        .performClick()
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
    setContentHelper(emptyUserRepo)

    // InfoItem displays "-" when value is blank
    composeTestRule
        .onNode(hasTestTag(ProfileSettingsScreenTestTags.EMAIL), useUnmergedTree = true)
        .assertIsDisplayed()
    // We can also check the literal "-" exists at least once on the screen
    composeTestRule.onNodeWithText("-", substring = false).assertIsDisplayed()
  }

  @Test
  fun clickingEdit_showsTextField_andConfirmCancelButtons() {
    setContentHelper()

    // Before editing: text is shown, not TextField
    composeTestRule.onNodeWithTag(ProfileSettingsScreenTestTags.text("NAME")).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(ProfileSettingsScreenTestTags.textField("NAME"))
        .assertDoesNotExist()

    // Enter edit mode
    composeTestRule.onNodeWithTag(ProfileSettingsScreenTestTags.editButton("NAME")).performClick()

    // Now TextField + Confirm + Cancel buttons appear
    composeTestRule
        .onNodeWithTag(ProfileSettingsScreenTestTags.textField("NAME"))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(ProfileSettingsScreenTestTags.confirmButton("NAME"))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(ProfileSettingsScreenTestTags.cancelButton("NAME"))
        .assertIsDisplayed()
  }

  @Test
  fun editingName_andSaving_updatesTextDisplayed() {
    setContentHelper()

    // Enter edit mode
    composeTestRule.onNodeWithTag(ProfileSettingsScreenTestTags.editButton("NAME")).performClick()

    // Clear any text
    composeTestRule
        .onNodeWithTag(ProfileSettingsScreenTestTags.textField("NAME"))
        .performTextClearance()

    // Update text
    composeTestRule
        .onNodeWithTag(ProfileSettingsScreenTestTags.textField("NAME"))
        .performTextInput("New Name")

    // Save
    composeTestRule
        .onNodeWithTag(ProfileSettingsScreenTestTags.confirmButton("NAME"))
        .performClick()

    // TextField is gone and the updated text is shown
    composeTestRule
        .onNodeWithTag(ProfileSettingsScreenTestTags.textField("NAME"))
        .assertDoesNotExist()
    composeTestRule.onNodeWithText("New Name").assertIsDisplayed()
  }

  @Test
  fun editingName_andCancel_restoresOriginalValue() {
    setContentHelper()

    // Original value from FakeUserRepository is "Test User"
    composeTestRule.onNodeWithText("Test User").assertIsDisplayed()

    // Enter edit mode
    composeTestRule.onNodeWithTag(ProfileSettingsScreenTestTags.editButton("NAME")).performClick()

    // Clear any text
    composeTestRule
        .onNodeWithTag(ProfileSettingsScreenTestTags.textField("NAME"))
        .performTextClearance()

    // Change text
    composeTestRule
        .onNodeWithTag(ProfileSettingsScreenTestTags.textField("NAME"))
        .performTextInput("Should Not Save")

    // Cancel
    composeTestRule.onNodeWithTag(ProfileSettingsScreenTestTags.cancelButton("NAME")).performClick()

    // Should show original text again
    composeTestRule.onNodeWithText("Test User").assertIsDisplayed()
    composeTestRule.onNodeWithText("Should Not Save").assertDoesNotExist()
  }

  @Test
  fun emptyBiography_showsPressEditToAdd() {
    setContentHelper(emptyUserRepo)

    // Empty bio shows fallback placeholder
    composeTestRule
        .onNodeWithTag(ProfileSettingsScreenTestTags.empty("BIOGRAPHY"))
        .assertIsDisplayed()
  }

  @Test
  fun emptyField_editMode_showsTextField() {
    setContentHelper(emptyUserRepo)

    // Press edit
    composeTestRule
        .onNodeWithTag(ProfileSettingsScreenTestTags.editButton("BIOGRAPHY"))
        .performClick()

    // Should show TextField
    composeTestRule
        .onNodeWithTag(ProfileSettingsScreenTestTags.textField("BIOGRAPHY"))
        .assertIsDisplayed()
  }
}
