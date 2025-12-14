package com.github.swent.swisstravel.ui.profile.selectpinnedpictures

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.swent.swisstravel.model.image.Image
import com.github.swent.swisstravel.model.image.ImageRepositoryFirebase
import com.github.swent.swisstravel.model.user.User
import com.github.swent.swisstravel.model.user.UserRepositoryFirebase
import com.github.swent.swisstravel.ui.composable.DeleteDialogTestTags
import com.github.swent.swisstravel.ui.composable.PhotoGridTestTags
import com.github.swent.swisstravel.ui.profile.selectpinnedpictures.SelectPinnedPicturesScreenTestTags.ADD_PICTURE_BUTTON
import com.github.swent.swisstravel.ui.profile.selectpinnedpictures.SelectPinnedPicturesScreenTestTags.CANCEL_BUTTON
import com.github.swent.swisstravel.ui.profile.selectpinnedpictures.SelectPinnedPicturesScreenTestTags.EDIT_BUTTON
import com.github.swent.swisstravel.ui.profile.selectpinnedpictures.SelectPinnedPicturesScreenTestTags.LOADING_INDICATOR
import com.github.swent.swisstravel.ui.profile.selectpinnedpictures.SelectPinnedPicturesScreenTestTags.MAIN_SCREEN
import com.github.swent.swisstravel.ui.profile.selectpinnedpictures.SelectPinnedPicturesScreenTestTags.PHOTO_GRID
import com.github.swent.swisstravel.ui.profile.selectpinnedpictures.SelectPinnedPicturesScreenTestTags.REMOVE_BUTTON
import com.github.swent.swisstravel.ui.profile.selectpinnedpictures.SelectPinnedPicturesScreenTestTags.SAVE_BUTTON
import com.github.swent.swisstravel.utils.FirebaseEmulator
import com.github.swent.swisstravel.utils.UI_WAIT_TIMEOUT
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for SelectPinnedPicturesScreen. This class was made with some help from AI. */
@RunWith(AndroidJUnit4::class)
class SelectPinnedPicturesScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var userRepository: UserRepositoryFirebase
  private lateinit var imageRepository: ImageRepositoryFirebase
  private lateinit var viewModel: SelectPinnedPicturesViewModel

  // Mock navigation
  private val onBack: () -> Unit = mockk(relaxed = true)

  // Valid 1x1 pixel red image Base64
  private val validBase64 =
      "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="
  private lateinit var testUid: String
  private val imgUid1 = "img-1"
  private val imgUid2 = "img-2"

  @Before
  fun setup() = runBlocking {
    assertTrue("Firebase Emulator must be running", FirebaseEmulator.isRunning)

    val authResult = FirebaseEmulator.auth.signInAnonymously().await()
    testUid = authResult.user!!.uid

    userRepository = UserRepositoryFirebase(FirebaseEmulator.auth, FirebaseEmulator.firestore)
    imageRepository = ImageRepositoryFirebase(FirebaseEmulator.firestore, FirebaseEmulator.auth)

    // Seed Data into Firestore
    seedData()
    // Initialize ViewModel with seeded data
    viewModel = SelectPinnedPicturesViewModel(userRepository, imageRepository)
  }

  @After
  fun tearDown() = runBlocking {
    FirebaseEmulator.clearFirestoreEmulator()
    FirebaseEmulator.auth.signOut()
  }

  private suspend fun seedData() {
    // Create Images in 'images' collection
    val img1 = Image(imgUid1, testUid, validBase64)
    val img2 = Image(imgUid2, testUid, validBase64)

    val imagesCollection = FirebaseEmulator.firestore.collection("images")
    imagesCollection.document(imgUid1).set(img1).await()
    imagesCollection.document(imgUid2).set(img2).await()

    // Create User in 'users' collection
    val user =
        User(
            uid = testUid,
            name = "Test User",
            biography = "",
            email = "",
            profilePicUrl = "",
            preferences = emptyList(),
            friends = emptyList(),
            pinnedTripsUids = emptyList(),
            pinnedPicturesUids = listOf(imgUid1, imgUid2),
            favoriteTripsUids = emptyList())
    FirebaseEmulator.firestore.collection("users").document(testUid).set(user).await()
  }

  private fun launchScreen() {
    composeTestRule.setContent {
      SelectPinnedPicturesScreen(onBack = onBack, selectPinnedPicturesViewModel = viewModel)
    }
    composeTestRule.waitForIdle()
  }

  @Test
  fun screenDisplaysInitialImagesAndButtons() {
    launchScreen()

    // Verify Screen Content
    composeTestRule.onNodeWithTag(MAIN_SCREEN).assertIsDisplayed()

    // Wait until the first image appears
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(PhotoGridTestTags.getTestTagForPhoto(0))
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag(PHOTO_GRID).assertIsDisplayed()

    // Check for the presence of the two seeded images using PhotoGrid tags
    composeTestRule.onNodeWithTag(PhotoGridTestTags.getTestTagForPhoto(0)).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PhotoGridTestTags.getTestTagForPhoto(1)).assertIsDisplayed()

    // Verify Default Mode Buttons
    composeTestRule.onNodeWithTag(ADD_PICTURE_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SAVE_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EDIT_BUTTON).assertIsDisplayed()

    // Verify Edit Mode Buttons are not displayed
    composeTestRule.onNodeWithTag(REMOVE_BUTTON).assertDoesNotExist()
    composeTestRule.onNodeWithTag(CANCEL_BUTTON).assertDoesNotExist()

    composeTestRule.onNodeWithTag(LOADING_INDICATOR).assertDoesNotExist()
  }

  @Test
  fun editModeToggleChangesUI() {
    launchScreen()

    // Wait for load
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(PhotoGridTestTags.getTestTagForPhoto(0))
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Enter Edit Mode
    composeTestRule.onNodeWithTag(EDIT_BUTTON).performClick()

    // Check UI Changes
    composeTestRule.onNodeWithTag(CANCEL_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(REMOVE_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(REMOVE_BUTTON).assertIsNotEnabled() // No selection yet

    composeTestRule.onNodeWithTag(ADD_PICTURE_BUTTON).assertDoesNotExist()
    composeTestRule.onNodeWithTag(SAVE_BUTTON).assertDoesNotExist()
    composeTestRule.onNodeWithTag(EDIT_BUTTON).assertDoesNotExist()

    // Exit Edit Mode
    composeTestRule.onNodeWithTag(CANCEL_BUTTON).performClick()

    // Verify Reversion
    composeTestRule.onNodeWithTag(EDIT_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ADD_PICTURE_BUTTON).assertIsDisplayed()
  }

  @Test
  fun deleteFlowRemovesImageFromUiAndRepository() = runBlocking {
    launchScreen()

    // Wait for images
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(PhotoGridTestTags.getTestTagForPhoto(0))
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // 1. Enter Edit Mode
    composeTestRule.onNodeWithTag(EDIT_BUTTON).performClick()

    // 2. Select first image (index 0) using PhotoGrid tag
    composeTestRule
        .onNodeWithTag(PhotoGridTestTags.getTestTagForPhoto(0), useUnmergedTree = true)
        .performClick()

    // 3. Click Remove
    composeTestRule.onNodeWithTag(REMOVE_BUTTON).assertIsEnabled()
    composeTestRule.onNodeWithTag(REMOVE_BUTTON).performClick()

    // 4. Confirm Dialog using DeleteDialog tag
    composeTestRule.onNodeWithTag(DeleteDialogTestTags.CONFIRM_DELETE_BUTTON).performClick()

    composeTestRule.waitForIdle()

    // 5. Verify UI Update
    // Grid should now have only 1 item, the previous index 1 is now index 0.
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(PhotoGridTestTags.getTestTagForPhoto(1))
          .fetchSemanticsNodes()
          .isEmpty()
    }
    composeTestRule.onNodeWithTag(PhotoGridTestTags.getTestTagForPhoto(0)).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PhotoGridTestTags.getTestTagForPhoto(1)).assertDoesNotExist()

    // 6. Verify Repository Update
    val imgDoc = FirebaseEmulator.firestore.collection("images").document(imgUid1).get().await()
    assertNull("Image document should be deleted", imgDoc.data)
  }
}
