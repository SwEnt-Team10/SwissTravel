package com.github.swent.swisstravel.ui.composable

import android.graphics.Bitmap
import android.util.Base64
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.platform.app.InstrumentationRegistry
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.image.Image
import com.github.swent.swisstravel.model.image.ImageRepositoryFirebase
import com.github.swent.swisstravel.utils.FirebaseEmulator
import com.github.swent.swisstravel.utils.FirestoreSwissTravelTest
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Tests for the Profile Image composable. Partially made with the help of AI. */
class ProfileImageTest : FirestoreSwissTravelTest() {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var imageRepository: ImageRepositoryFirebase

  @Before
  override fun setUp() {
    super.setUp()
    // Initialize the real repository using the Emulator instances
    imageRepository = ImageRepositoryFirebase(FirebaseEmulator.firestore, FirebaseEmulator.auth)
  }

  private fun getProfilePicContentDescription(): String {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    return context.getString(R.string.profile_pic_desc)
  }

  // Helper to generate a valid Base64 string for a 1x1 pixel image
  private fun createValidBase64Image(): String {
    val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
    return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
  }

  @Test
  fun profileImage_displaysPlaceholder_whenUrlIsNull() {
    composeTestRule.setContent { ProfileImage(urlOrUid = null, imageRepository = imageRepository) }

    // Verify placeholder is displayed
    composeTestRule
        .onNodeWithContentDescription(getProfilePicContentDescription())
        .assertIsDisplayed()
  }

  @Test
  fun profileImage_displaysPlaceholder_whenUrlIsBlank() {
    composeTestRule.setContent { ProfileImage(urlOrUid = "", imageRepository = imageRepository) }

    // Verify placeholder is displayed
    composeTestRule
        .onNodeWithContentDescription(getProfilePicContentDescription())
        .assertIsDisplayed()
  }

  @Test
  fun profileImage_usesCoil_whenUrlIsHttp() {
    // Note: We cannot easily verify Coil loaded the image without network,
    // but we can verify no crash and the component exists.
    composeTestRule.setContent {
      ProfileImage(urlOrUid = "http://example.com/image.png", imageRepository = imageRepository)
    }

    composeTestRule
        .onNodeWithContentDescription(getProfilePicContentDescription())
        .assertIsDisplayed()
  }

  @Test
  fun profileImage_fetchesAndDisplaysImage_whenUidExistsInFirestore() {
    val testUid = "test_image_uid"
    val base64Image = createValidBase64Image()

    // 1. Seed the Firestore Emulator with a real image document
    runBlocking {
      val currentUserUid = FirebaseEmulator.auth.currentUser?.uid ?: ""
      val image = Image(uid = testUid, ownerId = currentUserUid, base64 = base64Image)
      FirebaseEmulator.firestore.collection("images").document(testUid).set(image).await()
    }

    // 2. Load the composable with the UID
    composeTestRule.setContent {
      ProfileImage(urlOrUid = testUid, imageRepository = imageRepository)
    }

    // 3. Wait for the LaunchedEffect to fetch the image from the emulator
    composeTestRule.waitForIdle()

    // 4. Verify the image component is displayed
    // (Since both placeholder and loaded image share the same content description,
    // this primarily verifies that the async fetch didn't crash and resulted in a visible UI)
    composeTestRule
        .onNodeWithContentDescription(getProfilePicContentDescription())
        .assertIsDisplayed()
  }

  @Test
  fun profileImage_showsPlaceholder_whenUidDoesNotExist() {
    val testUid = "non_existent_uid"

    // Ensure emulator is clean (no image with this UID)

    composeTestRule.setContent {
      ProfileImage(urlOrUid = testUid, imageRepository = imageRepository)
    }

    composeTestRule.waitForIdle()

    // Verify placeholder is still displayed (fallback behavior)
    composeTestRule
        .onNodeWithContentDescription(getProfilePicContentDescription())
        .assertIsDisplayed()
  }
}
