package com.github.swent.swisstravel.ui.profile.selectpinnedpictures

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.swent.swisstravel.model.image.Image
import com.github.swent.swisstravel.model.image.ImageRepositoryFirebase
import com.github.swent.swisstravel.model.user.User
import com.github.swent.swisstravel.model.user.UserRepositoryFirebase
import com.github.swent.swisstravel.ui.profile.selectpinnedpictures.SelectPinnedPicturesScreenTestTags.ADD_PICTURE_BUTTON
import com.github.swent.swisstravel.ui.profile.selectpinnedpictures.SelectPinnedPicturesScreenTestTags.LOADING_INDICATOR
import com.github.swent.swisstravel.ui.profile.selectpinnedpictures.SelectPinnedPicturesScreenTestTags.MAIN_SCREEN
import com.github.swent.swisstravel.ui.profile.selectpinnedpictures.SelectPinnedPicturesScreenTestTags.SAVE_BUTTON
import com.github.swent.swisstravel.ui.profile.selectpinnedpictures.SelectPinnedPicturesScreenTestTags.VERTICAL_GRID
import com.github.swent.swisstravel.ui.profile.selectpinnedpictures.SelectPinnedPicturesScreenTestTags.getTestTagForDelete
import com.github.swent.swisstravel.ui.profile.selectpinnedpictures.SelectPinnedPicturesScreenTestTags.getTestTagForImage
import com.github.swent.swisstravel.utils.FirebaseEmulator
import com.github.swent.swisstravel.utils.UI_WAIT_TIMEOUT
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
            pinnedPicturesUids = listOf(imgUid1, imgUid2))
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
      composeTestRule.onAllNodesWithTag(getTestTagForImage(0)).fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag(VERTICAL_GRID).assertIsDisplayed()
    // Verify images loaded from Emulator
    composeTestRule.onNodeWithTag(getTestTagForImage(0)).assertIsDisplayed()
    composeTestRule.onNodeWithTag(getTestTagForImage(1)).assertIsDisplayed()

    // Verify buttons
    composeTestRule.onNodeWithTag(ADD_PICTURE_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SAVE_BUTTON).assertIsDisplayed()

    composeTestRule.onNodeWithTag(LOADING_INDICATOR).assertDoesNotExist()
  }

  @Test
  fun deleteImageRemovesItFromUi() {
    launchScreen()

    // Wait for images to load
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule.onAllNodesWithTag(getTestTagForImage(0)).fetchSemanticsNodes().isNotEmpty()
    }

    // Delete the first image
    composeTestRule.onNodeWithTag(getTestTagForDelete(0)).performClick()
    composeTestRule.waitForIdle()

    // Verify UI update: Item 1 should be gone, Item 0 remains (list shifted)
    composeTestRule.onNodeWithTag(getTestTagForImage(1)).assertDoesNotExist()
    composeTestRule.onNodeWithTag(getTestTagForImage(0)).assertIsDisplayed()
  }

  @Test
  fun saveButtonTriggersRepositoryAndUpdate() = runBlocking {
    launchScreen()

    // Wait for images
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule.onAllNodesWithTag(getTestTagForImage(0)).fetchSemanticsNodes().isNotEmpty()
    }

    // Delete an image in UI
    composeTestRule.onNodeWithTag(getTestTagForDelete(0)).performClick()
    composeTestRule.waitForIdle()

    // Click Save
    composeTestRule.onNodeWithTag(SAVE_BUTTON).performClick()

    // Verify Navigation
    verify(timeout = UI_WAIT_TIMEOUT) { onBack() }

    // Check Image was deleted from "images" collection
    val imgDoc = FirebaseEmulator.firestore.collection("images").document(imgUid1).get().await()
    assertNull("Image document should be deleted", imgDoc.data)

    // Check User Profile was updated
    val userDoc = FirebaseEmulator.firestore.collection("users").document(testUid).get().await()

    // Made with the help of AI
    @Suppress("UNCHECKED_CAST") val pinnedPics = userDoc.get("pinnedPicturesUids") as? List<String>

    assertNotNull("Pinned pictures list should not be null", pinnedPics)
    // Should only contain the second image now
    assertEquals(listOf(imgUid2), pinnedPics)
  }
}
