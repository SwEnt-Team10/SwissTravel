package com.github.swent.swisstravel.ui.profile

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.github.swent.swisstravel.model.image.ImageRepositoryFirebase
import com.github.swent.swisstravel.model.trip.TripsRepositoryFirestore
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.model.user.User
import com.github.swent.swisstravel.model.user.UserRepositoryFirebase
import com.github.swent.swisstravel.model.user.UserStats
import com.github.swent.swisstravel.ui.composable.PreferenceSelectorTestTags
import com.github.swent.swisstravel.utils.FirebaseEmulator
import com.github.swent.swisstravel.utils.FirestoreSwissTravelTest
import com.github.swent.swisstravel.utils.UI_WAIT_TIMEOUT
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Tests for the Profile Settings Screen. Made with the help of AI. */
class ProfileSettingsScreenTest : FirestoreSwissTravelTest() {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var userRepo: UserRepositoryFirebase
  private lateinit var tripsRepo: TripsRepositoryFirestore
  private lateinit var imageRepo: ImageRepositoryFirebase

  private val testUser =
      User(
          uid = "",
          name = "Test User",
          biography = "Fake Bio",
          email = "test@example.com",
          profilePicUrl = "",
          preferences = listOf(Preference.MUSEUMS, Preference.QUICK),
          friends = emptyList(),
          stats = UserStats(),
          pinnedTripsUids = emptyList(),
          pinnedPicturesUids = emptyList(),
          favoriteTripsUids = emptyList())

  @Before
  override fun setUp() {
    super.setUp()

    userRepo = UserRepositoryFirebase(FirebaseEmulator.auth, FirebaseEmulator.firestore)
    tripsRepo = TripsRepositoryFirestore(FirebaseEmulator.firestore, FirebaseEmulator.auth)
    imageRepo = ImageRepositoryFirebase(FirebaseEmulator.firestore, FirebaseEmulator.auth)

    runBlocking {
      FirebaseEmulator.auth.signOut()

      // CHANGED: Use Anonymous Sign In
      FirebaseEmulator.auth.signInAnonymously().await()

      // Write the User document to Firestore Emulator using the new anonymous UID
      val uid = FirebaseEmulator.auth.currentUser!!.uid
      val userWithUid = testUser.copy(uid = uid)
      FirebaseEmulator.firestore.collection("users").document(uid).set(userWithUid).await()
    }
  }

  private fun setContentHelper(): ProfileSettingsViewModel {
    val vm = ProfileSettingsViewModel(userRepo, tripsRepo, imageRepo)
    composeTestRule.setContent { ProfileSettingsScreen(vm) }
    waitForLoading()
    return vm
  }

  private fun setupEmptyUser() {
    runBlocking {
      val uid = FirebaseEmulator.auth.currentUser!!.uid
      val emptyUser =
          testUser.copy(uid = uid, name = "", biography = "", email = "", preferences = emptyList())
      FirebaseEmulator.firestore.collection("users").document(uid).set(emptyUser).await()
    }
  }

  private fun waitForLoading() {
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(ProfileSettingsScreenTestTags.CONTENT)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
  }

  private fun createTestImageUri(): android.net.Uri {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val file = java.io.File(context.cacheDir, "test_profile_image.jpg")
    java.io.FileOutputStream(file).use { out ->
      android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888)
          .compress(android.graphics.Bitmap.CompressFormat.JPEG, 50, out)
    }
    return android.net.Uri.fromFile(file)
  }

  @Test
  fun allKeyUIElementsAreDisplayed_collapsedByDefault() {
    setContentHelper()
    // Static bits
    composeTestRule
        .onNodeWithTag(ProfileSettingsScreenTestTags.EDIT_PROFILE_PIC)
        .assertIsDisplayed()
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
      InfoSection(title = "Section Title", modifier = Modifier.testTag("section")) {
        InfoItem(label = "Label", value = "Value", modifier = Modifier.testTag("value"))
      }
    }
    composeTestRule.onNodeWithText("Section Title").assertIsDisplayed()
    composeTestRule.onNodeWithText("Label").assertIsDisplayed()
    composeTestRule.onNodeWithText("Value").assertIsDisplayed()
  }

  @Test
  fun profileDisplaysFallbackValuesWhenEmpty() {
    setupEmptyUser()
    setContentHelper()

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

    // Wait until the TextField is gone
    composeTestRule.waitUntil(timeoutMillis = UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(ProfileSettingsScreenTestTags.textField("NAME"))
          .fetchSemanticsNodes()
          .isEmpty()
    }

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
    setupEmptyUser()
    setContentHelper()

    // Empty bio shows fallback placeholder
    composeTestRule
        .onNodeWithTag(ProfileSettingsScreenTestTags.empty("BIOGRAPHY"))
        .assertIsDisplayed()
  }

  @Test
  fun emptyField_editMode_showsTextField() {
    setupEmptyUser()
    setContentHelper()

    // Press edit
    composeTestRule
        .onNodeWithTag(ProfileSettingsScreenTestTags.editButton("BIOGRAPHY"))
        .performClick()

    // Should show TextField
    composeTestRule
        .onNodeWithTag(ProfileSettingsScreenTestTags.textField("BIOGRAPHY"))
        .assertIsDisplayed()
  }

  @Test
  fun editProfilePic_showsPreviewDialog() {
    val vm = setContentHelper()
    val uri = createTestImageUri()

    // 1. Verify edit button exists
    composeTestRule
        .onNodeWithTag(ProfileSettingsScreenTestTags.EDIT_PROFILE_PIC)
        .assertIsDisplayed()

    // 2. Simulate selection
    composeTestRule.runOnUiThread { vm.onProfilePicSelected(uri) }

    // 3. Verify Dialog Elements using your new tags
    composeTestRule
        .onNodeWithTag(ProfileSettingsScreenTestTags.PREVIEW_PFP_TEXT)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(ProfileSettingsScreenTestTags.PREVIEW_PFP_IMAGE)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(ProfileSettingsScreenTestTags.PREVIEW_PFP_CONFIRM)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(ProfileSettingsScreenTestTags.PREVIEW_PFP_CANCEL)
        .assertIsDisplayed()
  }

  @Test
  fun confirmProfilePic_updatesUserProfile() {
    val vm = setContentHelper()
    val uri = createTestImageUri()

    // 1. Simulate selection
    composeTestRule.runOnUiThread { vm.onProfilePicSelected(uri) }

    // 2. Click Confirm
    composeTestRule.onNodeWithTag(ProfileSettingsScreenTestTags.PREVIEW_PFP_CONFIRM).performClick()

    // 3. Wait for uploading to finish (Wait until loading is false AND URI is null)
    composeTestRule.waitUntil(timeoutMillis = UI_WAIT_TIMEOUT) {
      !vm.uiState.value.isLoading && vm.uiState.value.pendingProfilePicUri == null
    }

    // 4. Verify Dialog is gone
    composeTestRule
        .onNodeWithTag(ProfileSettingsScreenTestTags.PREVIEW_PFP_CONFIRM)
        .assertDoesNotExist()

    // 5. Verify User was updated in Firestore
    val updatedUser = runBlocking { userRepo.getCurrentUser() }
    assert(updatedUser.profilePicUrl.isNotBlank()) { "Profile picture URL should not be blank" }

    // Optional: Verify image exists in ImageRepository
    val savedImage = runBlocking { imageRepo.getImage(updatedUser.profilePicUrl) }
    assert(savedImage.uid == updatedUser.profilePicUrl)
    assert(savedImage.ownerId == updatedUser.uid)
  }

  @Test
  fun cancelProfilePic_dismissesDialog() {
    val vm = setContentHelper()
    val uri = createTestImageUri()

    // 1. Simulate selection
    composeTestRule.runOnUiThread { vm.onProfilePicSelected(uri) }

    // 2. Click Cancel
    composeTestRule.onNodeWithTag(ProfileSettingsScreenTestTags.PREVIEW_PFP_CANCEL).performClick()

    // 3. Verify Dialog is gone
    composeTestRule
        .onNodeWithTag(ProfileSettingsScreenTestTags.PREVIEW_PFP_CANCEL)
        .assertDoesNotExist()

    // 4. Verify URL has not changed
    val currentUser = runBlocking { userRepo.getCurrentUser() }
    assert(currentUser.profilePicUrl == "")
  }

  @Test
  fun editingBiography_andSaving_updatesTextDisplayed() {
    setContentHelper()

    // 1. Enter edit mode for Biography
    composeTestRule
        .onNodeWithTag(ProfileSettingsScreenTestTags.editButton("BIOGRAPHY"))
        .performClick()

    // 2. Clear and Enter new text
    val newBio = "I love traveling in Switzerland!"
    composeTestRule
        .onNodeWithTag(ProfileSettingsScreenTestTags.textField("BIOGRAPHY"))
        .performTextClearance()
    composeTestRule
        .onNodeWithTag(ProfileSettingsScreenTestTags.textField("BIOGRAPHY"))
        .performTextInput(newBio)

    // 3. Save
    composeTestRule
        .onNodeWithTag(ProfileSettingsScreenTestTags.confirmButton("BIOGRAPHY"))
        .performClick()

    // 4. Wait for the TextField to disappear (indicating edit mode ended)
    composeTestRule.waitUntil(timeoutMillis = UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(ProfileSettingsScreenTestTags.textField("BIOGRAPHY"))
          .fetchSemanticsNodes()
          .isEmpty()
    }

    // 5. Verify the new text is displayed
    composeTestRule.onNodeWithText(newBio).assertIsDisplayed()
  }

  @Test
  fun backButton_invokesCallback() {
    var backPressed = false
    val vm = ProfileSettingsViewModel(userRepo, tripsRepo, imageRepo)

    // Set content with a custom onBack callback
    composeTestRule.setContent {
      ProfileSettingsScreen(profileSettingsViewModel = vm, onBack = { backPressed = true })
    }
    waitForLoading()

    // Click the back button in the top bar
    composeTestRule
        .onNodeWithTag(com.github.swent.swisstravel.ui.navigation.NavigationTestTags.TOP_BAR_BUTTON)
        .performClick()

    // Verify callback was invoked
    assert(backPressed) { "Back button callback should have been invoked" }
  }

  @Test
  fun logout_signsUserOut() {
    setContentHelper()

    // Pre-check: User should be signed in from setUp()
    assert(FirebaseEmulator.auth.currentUser != null) { "User should be signed in initially" }

    // Scroll to and click Logout
    composeTestRule
        .onNodeWithTag(ProfileSettingsScreenTestTags.LOGOUT_BUTTON)
        .performScrollTo()
        .performClick()

    // Verify user is now signed out in the emulator
    assert(FirebaseEmulator.auth.currentUser == null) {
      "User should be signed out after clicking logout"
    }
  }
}
